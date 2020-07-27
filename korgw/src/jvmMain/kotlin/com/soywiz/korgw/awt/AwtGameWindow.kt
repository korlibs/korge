package com.soywiz.korgw.awt

import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.checkedIf
import com.soywiz.klock.hr.hrSeconds
import com.soywiz.kmem.*
import com.soywiz.korag.AGOpengl
import com.soywiz.korev.Key
import com.soywiz.korev.MouseButton
import com.soywiz.korgw.GameWindow
import com.soywiz.korgw.internal.MicroDynamic
import com.soywiz.korgw.osx.CoreGraphics
import com.soywiz.korgw.osx.DisplayLinkCallback
import com.soywiz.korgw.osx.MacKmlGL
import com.soywiz.korgw.platform.BaseOpenglContext
import com.soywiz.korgw.win32.Win32KmlGl
import com.soywiz.korgw.win32.Win32OpenglContext
import com.soywiz.korgw.x11.X
import com.soywiz.korgw.x11.X11KmlGl
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


class AwtAg(val window: AwtGameWindow, val checkGl: Boolean) : AGOpengl() {
    override val nativeComponent: Any = window
    override val gles: Boolean = true
    override val linux: Boolean = OS.isLinux
    override val gl: KmlGl by lazy {
        when {
            //OS.isMac -> MacKmlGL.checked(throwException = false)
            OS.isMac -> MacKmlGL
            OS.isWindows -> Win32KmlGl
            else -> X11KmlGl
        }.checkedIf(checkGl)
    }
}

class AwtGameWindow(val checkGl: Boolean) : GameWindow() {
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

    var ctx: BaseOpenglContext? = null
    //val frame = Window(Frame("Korgw"))
    val classLoader = this.javaClass.classLoader

    //private var currentInFullScreen = false

    //val fvsync get() = vsync
    val fvsync get() = false

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

        private var lastFactor = 0.0

