package com.soywiz.korgw.awt

import com.soywiz.kgl.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korgw.*
import com.soywiz.korgw.osx.*
import com.soywiz.korgw.platform.*
import com.soywiz.korgw.win32.*
import com.soywiz.korgw.x11.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.net.URL
import com.soywiz.korio.util.*
import com.sun.jna.*
import java.awt.*
import java.awt.event.*
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.net.*
import javax.swing.*
import java.awt.GraphicsDevice

abstract class BaseAwtGameWindow : GameWindow() {
    abstract override val ag: AwtAg

    //val fvsync get() = vsync
    val fvsync get() = false
    open val ctx: BaseOpenglContext? = null

    abstract val component: Component
    abstract val contentComponent: Component
    private var lastFactor = 0.0

    private var _window: Window? = null
    val window: Window? get() {
        if (_window == null) {
            _window = SwingUtilities.getWindowAncestor(component) ?: (component as? Window?)
        }
        return _window
    }
    val windowOrComponent get() = window ?: component

    override var cursor: Cursor = Cursor.DEFAULT
        set(value) {
            field = value
            val awtCursor = when (value) {
                Cursor.DEFAULT -> java.awt.Cursor.DEFAULT_CURSOR
                Cursor.CROSSHAIR -> java.awt.Cursor.CROSSHAIR_CURSOR
                Cursor.TEXT -> java.awt.Cursor.TEXT_CURSOR
                Cursor.HAND -> java.awt.Cursor.HAND_CURSOR
                Cursor.MOVE -> java.awt.Cursor.MOVE_CURSOR
                Cursor.WAIT -> java.awt.Cursor.WAIT_CURSOR
                Cursor.RESIZE_EAST -> java.awt.Cursor.E_RESIZE_CURSOR
                Cursor.RESIZE_SOUTH -> java.awt.Cursor.S_RESIZE_CURSOR
                Cursor.RESIZE_WEST -> java.awt.Cursor.W_RESIZE_CURSOR
                Cursor.RESIZE_NORTH -> java.awt.Cursor.N_RESIZE_CURSOR
                Cursor.RESIZE_NORTH_EAST -> java.awt.Cursor.NE_RESIZE_CURSOR
                Cursor.RESIZE_NORTH_WEST -> java.awt.Cursor.NW_RESIZE_CURSOR
                Cursor.RESIZE_SOUTH_EAST -> java.awt.Cursor.SE_RESIZE_CURSOR
                Cursor.RESIZE_SOUTH_WEST -> java.awt.Cursor.SW_RESIZE_CURSOR
            }
            component.cursor = java.awt.Cursor(awtCursor)
        }

    override fun showContextMenu(items: List<MenuItem?>) {
        val popupMenu = JPopupMenu()
        for (item in items) {
            if (item?.text == null) {
                popupMenu.add(JSeparator())
            } else {
                popupMenu.add(JMenuItem(item.text).also {
                    it.isEnabled = item.enabled
                    it.addActionListener {
                        item.action()
                    }
                })
            }
        }
        popupMenu.show(contentComponent, mouseX, mouseY)
    }

    protected open fun ensureContext() {
    }

    fun framePaint(g: Graphics) {
        //println("framePaint")

        ag.isGlAvailable = true

        if (fvsync) {
            EventQueue.invokeLater {
                //println("repaint!")
                component.repaint()
            }
        }
        val frame = this

        ensureContext()

        //GL.glClearColor(1f, 0f, 0f, 1f)
        //GL.glClear(GL.GL_COLOR_BUFFER_BIT)

        ctx?.useContext(g, ag, paintInContextDelegate)
        //Toolkit.getDefaultToolkit().sync();
    }

    val paintInContextDelegate: (Graphics, BaseOpenglContext.ContextInfo) -> Unit = { g, info ->
        paintInContext(g, info)
    }

    val state: KmlGlState by lazy { ag.createGlState() }

