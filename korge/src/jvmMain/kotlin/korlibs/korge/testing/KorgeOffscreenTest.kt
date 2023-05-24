package korlibs.korge.testing

import korlibs.graphics.*
import korlibs.image.color.*
import korlibs.io.async.*
import korlibs.io.lang.*
import korlibs.kgl.*
import korlibs.korge.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.render.awt.*
import kotlinx.coroutines.*

internal fun createKmlGlContext(fboWidth: Int, fboHeight: Int): KmlGlContext {
    val ctx = KmlGlContextDefault()
    ctx.set()

    val gl = ctx.gl

    val GL_RGBA8 = 0x8058

    // Build the texture that will serve as the color attachment for the framebuffer.
    val colorRenderbuffer = gl.genRenderbuffer()
    gl.bindRenderbuffer(KmlGl.RENDERBUFFER, colorRenderbuffer)
    gl.renderbufferStorage(KmlGl.RENDERBUFFER, GL_RGBA8, fboWidth, fboHeight)
    gl.bindRenderbuffer(KmlGl.RENDERBUFFER, 0)

    // Build the texture that will serve as the depth attachment for the framebuffer.
    val depthRenderbuffer = gl.genRenderbuffer()
    gl.bindRenderbuffer(KmlGl.RENDERBUFFER, depthRenderbuffer)
    gl.renderbufferStorage(KmlGl.RENDERBUFFER, KmlGl.DEPTH_COMPONENT, fboWidth, fboHeight)
    gl.bindRenderbuffer(KmlGl.RENDERBUFFER, 0)

    // Build the framebuffer.
    val framebuffer = gl.genFramebuffer()
    gl.bindFramebuffer(KmlGl.FRAMEBUFFER, framebuffer)
    gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, KmlGl.COLOR_ATTACHMENT0, KmlGl.RENDERBUFFER, colorRenderbuffer)
    gl.framebufferRenderbuffer(KmlGl.FRAMEBUFFER, KmlGl.DEPTH_ATTACHMENT, KmlGl.RENDERBUFFER, depthRenderbuffer)

    val status = gl.checkFramebufferStatus(KmlGl.FRAMEBUFFER)
    //if (status != GL_FRAMEBUFFER_COMPLETE)
    // Error

    return ctx
}

fun suspendTestWithOffscreenAG(fboSize: Size, checkGl: Boolean = false, logGl: Boolean = false, callback: suspend CoroutineScope.(ag: AG) -> Unit) = suspendTest {
    val fboWidth = fboSize.width.toInt()
    val fboHeight = fboSize.height.toInt()

    val ctx = createKmlGlContext(fboWidth, fboHeight)
    //GLOBAL_HEADLESS_KML_CONTEXT.also { ctx ->
    val ag = AGOpenglAWT(checkGl = checkGl, logGl = logGl, context = ctx)
    try {
        ag.contextsToFree += ctx
        ag.mainFrameBuffer.setSize(fboWidth, fboHeight)
        ctx.set()

        callback(ag)
    } finally {
        ag.contextsToFree.forEach { it?.unset(); it?.close() }
        ag.contextsToFree.clear()
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
    windowSize: Size = Size(512, 512),
    virtualSize: Size = windowSize,
    bgcolor: RGBA? = Colors.BLACK,
    devicePixelRatio: Float = 1f,
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
    suspendTestWithOffscreenAG(windowSize, checkGl = checkGl, logGl = logGl) { ag ->
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
