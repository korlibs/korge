package com.soywiz.korgw.awt

import com.soywiz.klock.hr.hrSeconds
import com.soywiz.kmem.*
import com.soywiz.korev.Key
import com.soywiz.korev.MouseButton
import com.soywiz.korgw.GameWindow
import com.soywiz.korgw.internal.MicroDynamic
import com.soywiz.korgw.osx.CoreGraphics
import com.soywiz.korgw.osx.DisplayLinkCallback
import com.soywiz.korgw.platform.*
import com.soywiz.korgw.win32.Win32OpenglContext
import com.soywiz.korgw.x11.X
import com.soywiz.korgw.x11.X11OpenglContext
import com.soywiz.korim.awt.toAwt
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.net.URL
import com.soywiz.korio.util.OS
import com.sun.jna.CallbackThreadInitializer
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.unix.X11
import com.sun.jna.platform.win32.WinDef
import java.awt.*
import java.awt.Toolkit.getDefaultToolkit
import java.awt.event.*
import java.lang.reflect.Method
import java.net.URI
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JOptionPane


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
            ctx = when {
                OS.isMac -> {
                    val utils = Class.forName("sun.java2d.opengl.OGLUtilities")
                    val invokeWithOGLContextCurrentMethod = utils.getDeclaredMethod(
                        "invokeWithOGLContextCurrent",
                        Graphics::class.java, Runnable::class.java
                    )
                    invokeWithOGLContextCurrentMethod.isAccessible = true

                    //var timeSinceLast = 0L
                    object : BaseOpenglContext {
                        override val scaleFactor: Double get() = frameScaleFactor

                        override fun useContext(obj: Any?, action: Runnable) {
                            invokeWithOGLContextCurrentMethod.invoke(null, obj as Graphics, action)
                        }
                        override fun makeCurrent() = Unit
                        override fun releaseCurrent() = Unit
                        override fun swapBuffers() = Unit
                    }
                }
                OS.isWindows -> Win32OpenglContext(
                    WinDef.HWND(Native.getComponentPointer(frame)),
                    doubleBuffered = true
                )
                else -> {
                    try {
                        val d = X.XOpenDisplay(null)
                        val displayName = X.XDisplayString(d);
                        //println("displayName: $displayName")
                        val src = X.XDefaultScreen(d)
                        X.XSynchronize(d, true)
                        val contentWindow = frame.awtGetPeer().reflective().dynamicInvoke("getContentWindow") as Long
                        val win = X11.Window(contentWindow)
                        //val drawableId = Native.getWindowID(frame)
                        //val drawable = X11.Drawable(drawableId)
                        //val winRef = X11.WindowByReference()
                        //val p = Array(6) { IntByReference() }
                        //X.XGetGeometry(d, drawable, winRef, p[0], p[1], p[2], p[3], p[4], p[5])
                        //val win = winRef.value
                        //println("winId: $winId")
                        X11OpenglContext(d, win, src)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        throw e
                    }
                }
            }
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