    fun paintInContext(g: Graphics, info: BaseOpenglContext.ContextInfo) {
        state.keep {
            //run {
            ctx?.swapInterval(1)

            val g = g as Graphics2D
            val gl = ag.gl
            val factor = frameScaleFactor
            if (lastFactor != factor) {
                lastFactor = factor
                reshaped = true
            }

            //println("RENDER[1]")

            val viewport = info.viewport
            val scissor = info.scissors
            /*
            gl.enableDisable(gl.SCISSOR_TEST, scissor != null)
            if (scissor != null) {
                gl.scissor(scissor.x.toInt(), scissor.y.toInt(), scissor.width.toInt(), scissor.height.toInt())
            }
            if (viewport != null) {
                gl.viewport(viewport.x.toInt(), viewport.y.toInt(), viewport.width.toInt(), viewport.height.toInt())
            } else {
                gl.viewport(0, 0, scaledWidth.toInt().coerceAtLeast(1), scaledHeight.toInt().coerceAtLeast(1))
            }
            gl.clearColor(.3f, .3f, .3f, 1f)
            gl.clear(gl.COLOR_BUFFER_BIT)
            */

            //println("-- viewport=$viewport, scissors=$scissor")

            if (component is JFrame) {
                //println("component.width: ${contentComponent.width}x${contentComponent.height}")
                ag.mainRenderBuffer.setSize(
                    0, 0, (contentComponent.width * factor).toInt(), (contentComponent.height * factor).toInt(),
                )
            } else {
                ag.mainRenderBuffer.scissor(scissor)
                if (viewport != null) {
                    //val window = SwingUtilities.getWindowAncestor(contentComponent)
                    //println("window=${window.width}x${window.height} : factor=$factor")

                    val frameOrComponent = (window as? JFrame)?.contentPane ?: windowOrComponent

                    ag.mainRenderBuffer.setSize(
                        viewport.x, viewport.y, viewport.width, viewport.height,
                        (frameOrComponent.width * factor).toInt(),
                        (frameOrComponent.height * factor).toInt(),
                    )
                } else {
                    ag.mainRenderBuffer.setSize(
                        0, 0, (component.width * factor).toInt(), (component.height * factor).toInt(),
                    )
                }
            }

            //println(gl.getString(gl.VERSION))
            //println(gl.versionString)
            if (reshaped) {
                reshaped = false
                //println("RESHAPED!")
                dispatchReshapeEventEx(
                    ag.mainRenderBuffer.x,
                    ag.mainRenderBuffer.y,
                    ag.mainRenderBuffer.width,
                    ag.mainRenderBuffer.height,
                    ag.mainRenderBuffer.fullWidth,
                    ag.mainRenderBuffer.fullHeight,
                )
            }

            //gl.clearColor(1f, 1f, 1f, 1f)
            //gl.clear(gl.COLOR_BUFFER_BIT)
            updateGamepads()
            frame()
            gl.flush()
            gl.finish()
        }
    }

    private fun updateGamepads() {
        when {
            OS.isWindows -> {
                xinputEventAdapter.updateGamepadsWin32(this)
            }
            OS.isLinux -> {
                linuxJoyEventAdapter.updateGamepads(this)
            }
            OS.isMac -> {
                macosGamepadEventAdapter.updateGamepads(this)
            }
            else -> {
                //println("undetected OS: ${OS.rawName}")
            }
        }
    }

    private val xinputEventAdapter by lazy { XInputEventAdapter() }
    private val linuxJoyEventAdapter by lazy { LinuxJoyEventAdapter() }
    private val macosGamepadEventAdapter by lazy { MacosGamepadEventAdapter() }


    val frameScaleFactor: Double
        get() = run {
            getDisplayScalingFactor(component)
            //val res = frame.toolkit.getDesktopProperty("apple.awt.contentScaleFactor") as? Number
            //if (res != null) return res.toDouble()
        }

    val nonScaledWidth get() = contentComponent.width.toDouble()
    val nonScaledHeight get() = contentComponent.height.toDouble()

    val scaledWidth get() = contentComponent.width * frameScaleFactor
    val scaledHeight get() = contentComponent.height * frameScaleFactor

    override val width: Int get() = (scaledWidth).toInt()
    override val height: Int get() = (scaledHeight).toInt()

