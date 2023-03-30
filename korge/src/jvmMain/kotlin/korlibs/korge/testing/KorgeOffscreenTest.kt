package korlibs.korge.testing

import korlibs.kgl.*
import korlibs.graphics.*
import korlibs.korge.*
import korlibs.korge.view.*
import korlibs.render.awt.*
import korlibs.image.color.*
import korlibs.io.async.*
import korlibs.io.lang.*
import korlibs.math.geom.*
import kotlinx.coroutines.*

fun suspendTestWithOffscreenAG(fbo_width: Int, fbo_height: Int, checkGl: Boolean = false, logGl: Boolean = false, callback: suspend CoroutineScope.(ag: AG) -> Unit) = suspendTest {
    KmlGlContextDefault().use { ctx ->
    //GLOBAL_HEADLESS_KML_CONTEXT.also { ctx ->
        val ag = AGOpenglAWT(checkGl = checkGl, logGl = logGl, context = ctx)
        ag.mainFrameBuffer.setSize(fbo_width, fbo_height)
        ctx.set()
        val gl = ag.gl

        val GL_RGBA8 = 0x8058


        // Build the texture that will serve as the color attachment for the framebuffer.
        val color_renderbuffer = gl.genRenderbuffer()
        gl.bindRenderbuffer(KmlGl.RENDERBUFFER, color_renderbuffer)
        gl.renderbufferStorage(KmlGl.RENDERBUFFER, GL_RGBA8, fbo_width, fbo_height)
        gl.bindRenderbuffer(KmlGl.RENDERBUFFER, 0)

        // Build the texture that will serve as the depth attachment for the framebuffer.
        val depth_renderbuffer = gl.genRenderbuffer()
        gl.bindRenderbuffer(KmlGl.RENDERBUFFER, depth_renderbuffer)
        gl.renderbufferStorage(KmlGl.RENDERBUFFER, KmlGl.DEPTH_COMPONENT, fbo_width, fbo_height)
        gl.bindRenderbuffer(KmlGl.RENDERBUFFER, 0)

        // Build the framebuffer.
        val framebuffer = gl.genFramebuffer()
        gl.bindFramebuffer(KmlGl.FRAMEBUFFER, framebuffer)
        gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, KmlGl.COLOR_ATTACHMENT0, KmlGl.RENDERBUFFER, color_renderbuffer)
        gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, KmlGl.DEPTH_ATTACHMENT, KmlGl.RENDERBUFFER, depth_renderbuffer)

        val status = gl.checkFramebufferStatus(KmlGl.FRAMEBUFFER)
        //if (status != GL_FRAMEBUFFER_COMPLETE)
        // Error

        callback(ag)
    }
}

class OffscreenContext(val testClassName: String, val testMethodName: String) {
    companion object {
        inline operator fun invoke(offset: Int = 0): OffscreenContext {
            val stack = Throwable().stackTrace[offset]
            return OffscreenContext(stack.className, stack.methodName)
        }
    }
}

class OffscreenStage(views: Views, val offscreenContext: OffscreenContext) : Stage(views)

inline fun korgeScreenshotTest(
    windowSize: SizeInt = SizeInt(512, 512),
    virtualSize: SizeInt = windowSize,
    bgcolor: RGBA? = Colors.BLACK,
    devicePixelRatio: Double = 1.0,
    checkGl: Boolean = true,
    logGl: Boolean = false,
    noinline callback: suspend OffscreenStage.() -> Unit
) {
    val offscreenContext = OffscreenContext()

    if (Environment["DISABLE_HEADLESS_TEST"] == "true") {
        System.err.println("Ignoring test $offscreenContext because env DISABLE_HEADLESS_TEST=true")
        return
    }

    var exception: Throwable? = null
    suspendTestWithOffscreenAG(windowSize.width, windowSize.height, checkGl = checkGl, logGl = logGl) { ag ->
        val korge = KorgeHeadless(KorgeConfig(
            windowSize = windowSize, virtualSize = virtualSize,
            backgroundColor = bgcolor,
            stageBuilder = { OffscreenStage(it, offscreenContext) }
            ),
            ag = ag, devicePixelRatio = devicePixelRatio,
        ) {
            injector.mapInstance(offscreenContext)
            try {
                callback(this as OffscreenStage)
            } catch (e: Throwable) {
                exception = e
            } finally {
                gameWindow.close()
            }
        }
    }
    exception?.let { throw it }
}
