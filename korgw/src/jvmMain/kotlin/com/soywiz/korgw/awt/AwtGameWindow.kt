package com.soywiz.korgw.awt

import com.soywiz.korgw.internal.MicroDynamic
import com.soywiz.korgw.platform.*
import com.soywiz.korim.awt.toAwt
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.util.OS
import java.awt.*
import java.awt.Toolkit.getDefaultToolkit
import java.awt.event.*
import javax.swing.JFrame


class AwtGameWindow(val checkGl: Boolean) : BaseAwtGameWindow() {
    override val ag: AwtAg = AwtAg(this, checkGl)

    /*
    fun JFrame.isFullScreen(): Boolean {
        try {
            awtGetPeer()
        } catch (e: Throwable) {
            e.printStackTrace()
            return false
        }
    }
    */

    override var ctx: BaseOpenglContext? = null
    //val frame = Window(Frame("Korgw"))
    val classLoader = this.javaClass.classLoader

    override fun ensureContext() {
        if (ctx == null) {
            ctx = glContextFromComponent(frame)
        }
    }

    //private var currentInFullScreen = false

    val frame: JFrame = object : JFrame("Korgw") {
        val frame = this
        //val frame = object : Frame("Korgw") {
        init {
            isVisible = false
            ignoreRepaint = true
            setBounds(0, 0, 640, 480)
            val frame = this
            val dim = getDefaultToolkit().screenSize
            frame.setLocation(dim.width / 2 - frame.size.width / 2, dim.height / 2 - frame.size.height / 2)

            if (OS.isMac) {
                //rootPane.putClientProperty("apple.awt.fullscreenable", true)

                /*
                val listener = Proxy.newProxyInstance(classLoader, arrayOf(Class.forName("com.apple.eawt.FullScreenListener"))) { proxy, method, args ->
                    //println("INVOKED: $proxy, $method")
                    when (method.name) {
                        "windowEnteredFullScreen" -> {
                            currentInFullScreen = true
                        }
                        "windowExitedFullScreen" -> {
                            currentInFullScreen = false
                        }
                    }
                    null
                }
                */

                MicroDynamic {
                    getClass("com.apple.eawt.FullScreenUtilities").invoke("setWindowCanFullScreen", frame, true)
                    //getClass("com.apple.eawt.FullScreenUtilities").invoke("addFullScreenListenerTo", frame, listener)
                }

                // @TODO: This one owns the whole screen in a bad way
                //val env = GraphicsEnvironment.getLocalGraphicsEnvironment()
                //val device = env.defaultScreenDevice
                //device.fullScreenWindow = this
            }
        }

        override fun paintComponents(g: Graphics?) {

        }


        // https://stackoverflow.com/questions/52108178/swing-animation-still-stutter-when-i-use-toolkit-getdefaulttoolkit-sync
        // https://www.oracle.com/java/technologies/painting.html
        // https://docs.oracle.com/javase/tutorial/extra/fullscreen/rendering.html
        // https://docs.oracle.com/javase/tutorial/extra/fullscreen/doublebuf.html
        override fun paint(g: Graphics) {
            framePaint(g)
        }
    }

    override var title: String
        get() = frame.title
        set(value) = run { frame.title = value }
    override var icon: Bitmap? = null
        set(value) {
            field = value
            val awtImage = value?.toAwt()
            if (awtImage != null) {
                kotlin.runCatching {
                    MicroDynamic {
                        getClass("java.awt.Taskbar").invoke("getTaskbar").invoke("setIconImage", awtImage)
                    }
                }
                frame.iconImage = awtImage
            }
        }

    override var fullscreen: Boolean
        get() = frame.rootPane.bounds == frame.bounds
        set(value) {
            //println("fullscreen: $fullscreen -> $value")
            if (fullscreen != value) {
                if (OS.isMac) {
                    //println("TOGGLING!")
                    queue {
                        MicroDynamic {
                            //println("INVOKE!: ${getClass("com.apple.eawt.Application").invoke("getApplication")}")
                            getClass("com.apple.eawt.Application").invoke("getApplication").invoke("requestToggleFullScreen", frame)
                        }
                    }
                }
            }
        }

    override fun setSize(width: Int, height: Int) {
        contentComponent.setSize(width, height)
        contentComponent.preferredSize = Dimension(width, height)
        frame.pack()
        val dim = Toolkit.getDefaultToolkit().screenSize
        component.setLocation(dim.width / 2 - component.size.width / 2, dim.height / 2 - component.size.height / 2)
    }

    override val component: Component get() = frame
    override val contentComponent: Component get() = frame.contentPane

    override fun loopInitialization() {
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                running = false
            }
        })

    }

    override fun frameDispose() {
        frame.dispose()
    }
}