    override var visible: Boolean
        get() = component.isVisible
        set(value) {
            component.isVisible = value
        }
    override var quality: Quality = Quality.AUTOMATIC

    override suspend fun browse(url: URL) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url.toString()));
        }
    }

    override suspend fun alert(message: String) {
        JOptionPane.showMessageDialog(component, message, "Message", JOptionPane.WARNING_MESSAGE)
    }

    override suspend fun confirm(message: String): Boolean {
        return JOptionPane.showConfirmDialog(component, message, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION
    }

    override suspend fun prompt(message: String, default: String): String {
        return JOptionPane.showInputDialog(component, message, "Input", JOptionPane.PLAIN_MESSAGE, null, null, default).toString()
    }

    override suspend fun openFileDialog(filter: String?, write: Boolean, multi: Boolean): List<VfsFile> {
        //val chooser = JFileChooser()
        val mode = if (write) FileDialog.SAVE else FileDialog.LOAD
        val chooser = FileDialog(this.component.getContainerFrame(), "Select file", mode)
        chooser.setLocationRelativeTo(null)
        //chooser.fileFilter = filter // @TODO: Filters
        chooser.isMultipleMode = multi
        //chooser.isMultiSelectionEnabled = multi
        chooser.isVisible = true
        return chooser.files.map { localVfs(it) }
    }

    fun dispatchReshapeEvent() {
        val factor = frameScaleFactor
        dispatchReshapeEvent(
            component.x,
            component.y,
            (contentComponent.width * factor).toInt().coerceAtLeast(1),
            (contentComponent.height * factor).toInt().coerceAtLeast(1)
        )
    }

    val displayLinkData = Memory(16L).also { it.clear() }
    var displayLinkLock: java.lang.Object? = null
    var displayLink: Pointer? = Pointer.NULL

    // @TODO: by lazy // but maybe causing issues with the intellij plugin?
    val displayLinkCallback = object : DisplayLinkCallback {
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

    open fun loopInitialization() {
    }

    open fun frameDispose() {
    }

    var reshaped = false

    protected var mouseX: Int = 0
    protected var mouseY: Int = 0

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        launchImmediately(getCoroutineDispatcherWithCurrentContext()) {
            entry()
        }
        //frame.setBounds(0, 0, width, height)

        //val timer= Timer(40, ActionListener {
        //})

        loopInitialization()

        component.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                //window.invalidate()
                //SwingUtilities.updateComponentTreeUI(window)
                reshaped = true
                //window.repaint()
                queue {
                    //window.revalidate()
                    //println("repaint")
                    component.repaint()
                    //EventQueue.invokeLater { SwingUtilities.updateComponentTreeUI(window) }
                }
            }
        })

        var waitingRobotEvents = true
        fun handleMouseEvent(e: MouseEvent) {
            val ev = when (e.id) {
                MouseEvent.MOUSE_MOVED -> com.soywiz.korev.MouseEvent.Type.MOVE
                MouseEvent.MOUSE_CLICKED -> com.soywiz.korev.MouseEvent.Type.CLICK
                MouseEvent.MOUSE_PRESSED -> com.soywiz.korev.MouseEvent.Type.DOWN
                MouseEvent.MOUSE_RELEASED -> com.soywiz.korev.MouseEvent.Type.UP
                else -> com.soywiz.korev.MouseEvent.Type.MOVE
            }
            if (waitingRobotEvents) {
                if (ev == com.soywiz.korev.MouseEvent.Type.CLICK) {
                    waitingRobotEvents = false
                }
                return
            }
            //println("MOUSE EVENT: $ev : ${e.button}")
            queue {
                val button = MouseButton[e.button - 1]
                val factor = frameScaleFactor
                val sx = e.x * factor
                val sy = e.y * factor
                val modifiers = e.modifiersEx
                mouseX = e.x
                mouseY = e.y
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
                    scaleCoords = false,
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
                val key = awtKeyCodeToKey(e.keyCode)

                dispatchKeyEventEx(ev, id, char, key, keyCode, e.isShiftDown, e.isControlDown, e.isAltDown, e.isMetaDown)
            }
        }

        contentComponent.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseDragged(e: MouseEvent) = handleMouseEvent(e)
        })

        contentComponent.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseMoved(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseEntered(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseDragged(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseClicked(e: MouseEvent) = handleMouseEvent(e)
            override fun mouseExited(e: MouseEvent) = handleMouseEvent(e)
            override fun mousePressed(e: MouseEvent) = handleMouseEvent(e)
        })

        component.addMouseWheelListener { e -> handleMouseWheelEvent(e) }

        component.setFocusTraversalKeysEnabled(false)
        component.addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) = handleKeyEvent(e)
            override fun keyPressed(e: KeyEvent) = handleKeyEvent(e)
            override fun keyReleased(e: KeyEvent) = handleKeyEvent(e)
        })

        queue {
            dispatchInitEvent()
            dispatchReshapeEvent()
        }
        EventQueue.invokeLater {
            component.isVisible = true
            component.repaint()
            //fullscreen = true

            // keys.up(Key.ENTER) { if (it.alt) gameWindow.toggleFullScreen() }
            if (OS.isWindows) {
                (component as? Frame?)?.apply {
                    val frame = this
                    val insets = frame.insets
                    frame.isAlwaysOnTop = true
                    // @TODO: HACK so the windows grabs focus on Windows 10 at least when launching on gradle daemon
                    try {
                        val robot = Robot()
                        val pos = MouseInfo.getPointerInfo().location
                        val bounds = frame.bounds
                        bounds.setFrameFromDiagonal(bounds.minX + insets.left, bounds.minY + insets.top, bounds.maxX - insets.right, bounds.maxY - insets.bottom)

                        //println("frame.bounds: ${frame.bounds}")
                        //println("frame.bounds: ${bounds}")
                        //println("frame.insets: ${insets}")
                        //println(frame.contentPane.bounds)
                        //println("START ROBOT")
                        waitingRobotEvents = true
                        robot.mouseMove(bounds.centerX.toInt(), bounds.centerY.toInt())
                        robot.mousePress(InputEvent.BUTTON3_MASK)
                        robot.mouseRelease(InputEvent.BUTTON3_MASK)
                        robot.mouseMove(pos.x, pos.y)
                        //println("END ROBOT")
                    } catch (e: Throwable) {
                    }
                    frame.isAlwaysOnTop = false
                }
            }
        }

        //val timer = Timer(1000 / 60, ActionListener { component.repaint() })
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

        println("running: ${Thread.currentThread()}")

        while (running) {
            if (fvsync) {
                Thread.sleep(1L)
            } else {
                //println("running[a]")
                component.repaint()
                //executePending()
                //println("running[b]")
                when {
                    //ctx?.supportsSwapInterval() == true -> {
                    false -> {
                        //println("running[ba]")
                        Unit
                    } // Do nothing. Already waited for vsync
                    displayLock != null -> {
                        //println("running[bc]")
                        synchronized(displayLock) { displayLock.wait(100L) }
                    }
                    else -> {
                        //println("running[bb]")
                        val nanos = System.nanoTime()
                        val frameTimeNanos = (1.0 / fps.toDouble()).seconds.nanosecondsInt
                        val delayNanos = frameTimeNanos - (nanos % frameTimeNanos)
                        if (delayNanos > 0) {
                            //println(delayNanos / 1_000_000)
                            Thread.sleep(delayNanos / 1_000_000, (delayNanos % 1_000_000).toInt())
                        }
                        //println("[2] currentFrameCount=$currentFrameCount, frameCount=$frameCount")

                        //println(System.nanoTime())
                    }
                }
                //println("running[c]")
                //val end = PerformanceCounter.hr
                //println((end - start).timeSpan)
            }
        }
        println("completed. running=$running")
        //timer.stop()

        if (OS.isMac && displayLink != Pointer.NULL) {
            CoreGraphics.CVDisplayLinkStop(displayLink)
        }

        dispatchDestroyEvent()

        component.isVisible = false
        frameDispose()

        //exitProcess(0) // Don't do this since we might continue in the e2e test
    }

    override fun computeDisplayRefreshRate(): Int {
        val window = this.window ?: return 60
        return window.getScreenDevice().cachedRefreshRate
    }
}
