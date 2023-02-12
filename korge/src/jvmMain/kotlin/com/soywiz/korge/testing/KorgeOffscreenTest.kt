package com.soywiz.korge.testing

import com.soywiz.kgl.*
import com.soywiz.korag.*
import com.soywiz.korge.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.awt.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.*

fun suspendTestWithOffscreenAG(callback: suspend CoroutineScope.(ag: AG) -> Unit) = suspendTest {
    KmlGlContextDefault().use { ctx ->
    //GLOBAL_HEADLESS_KML_CONTEXT.also { ctx ->
        val ag = AGOpenglAWT(checkGl = false, logGl = false, context = ctx)
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
    width: Int = 512, height: Int = 512,
    virtualWidth: Int = width, virtualHeight: Int = height,
    bgcolor: RGBA? = Colors.BLACK,
    devicePixelRatio: Double = 1.0,
    noinline callback: suspend OffscreenStage.() -> Unit
) {
    val offscreenContext = OffscreenContext()

    if (Environment["DISABLE_HEADLESS_TEST"] == "true") {
        System.err.println("Ignoring test $offscreenContext because env DISABLE_HEADLESS_TEST=true")
        return
    }

    var exception: Throwable? = null
    suspendTestWithOffscreenAG { ag ->
        val korge = KorgeHeadless(
            width = width, height = height,
            virtualWidth = virtualWidth, virtualHeight = virtualHeight,
            bgcolor = bgcolor,
            ag = ag, devicePixelRatio = devicePixelRatio,
            stageBuilder = { OffscreenStage(it, offscreenContext) }
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
