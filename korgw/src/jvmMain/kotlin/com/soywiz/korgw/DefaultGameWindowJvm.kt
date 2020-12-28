package com.soywiz.korgw

import com.soywiz.korag.AG
import com.soywiz.korag.AGConfig
import com.soywiz.korag.AGFactory
import com.soywiz.korag.AGWindow
import com.soywiz.korev.*
import com.soywiz.korgw.awt.AwtGameWindow
//import com.soywiz.korgw.jogl.JoglGameWindow
import com.soywiz.korgw.osx.MacGameWindow
import com.soywiz.korgw.osx.initializeMacOnce
import com.soywiz.korgw.osx.isOSXMainThread
import com.soywiz.korgw.x11.X11GameWindow
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.util.OS
import kotlinx.coroutines.runBlocking

actual fun CreateDefaultGameWindow(): GameWindow {
    if (OS.isMac) {
        initializeMacOnce()
    }

    val engine = korgwJvmEngine
        ?: System.getenv("KORGW_JVM_ENGINE")
        ?: System.getProperty("korgw.jvm.engine")
        //?: "jogl"
        //?: "jna"
        ?: "default"

    val checkGl = null
        ?: System.getenv("KORGW_CHECK_OPENGL")?.toBooleanOrNull()
        ?: System.getProperty("korgw.check.opengl")?.toBooleanOrNull()
        ?: GLOBAL_CHECK_GL

    val logGl = null
        ?: System.getenv("KORGW_LOG_OPENGL")?.toBooleanOrNull()
        ?: System.getProperty("korgw.log.opengl")?.toBooleanOrNull()
        ?: false

    return when (engine) {
        "default" -> when {
            //OS.isLinux -> X11GameWindow(checkGl)
            else -> AwtGameWindow(checkGl, logGl)
        }
        "jna" -> when {
            OS.isMac -> {
                when {
                    isOSXMainThread -> MacGameWindow(checkGl, logGl)
                    else -> {
                        println("WARNING. Slower startup: NOT in main thread! Using AWT! (on mac use -XstartOnFirstThread when possible)")
                        AwtGameWindow(checkGl, logGl)
                    }
                }
            }
            //OS.isLinux -> X11GameWindow(checkGl)
            OS.isLinux -> AwtGameWindow(checkGl, logGl)
            //OS.isWindows -> com.soywiz.korgw.win32.Win32GameWindow()
            OS.isWindows -> AwtGameWindow(checkGl, logGl)
            else -> X11GameWindow(checkGl)
        }
        "awt" -> when {
            OS.isMac && isOSXMainThread -> MacGameWindow(checkGl,logGl)
            else -> AwtGameWindow(checkGl, logGl)
        }
        //"jogl" -> {
        //    if (isOSXMainThread) {
        //        println("-XstartOnFirstThread not supported via Jogl, switching to an experimental native jna-based implementation")
        //        MacGameWindow()
        //    } else {
        //        // @TODO: Remove JoGL after a month once we ensure JNA/native versions work for everyone
        //        JoglGameWindow()
        //    }
        //}
        else -> {
            error("Unsupported KORGW_JVM_ENGINE,korgw.jvm.engine='$engine'")
        }
    }
}

object JvmAGFactory : AGFactory {
    override val supportsNativeFrame: Boolean = true

    override fun create(nativeControl: Any?, config: AGConfig): AG {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createFastWindow(title: String, width: Int, height: Int): AGWindow {
        return CreateDefaultGameWindow().apply {
            this.title = title
            this.setSize(width, height)
        }
    }
}

object TestGameWindow {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val gameWindow = CreateDefaultGameWindow()
            //val gameWindow = Win32GameWindow()
            //val gameWindow = AwtGameWindow()
            gameWindow.addEventListener<MouseEvent> {
                if (it.type == MouseEvent.Type.CLICK) {
                    //println("MOUSE EVENT $it")
                    gameWindow.toggleFullScreen()
                }
            }
            //gameWindow.toggleFullScreen()
            gameWindow.setSize(320, 240)
            gameWindow.title = "HELLO WORLD"
            var step = 0
            gameWindow.loop {
                val ag = gameWindow.ag
                gameWindow.onRenderEvent {
                    ag.clear(RGBA(64, 96, step % 256, 255))
                    step++
                }
                //println("HELLO")
            }
        }
    }
}

private fun String.toBooleanOrNull(): Boolean? = try {
    toBoolean()
} catch (e: Throwable) {
    null
}