        private fun ensureContext() {
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
                        val d = X.XOpenDisplay(null)
                        val src = X.XDefaultScreen(d)
                        val winId = Native.getWindowID(frame)
                        //println("winId: $winId")
                        X11OpenglContext(d, X11.Window(winId), src)
                    }
                }
            }
        }

        // https://stackoverflow.com/questions/52108178/swing-animation-still-stutter-when-i-use-toolkit-getdefaulttoolkit-sync
        // https://www.oracle.com/java/technologies/painting.html
        // https://docs.oracle.com/javase/tutorial/extra/fullscreen/rendering.html
        // https://docs.oracle.com/javase/tutorial/extra/fullscreen/doublebuf.html
        override fun paint(g: Graphics) {
            if (fvsync) {
                EventQueue.invokeLater {
                    //println("repaint!")
                    frame.repaint()
                }
            }
            val frame = this

            ensureContext()

            //GL.glClearColor(1f, 0f, 0f, 1f)
            //GL.glClear(GL.GL_COLOR_BUFFER_BIT)

            ctx?.useContext(g, Runnable {
                ctx?.swapInterval(1)

                val gl = ag.gl
                val factor = frameScaleFactor
                if (lastFactor != factor) {
                    lastFactor = factor
                    dispatchReshapeEvent()
                }

                //println("RENDER[1]")

                //println("FACTOR: $factor, nonScaledWidth=$nonScaledWidth, nonScaledHeight=$nonScaledHeight, scaledWidth=$scaledWidth, scaledHeight=$scaledHeight")
                gl.viewport(0, 0, scaledWidth.toInt(), scaledHeight.toInt())
                //gl.clearColor(.2f, .4f, .9f, 1f)
                gl.clearColor(.3f, .3f, .3f, 1f)
                gl.clear(gl.COLOR_BUFFER_BIT)
                //println(gl.getString(gl.VERSION))
                //println(gl.versionString)
                frame()
                gl.flush()
                gl.finish()
            })
            //Toolkit.getDefaultToolkit().sync();
        }
    }

    private fun getDisplayScalingFactor(component: Component): Double {
        val device = (component.graphicsConfiguration?.device ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice)
        val getScaleFactorMethod: Method? = try { device.javaClass.getMethod("getScaleFactor") } catch (e: Throwable) { null }
        return if (getScaleFactorMethod != null) {
            val scale: Any = getScaleFactorMethod.invoke(device)
            ((scale as? Number)?.toDouble()) ?: 1.0
        } else {
            (component.graphicsConfiguration ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration).defaultTransform.scaleX
        }
    }

    val frameScaleFactor: Double get() = run {
        getDisplayScalingFactor(frame)
        //val res = frame.toolkit.getDesktopProperty("apple.awt.contentScaleFactor") as? Number
        //if (res != null) return res.toDouble()
    }

    val nonScaledWidth get() = frame.contentPane.width.toDouble()
    val nonScaledHeight get() = frame.contentPane.height.toDouble()

    val scaledWidth get() = frame.contentPane.width * frameScaleFactor
    val scaledHeight get() = frame.contentPane.height * frameScaleFactor

    override var title: String
        get() = frame.title
        set(value) = run { frame.title = value }
    override val width: Int get() = (scaledWidth).toInt()
    override val height: Int get() = (scaledHeight).toInt()
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
    override var visible: Boolean
        get() = frame.isVisible
        set(value) {
            frame.isVisible = value
        }
    override var quality: Quality = Quality.AUTOMATIC

    override fun setSize(width: Int, height: Int) {
        frame.contentPane.setSize(width, height)
        frame.contentPane.preferredSize = Dimension(width, height)
        frame.pack()
        val dim = getDefaultToolkit().screenSize
        frame.setLocation(dim.width / 2 - frame.size.width / 2, dim.height / 2 - frame.size.height / 2)
    }

    override suspend fun browse(url: URL) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url.toString()));
        }
    }

    override suspend fun alert(message: String) {
        JOptionPane.showMessageDialog(frame, message, "Message", JOptionPane.WARNING_MESSAGE)
    }

    override suspend fun confirm(message: String): Boolean {
        return JOptionPane.showConfirmDialog(frame, message, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION
    }

    override suspend fun prompt(message: String, default: String): String {
        return JOptionPane.showInputDialog(frame, message, "Input", JOptionPane.PLAIN_MESSAGE, null, null, default).toString()
    }

    override suspend fun openFileDialog(filter: String?, write: Boolean, multi: Boolean): List<VfsFile> {
        val chooser = JFileChooser()
        //chooser.fileFilter = filter // @TODO: Filters
        chooser.isMultiSelectionEnabled = multi
        val result = if (write) {
            chooser.showSaveDialog(frame)
        } else {
            chooser.showOpenDialog(frame)
        } == JFileChooser.APPROVE_OPTION
        return if (result) chooser.selectedFiles.map { localVfs(it) } else listOf()
    }

    fun dispatchReshapeEvent() {
        val factor = frameScaleFactor
        dispatchReshapeEvent(
            frame.x,
            frame.y,
            (frame.contentPane.width * factor).toInt(),
            (frame.contentPane.height * factor).toInt()
        )
    }

    val displayLinkData = Memory(16L).also { it.clear() }
    var displayLinkLock: java.lang.Object? = null
    var displayLink: Pointer? = Pointer.NULL

    val displayLinkCallback by lazy {
        object : DisplayLinkCallback {
            override fun callback(
                displayLink: Pointer?,
                inNow: Pointer?,
                inOutputTime: Pointer?,
                flagsIn: Pointer?,
                flagsOut: Pointer?,
                userInfo: Pointer?
            ): Int {
                displayLinkLock?.let { displayLock ->
                    synchronized(displayLock) {
                        displayLock.notify()
                    }
                }
                return 0
            }
        }.also {
            Native.setCallbackThreadInitializer(it, CallbackThreadInitializer(false, false, "DisplayLink"))
        }
    }

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        launchImmediately(getCoroutineDispatcherWithCurrentContext()) {
            entry()
        }
        //frame.setBounds(0, 0, width, height)

        //val timer= Timer(40, ActionListener {
        //})

        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                running = false
            }
        })

        frame.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                queue {
                    dispatchReshapeEvent()
                    frame.repaint()
                }
            }
        })

        fun handleMouseEvent(e: MouseEvent) {
            queue {
                val ev = when (e.id) {
                    MouseEvent.MOUSE_MOVED -> com.soywiz.korev.MouseEvent.Type.MOVE
                    MouseEvent.MOUSE_CLICKED -> com.soywiz.korev.MouseEvent.Type.CLICK
                    MouseEvent.MOUSE_PRESSED -> com.soywiz.korev.MouseEvent.Type.DOWN
                    MouseEvent.MOUSE_RELEASED -> com.soywiz.korev.MouseEvent.Type.UP
                    else -> com.soywiz.korev.MouseEvent.Type.MOVE
                }
                val button = MouseButton[e.button - 1]
                val factor = frameScaleFactor
                val sx = e.x * factor
                val sy = e.y * factor
                val modifiers = e.modifiersEx
                dispatchMouseEvent(
                    type = ev,
                    id = 0,
                    x = sx.toInt(),
                    y = sy.toInt(),
                    button = button,
                    buttons = 0,
                    scrollDeltaX = 0.0,
                    scrollDeltaY = 0.0,
                    scrollDeltaZ = 0.0,
                    isShiftDown = modifiers hasFlags MouseEvent.SHIFT_DOWN_MASK,
                    isCtrlDown = modifiers hasFlags MouseEvent.CTRL_DOWN_MASK,
                    isAltDown = modifiers hasFlags MouseEvent.ALT_DOWN_MASK,
                    isMetaDown = modifiers hasFlags MouseEvent.META_DOWN_MASK,
                    scaleCoords = false,`
                    simulateClickOnUp = false
                )
            }
        }

        fun handleMouseWheelEvent(e: MouseWheelEvent) {
            queue {
                val ev = com.soywiz.korev.MouseEvent.Type.SCROLL
                val button = MouseButton[8]
                val factor = frameScaleFactor
                val sx = e.x * factor
                val sy = e.y * factor
                val modifiers = e.modifiersEx
                //TODO: check this on linux and macos
                val scrollDelta = e.scrollAmount * e.preciseWheelRotation // * e.unitsToScroll
                dispatchMouseEvent(
                    type = ev,
                    id = 0,
                    x = sx.toInt(),
                    y = sy.toInt(),
                    button = button,
                    buttons = 0,
                    scrollDeltaX = 0.0,
                    scrollDeltaY = scrollDelta,
                    scrollDeltaZ = 0.0,
                    isShiftDown = modifiers hasFlags MouseEvent.SHIFT_DOWN_MASK,
                    isCtrlDown = modifiers hasFlags MouseEvent.CTRL_DOWN_MASK,
                    isAltDown = modifiers hasFlags MouseEvent.ALT_DOWN_MASK,
                    isMetaDown = modifiers hasFlags MouseEvent.META_DOWN_MASK,
                    scaleCoords = false,
                    simulateClickOnUp = false
                )
            }
        }

        fun handleKeyEvent(e: KeyEvent) {
            queue {
                val ev = when (e.id) {
                    KeyEvent.KEY_TYPED -> com.soywiz.korev.KeyEvent.Type.TYPE
                    KeyEvent.KEY_PRESSED -> com.soywiz.korev.KeyEvent.Type.DOWN
                    KeyEvent.KEY_RELEASED -> com.soywiz.korev.KeyEvent.Type.UP
                    else -> com.soywiz.korev.KeyEvent.Type.TYPE
                }
                val id = 0
                val char = e.keyChar
                val keyCode = e.keyCode
                val key = AwtKeyMap[e.keyCode] ?: Key.UNKNOWN
                dispatchKeyEvent(ev, id, char, key, keyCode)
            }
        }

        frame.contentPane.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseDragged(e: MouseEvent) = handleMouseEvent(e)
        })

        frame.contentPane.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseMoved(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseEntered(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseDragged(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseClicked(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseExited(e: MouseEvent) = handleMouseEvent(e)
            override fun mousePressed(e: MouseEvent) = handleMouseEvent(e)
        })

        frame.addMouseWheelListener { e -> handleMouseWheelEvent(e) }

        frame.addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) = handleKeyEvent(e)
            override fun keyPressed(e: KeyEvent) = handleKeyEvent(e)
            override fun keyReleased(e: KeyEvent) = handleKeyEvent(e)
        })

        queue {
            dispatchInitEvent()
            dispatchReshapeEvent()
        }
        EventQueue.invokeLater {
            frame.isVisible = true
        }

        EventQueue.invokeLater {
            //println("repaint!")
            frame.repaint()
        }
        //val timer = Timer(1000 / 60, ActionListener { frame.repaint() })
        //timer.start()

        //val toolkit = Toolkit.getDefaultToolkit()
        //val events = toolkit.systemEventQueue

        if (OS.isMac) {
            val displayID = CoreGraphics.CGMainDisplayID()
            val res = CoreGraphics.CVDisplayLinkCreateWithCGDisplay(displayID, displayLinkData)

            if (res == 0) {
                displayLinkLock = java.lang.Object()
                displayLink = displayLinkData.getPointer(0L)
                if (CoreGraphics.CVDisplayLinkSetOutputCallback(displayLink, displayLinkCallback, Pointer.NULL) == 0) {
                    CoreGraphics.CVDisplayLinkStart(displayLink)
                } else {
                    displayLinkLock = null
                    displayLink = Pointer.NULL
                }
            }
        }

        //Thread.sleep(1000000L)

        val displayLock = this.displayLinkLock

        if (displayLock != null) {
            println("Using DisplayLink")
        } else {
            println("NOT Using DisplayLink")
        }

        while (running) {
            //frame.invalidate()
            if (fvsync) {
                //val startFrameCount = frameCount
                //EventQueue.invokeLater {
                //    //println("repaint!")
                //    frame.repaint()
                //}
                //while (frameCount == startFrameCount) {
                //    Thread.sleep(0L, 100_000)
                //}
                Thread.sleep(1L)
            } else {

                /*
                val startTime = PerformanceCounter.hr
                val endTime = startTime + (1000.toDouble() / fps.toDouble()).hrSeconds
                //Toolkit.getEventQueue()
                //EventQueue.isDispatchThread()
                val currentFrameCount = frameCount
                events.postEvent(PeerEvent(toolkit, Runnable {
                    frame.repaint()
                }, PeerEvent.ULTIMATE_PRIORITY_EVENT))
                //EventQueue.invokeLater { frame.repaint() }
                //timer.delay = fps

                while (currentFrameCount == frameCount) {
                    Thread.sleep(0L, 100_000)
                }

                val delay = endTime - PerformanceCounter.hr
                if (delay > 0.hrNanoseconds) {
                    Thread.sleep(0L, delay.nanosecondsInt)
                }

                //Thread.sleep(timePerFrame.millisecondsLong)
                 */

                //events.postEvent(PeerEvent(toolkit, Runnable { frame.repaint() }, PeerEvent.ULTIMATE_PRIORITY_EVENT))
                //println("---")

                //val start = PerformanceCounter.hr
                //val currentTick = ticks
                frame.repaint()

                when {
                    ctx?.supportsSwapInterval() == true -> {
                        Unit // Do nothing. Already waited for vsync
                    }
                    displayLock != null -> {
                        synchronized(displayLock) { displayLock.wait(100L) }
                    }
                    else -> {
                        val nanos = System.nanoTime()
                        val frameTimeNanos = (1.0 / fps.toDouble()).hrSeconds.nanosecondsInt
                        val delayNanos = frameTimeNanos - (nanos % frameTimeNanos)
                        if (delayNanos > 0) {
                            //println(delayNanos / 1_000_000)
                            Thread.sleep(delayNanos / 1_000_000, (delayNanos % 1_000_000).toInt())
                        }
                        //println("[2] currentFrameCount=$currentFrameCount, frameCount=$frameCount")

                        //println(System.nanoTime())
                    }
                }
                //val end = PerformanceCounter.hr
                //println((end - start).timeSpan)
            }
        }
        //timer.stop()

        if (OS.isMac && displayLink != Pointer.NULL) {
            CoreGraphics.CVDisplayLinkStop(displayLink)
        }

        dispatchDestroyEvent()

        frame.isVisible = false
        frame.dispose()

        //exitProcess(0) // Don't do this since we might continue in the e2e test
    }
}

internal val AwtKeyMap = mapOf(
    KeyEvent.VK_ENTER to Key.ENTER,
    KeyEvent.VK_BACK_SPACE to Key.BACKSPACE,
    KeyEvent.VK_TAB to Key.TAB,
    KeyEvent.VK_CANCEL to Key.CANCEL,
    KeyEvent.VK_CLEAR to Key.CLEAR,
    KeyEvent.VK_SHIFT to Key.LEFT_SHIFT,
    KeyEvent.VK_CONTROL to Key.LEFT_CONTROL,
    KeyEvent.VK_ALT to Key.LEFT_ALT,
    KeyEvent.VK_PAUSE to Key.PAUSE,
    KeyEvent.VK_CAPS_LOCK to Key.CAPS_LOCK,
    KeyEvent.VK_ESCAPE to Key.ESCAPE,
    KeyEvent.VK_SPACE to Key.SPACE,
    KeyEvent.VK_PAGE_UP to Key.PAGE_UP,
    KeyEvent.VK_PAGE_DOWN to Key.PAGE_DOWN,
    KeyEvent.VK_END to Key.END,
    KeyEvent.VK_HOME to Key.HOME,
    KeyEvent.VK_LEFT to Key.LEFT,
    KeyEvent.VK_UP to Key.UP,
    KeyEvent.VK_RIGHT to Key.RIGHT,
    KeyEvent.VK_DOWN to Key.DOWN,
    KeyEvent.VK_COMMA to Key.COMMA,
    KeyEvent.VK_MINUS to Key.MINUS,
    KeyEvent.VK_PLUS to Key.PLUS,
    KeyEvent.VK_PERIOD to Key.PERIOD,
    KeyEvent.VK_SLASH to Key.SLASH,
    KeyEvent.VK_0 to Key.N0,
    KeyEvent.VK_1 to Key.N1,
    KeyEvent.VK_2 to Key.N2,
    KeyEvent.VK_3 to Key.N3,
    KeyEvent.VK_4 to Key.N4,
    KeyEvent.VK_5 to Key.N5,
    KeyEvent.VK_6 to Key.N6,
    KeyEvent.VK_7 to Key.N7,
    KeyEvent.VK_8 to Key.N8,
    KeyEvent.VK_9 to Key.N9,
    KeyEvent.VK_SEMICOLON to Key.SEMICOLON,
    KeyEvent.VK_EQUALS to Key.EQUAL,
    KeyEvent.VK_A to Key.A,
    KeyEvent.VK_B to Key.B,
    KeyEvent.VK_C to Key.C,
    KeyEvent.VK_D to Key.D,
    KeyEvent.VK_E to Key.E,
    KeyEvent.VK_F to Key.F,
    KeyEvent.VK_G to Key.G,
    KeyEvent.VK_H to Key.H,
    KeyEvent.VK_I to Key.I,
    KeyEvent.VK_J to Key.J,
    KeyEvent.VK_K to Key.K,
    KeyEvent.VK_L to Key.L,
    KeyEvent.VK_M to Key.M,
    KeyEvent.VK_N to Key.N,
    KeyEvent.VK_O to Key.O,
    KeyEvent.VK_P to Key.P,
    KeyEvent.VK_Q to Key.Q,
    KeyEvent.VK_R to Key.R,
    KeyEvent.VK_S to Key.S,
    KeyEvent.VK_T to Key.T,
    KeyEvent.VK_U to Key.U,
    KeyEvent.VK_V to Key.V,
    KeyEvent.VK_W to Key.W,
    KeyEvent.VK_X to Key.X,
    KeyEvent.VK_Y to Key.Y,
    KeyEvent.VK_Z to Key.Z,
    KeyEvent.VK_OPEN_BRACKET to Key.OPEN_BRACKET,
    KeyEvent.VK_BACK_SLASH to Key.BACKSLASH,
    KeyEvent.VK_CLOSE_BRACKET to Key.CLOSE_BRACKET,
    KeyEvent.VK_NUMPAD0 to Key.NUMPAD0,
    KeyEvent.VK_NUMPAD1 to Key.NUMPAD1,
    KeyEvent.VK_NUMPAD2 to Key.NUMPAD2,
    KeyEvent.VK_NUMPAD3 to Key.NUMPAD3,
    KeyEvent.VK_NUMPAD4 to Key.NUMPAD4,
    KeyEvent.VK_NUMPAD5 to Key.NUMPAD5,
    KeyEvent.VK_NUMPAD6 to Key.NUMPAD6,
    KeyEvent.VK_NUMPAD7 to Key.NUMPAD7,
    KeyEvent.VK_NUMPAD8 to Key.NUMPAD8,
    KeyEvent.VK_NUMPAD9 to Key.NUMPAD9,
    KeyEvent.VK_MULTIPLY to Key.KP_MULTIPLY,
    KeyEvent.VK_ADD to Key.KP_ADD,
    KeyEvent.VK_SEPARATER to Key.KP_SEPARATOR,
    KeyEvent.VK_SUBTRACT to Key.KP_SUBTRACT,
    KeyEvent.VK_DECIMAL to Key.KP_DECIMAL,
    KeyEvent.VK_DIVIDE to Key.KP_DIVIDE,
    KeyEvent.VK_DELETE to Key.DELETE,
    KeyEvent.VK_NUM_LOCK to Key.NUM_LOCK,
    KeyEvent.VK_SCROLL_LOCK to Key.SCROLL_LOCK,
    KeyEvent.VK_F1 to Key.F1,
    KeyEvent.VK_F2 to Key.F2,
    KeyEvent.VK_F3 to Key.F3,
    KeyEvent.VK_F4 to Key.F4,
    KeyEvent.VK_F5 to Key.F5,
    KeyEvent.VK_F6 to Key.F6,
    KeyEvent.VK_F7 to Key.F7,
    KeyEvent.VK_F8 to Key.F8,
    KeyEvent.VK_F9 to Key.F9,
    KeyEvent.VK_F10 to Key.F10,
    KeyEvent.VK_F11 to Key.F11,
    KeyEvent.VK_F12 to Key.F12,
    KeyEvent.VK_F13 to Key.F13,
    KeyEvent.VK_F14 to Key.F14,
    KeyEvent.VK_F15 to Key.F15,
    KeyEvent.VK_F16 to Key.F16,
    KeyEvent.VK_F17 to Key.F17,
    KeyEvent.VK_F18 to Key.F18,
    KeyEvent.VK_F19 to Key.F19,
    KeyEvent.VK_F20 to Key.F20,
    KeyEvent.VK_F21 to Key.F21,
    KeyEvent.VK_F22 to Key.F22,
    KeyEvent.VK_F23 to Key.F23,
    KeyEvent.VK_F24 to Key.F24,
    KeyEvent.VK_PRINTSCREEN to Key.PRINT_SCREEN,
    KeyEvent.VK_INSERT to Key.INSERT,
    KeyEvent.VK_HELP to Key.HELP,
    KeyEvent.VK_META to Key.META,
    KeyEvent.VK_BACK_QUOTE to Key.BACKQUOTE,
    KeyEvent.VK_QUOTE to Key.QUOTE,
    KeyEvent.VK_KP_UP to Key.KP_UP,
    KeyEvent.VK_KP_DOWN to Key.KP_DOWN,
    KeyEvent.VK_KP_LEFT to Key.KP_LEFT,
    KeyEvent.VK_KP_RIGHT to Key.KP_RIGHT,
    KeyEvent.VK_UNDEFINED to Key.UNDEFINED
)
