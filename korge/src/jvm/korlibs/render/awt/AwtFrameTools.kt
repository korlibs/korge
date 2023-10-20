package korlibs.render.awt

import korlibs.io.dynamic.*
import korlibs.platform.*
import java.awt.*
import java.awt.image.*
import javax.swing.*

fun JFrame.setIconIncludingTaskbarFromImage(awtImage: BufferedImage?) {
    val frame = this
    runCatching {
        frame.iconImage = awtImage?.getScaledInstance(32, 32, Image.SCALE_SMOOTH)
        Dyn.global["java.awt.Taskbar"].dynamicInvoke("getTaskbar").dynamicInvoke("setIconImage", awtImage)
    }
}

var JFrame.isFullScreen: Boolean
    get() {
        val frame = this
        return when {
            Platform.isMac -> frame.rootPane.bounds == frame.bounds
            else -> GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.fullScreenWindow == frame
        }
    }
    set(value) {
        val frame = this
        //println("fullscreen: $fullscreen -> $value")
        if (isFullScreen != value) {
            when {
                Platform.isMac -> {
                    //println("TOGGLING!")
                    if (isFullScreen != value) {
                        EventQueue.invokeLater {
                            try {
                                //println("INVOKE!: ${getClass("com.apple.eawt.Application").invoke("getApplication")}")
                                Dyn.global["com.apple.eawt.Application"]
                                    .dynamicInvoke("getApplication")
                                    .dynamicInvoke("requestToggleFullScreen", frame)
                            } catch (e: Throwable) {
                                if (e::class.qualifiedName != "java.lang.reflect.InaccessibleObjectException") {
                                    e.printStackTrace()
                                }
                                GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.fullScreenWindow = if (value) frame else null
                                frame.isVisible = true
                            }
                        }
                    }
                }
                else -> {
                    GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.fullScreenWindow = if (value) frame else null

                    //frame.extendedState = if (value) JFrame.MAXIMIZED_BOTH else JFrame.NORMAL
                    //frame.isUndecorated = value
                    frame.isVisible = true
                    //frame.isAlwaysOnTop = true
                }
            }
        }
    }

fun JFrame.initTools() {
    if (Platform.isMac) {
        try {
            Dyn.global["com.apple.eawt.FullScreenUtilities"].dynamicInvoke("setWindowCanFullScreen", this, true)
            //Dyn.global["com.apple.eawt.FullScreenUtilities"].invoke("addFullScreenListenerTo", frame, listener)
        } catch (e: Throwable) {
            if (e::class.qualifiedName != "java.lang.reflect.InaccessibleObjectException") {
                e.printStackTrace()
            }
        }
    }
}
