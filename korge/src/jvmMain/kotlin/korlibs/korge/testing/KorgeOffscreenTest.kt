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

fun createKmlGlContext(fboWidth: Int, fboHeight: Int): KmlGlContext = OffsetKmlGlContext(fboWidth, fboHeight, doUnset = false)

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
        // IMPORTANT!!! This must be kept inline as we use the stacktrace to automatically detect
        // the test method name.
        inline operator fun invoke(offset: Int = 0): OffscreenContext {
            val stack = Throwable().stackTrace[offset]
            return OffscreenContext(stack.className, stack.methodName)
        }
    }
}

class OffscreenStage(views: Views) : Stage(views)

inline fun korgeScreenshotTest(
    windowSize: Size = Size(512, 512),
    virtualSize: Size = windowSize,
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
    suspendTestWithOffscreenAG(windowSize, checkGl = checkGl, logGl = logGl) { ag ->
        KorgeHeadless(KorgeConfig(
            windowSize = windowSize, virtualSize = virtualSize,
            backgroundColor = bgcolor,
            stageBuilder = { OffscreenStage(it) }
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
