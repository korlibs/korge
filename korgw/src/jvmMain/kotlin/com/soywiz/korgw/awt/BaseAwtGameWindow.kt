package com.soywiz.korgw.awt

import com.soywiz.kds.*
import com.soywiz.kgl.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.kmem.Platform
import com.soywiz.kmem.dyn.osx.*
import com.soywiz.korag.*
import com.soywiz.korag.gl.*
import com.soywiz.korev.*
import com.soywiz.korgw.*
import com.soywiz.korgw.osx.*
import com.soywiz.korgw.platform.*
import com.soywiz.korgw.win32.*
import com.soywiz.korgw.x11.*
import com.soywiz.korim.awt.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.dynamic.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.sun.jna.*
import kotlinx.coroutines.*
import java.awt.*
import java.awt.datatransfer.*
import java.awt.event.*
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.*
import kotlin.system.*

abstract class BaseAwtGameWindow(
    override val ag: AGOpengl
) : GameWindow(), ClipboardOwner {
    private val localGraphicsEnvironment : GraphicsEnvironment by lazy(LazyThreadSafetyMode.PUBLICATION) {
        GraphicsEnvironment.getLocalGraphicsEnvironment()
    }

    override val devicePixelRatio: Double get() {
        if (GraphicsEnvironment.isHeadless()) {
            return super.devicePixelRatio
        }
        // transform
        // https://stackoverflow.com/questions/20767708/how-do-you-detect-a-retina-display-in-java
        val config = component.graphicsConfiguration
            ?: localGraphicsEnvironment.defaultScreenDevice.defaultConfiguration
        return config.defaultTransform.scaleX
    }

    //override val pixelsPerInch: Double by lazy(LazyThreadSafetyMode.PUBLICATION) {
    override val pixelsPerInch: Double get() {
        if (GraphicsEnvironment.isHeadless()) {
            return AG.defaultPixelsPerInch
        }
        // maybe this is not just windows specific :
        // https://stackoverflow.com/questions/32586883/windows-scaling
        // somehow this value is not update when you change the scaling in the windows settings while the jvm is running :(
        return Toolkit.getDefaultToolkit().screenResolution.toDouble()
    }

    override val pixelsPerLogicalInchRatio: Double by lazy(LazyThreadSafetyMode.PUBLICATION) {
        pixelsPerInch / AG.defaultPixelsPerInch
    }

    //val fvsync get() = vsync
    val fvsync get() = false
    open val ctx: BaseOpenglContext? = null

    abstract val component: Component
    abstract val contentComponent: Component
    private var lastFactor = 0.0

    override val dialogInterface: DialogInterface = DialogInterfaceAwt { component }

    private var _window: Window? = null
    val window: Window? get() {
        if (_window == null) {
            _window = SwingUtilities.getWindowAncestor(component) ?: (component as? Window?)
        }
        return _window
    }
    val windowOrComponent get() = window ?: component

    val Cursor.jvmCursor: java.awt.Cursor get() = java.awt.Cursor(when (this) {
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
        else -> java.awt.Cursor.DEFAULT_CURSOR
    })

    val CustomCursor.jvmCursor: java.awt.Cursor by extraPropertyThis {
        val toolkit = Toolkit.getDefaultToolkit()
        val size = toolkit.getBestCursorSize(bounds.width.toIntCeil(), bounds.height.toIntCeil())
        val result = this.createBitmap(MSize(size.width, size.height))
        //println("BITMAP SIZE=${result.bitmap.size}, hotspot=${result.hotspot}")
        val hotspotX = result.hotspot.x.toInt().coerceIn(0, result.bitmap.width - 1)
        val hotspotY = result.hotspot.y.toInt().coerceIn(0, result.bitmap.height - 1)
        toolkit.createCustomCursor(result.bitmap.toAwt(), java.awt.Point(hotspotX, hotspotY), name).also {
            //println("CUSTOM CURSOR: $it")
        }
    }

    override var cursor: ICursor = Cursor.DEFAULT
        set(value) {
            if (field == value) return
            field = value
            component.cursor = when (value) {
                is Cursor -> value.jvmCursor
                is CustomCursor -> value.jvmCursor
                else -> java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR)
            }
        }

    fun MenuItem?.toJMenuItem(): JComponent {
        val item = this
        return when {
            item == null || item.text == null -> JSeparator()
            item.children != null -> {
                JMenu(item.text).also {
                    it.isEnabled = item.enabled
                    it.addActionListener { item.action() }
                    for (child in item.children) {
                        it.add(child.toJMenuItem())
                    }
                }
            }
            else -> JMenuItem(item.text).also {
                it.isEnabled = item.enabled
                it.addActionListener { item.action() }
            }
        }
    }

    override fun setMainMenu(items: List<MenuItem>) {
        val component = this.component
        if (component !is JFrame) {
            println("GameWindow.setMainMenu: component=$component")
            return
        }

        val bar = JMenuBar()
        for (item in items) {
            val mit = item.toJMenuItem()
            if (mit is JMenu) {
                bar.add(mit)
            }
        }
        component.jMenuBar = bar
        component.doLayout()
        component.repaint()
        println("GameWindow.setMainMenu: component=$component, bar=$bar")
    }

    override fun showContextMenu(items: List<MenuItem>) {
        val popupMenu = JPopupMenu()
        for (item in items) {
            popupMenu.add(item.toJMenuItem())
        }
        //println("showContextMenu: $items")
        popupMenu.setLightWeightPopupEnabled(false)
        popupMenu.show(contentComponent, mouseX, mouseY)
    }

    protected open fun ensureContext() {
    }

    fun framePaint(g: Graphics) {
        //println("framePaint")

        if (fvsync) {
            EventQueue.invokeLater {
                //println("repaint!")
                component.repaint()
            }
        }

        ensureContext()

        ctx?.useContext(g, ag, paintInContextDelegate)
        //Toolkit.getDefaultToolkit().sync();
    }

    val paintInContextDelegate: (Graphics, BaseOpenglContext.ContextInfo) -> Unit = { g, info ->
        paintInContext(g, info)
    }

    val state: KmlGlState by lazy { ag.createGlState() }

    fun paintInContext(g: Graphics, info: BaseOpenglContext.ContextInfo) {
        // We only have to keep the state on mac, in linux and windows, the context/state is not shared
        if (Platform.isMac) {
            state.keep {
                paintInContextInternal(g, info)
            }
        } else {
            paintInContextInternal(g, info)
        }
    }

    fun paintInContextInternal(g: Graphics, info: BaseOpenglContext.ContextInfo) {
        run {
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

            //println("RENDER: $info, factor=$factor")

            if (component is JFrame) {
                //println("component.width: ${contentComponent.width}x${contentComponent.height}")
                ag.mainFrameBuffer.setSize(
                    0, 0, (contentComponent.width * factor).toInt(), (contentComponent.height * factor).toInt(),
                )
            } else {
                ag.mainFrameBuffer.scissor(scissor)
                if (viewport != null) {
                    //val window = SwingUtilities.getWindowAncestor(contentComponent)
                    //println("window=${window.width}x${window.height} : factor=$factor")

                    val frameOrComponent = (window as? JFrame)?.contentPane ?: windowOrComponent

                    ag.mainFrameBuffer.setSize(
                        viewport.x, viewport.y, viewport.width, viewport.height,
                        (frameOrComponent.width * factor).toInt(),
                        (frameOrComponent.height * factor).toInt(),
                    )
                } else {
                    ag.mainFrameBuffer.setSize(
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
                    ag.mainFrameBuffer.x,
                    ag.mainFrameBuffer.y,
                    ag.mainFrameBuffer.width,
                    ag.mainFrameBuffer.height,
                    ag.mainFrameBuffer.fullWidth,
                    ag.mainFrameBuffer.fullHeight,
                )
            }

            //gl.clearColor(1f, 1f, 1f, 1f)
            //gl.clear(gl.COLOR_BUFFER_BIT)
            var gamePadTime: TimeSpan = 0.milliseconds
            var frameTime: TimeSpan = 0.milliseconds
            var finishTime: TimeSpan = 0.milliseconds
            val totalTime = measureTime {
                frameTime = measureTime {
                    frame()
                }
                finishTime = measureTime {
                    gl.flush()
                    gl.finish()
                }
            }

            //println("totalTime=$totalTime, gamePadTime=$gamePadTime, finishTime=$finishTime, frameTime=$frameTime, timedTasksTime=${coroutineDispatcher.timedTasksTime}, tasksTime=${coroutineDispatcher.tasksTime}, renderTime=${renderTime}, updateTime=${updateTime}")
        }
    }

    override fun updateGamepads() {
        when {
            Platform.isWindows -> xinputEventAdapter.updateGamepadsWin32(this.gamepadEmitter)
            Platform.isLinux -> linuxJoyEventAdapter.updateGamepads(this.gamepadEmitter)
            Platform.isMac -> macosGamepadEventAdapter.updateGamepads(this)
            else -> Unit //println("undetected OS: ${OS.rawName}")
        }
    }

    private val xinputEventAdapter by lazy { XInputEventAdapter() }
    private val linuxJoyEventAdapter by lazy { LinuxJoyEventAdapter() }
    private val macosGamepadEventAdapter by lazy { MacosGamepadEventAdapter() }


    val frameScaleFactor: Double
        get() {
            return getDisplayScalingFactor(component)
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
    override var bgcolor: RGBA
        get() = component.background.toRgba()
        set(value) {
            component.background = value.toAwt()
        }
    override var quality: Quality = Quality.AUTOMATIC

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

        var lastMouseX: Int = 0
        var lastMouseY: Int = 0
        var lockingX: Int = 0
        var lockingY: Int = 0
        var locking = false

        mouseEvent.requestLock = {
            val location = MouseInfo.getPointerInfo().location
            lockingX = location.x
            lockingY = location.y
            locking = true
        }

        fun handleMouseEvent(e: MouseEvent) {
            val ev = when (e.id) {
                MouseEvent.MOUSE_DRAGGED -> com.soywiz.korev.MouseEvent.Type.DRAG
                MouseEvent.MOUSE_MOVED -> com.soywiz.korev.MouseEvent.Type.MOVE
                MouseEvent.MOUSE_CLICKED -> com.soywiz.korev.MouseEvent.Type.CLICK
                MouseEvent.MOUSE_PRESSED -> com.soywiz.korev.MouseEvent.Type.DOWN
                MouseEvent.MOUSE_RELEASED -> com.soywiz.korev.MouseEvent.Type.UP
                MouseEvent.MOUSE_ENTERED -> com.soywiz.korev.MouseEvent.Type.ENTER
                MouseEvent.MOUSE_EXITED -> com.soywiz.korev.MouseEvent.Type.EXIT
                else -> com.soywiz.korev.MouseEvent.Type.MOVE
            }
            //println("MOUSE EVENT: $ev : ${e.button} : ${MouseButton[e.button - 1]}")
            queue {
                val button = if (e.button == 0) MouseButton.NONE else MouseButton[e.button - 1]
                val factor = frameScaleFactor

                if (locking) {
                    Robot().mouseMove(lockingX, lockingY)
                    if (ev == com.soywiz.korev.MouseEvent.Type.UP) {
                        locking = false
                    }
                }

                lastMouseX = e.x
                lastMouseY = e.y
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
                    scrollDeltaMode = com.soywiz.korev.MouseEvent.ScrollDeltaMode.PIXEL,
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
                //val scrollDelta = e.scrollAmount * e.preciseWheelRotation // * e.unitsToScroll
                val osfactor = when {
                    Platform.isMac -> 0.25
                    else -> 1.0
                }
                val scrollDelta = e.preciseWheelRotation * osfactor

                val isShiftDown = modifiers hasFlags MouseEvent.SHIFT_DOWN_MASK
                dispatchMouseEvent(
                    type = ev,
                    id = 0,
                    x = sx.toInt(),
                    y = sy.toInt(),
                    button = button,
                    buttons = 0,
                    scrollDeltaX = if (isShiftDown) scrollDelta else 0.0,
                    scrollDeltaY = if (isShiftDown) 0.0 else scrollDelta,
                    scrollDeltaZ = 0.0,
                    scrollDeltaMode = com.soywiz.korev.MouseEvent.ScrollDeltaMode.PIXEL,
                    isShiftDown = isShiftDown,
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

        component.addMouseWheelListener { e -> handleMouseWheelEvent(e) }

        component.focusTraversalKeysEnabled = false
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

            // @TODO: HACK so the windows grabs focus on Windows 10 when launching on gradle daemon
            val useRobotHack = Platform.isWindows

            if (useRobotHack) {
                (component as? Frame?)?.apply {
                    val frame = this
                    val insets = frame.insets
                    frame.isAlwaysOnTop = true
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

            EventQueue.invokeLater {
                // Here all the robot events have been already processed so they won't be processed
                //println("END ROBOT2")

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

            }

        }

        //val timer = Timer(1000 / 60, ActionListener { component.repaint() })
        //timer.start()

        //val toolkit = Toolkit.getDefaultToolkit()
        //val events = toolkit.systemEventQueue

        if (Platform.isMac) {
            try {
                val displayID = CoreGraphics.CGMainDisplayID()
                val res = CoreVideo.CVDisplayLinkCreateWithCGDisplay(displayID, displayLinkData)

                if (res == 0) {
                    displayLinkLock = java.lang.Object()
                    displayLink = displayLinkData.getPointer(0L)
                    if (CoreVideo.CVDisplayLinkSetOutputCallback(displayLink, displayLinkCallback, Pointer.NULL) == 0) {
                        CoreVideo.CVDisplayLinkStart(displayLink)
                    } else {
                        displayLinkLock = null
                        displayLink = Pointer.NULL
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        //Thread.sleep(1000000L)

        val displayLock = this.displayLinkLock

        if (displayLock != null) {
            logger.info { "Using DisplayLink" }
        } else {
            logger.info { "NOT Using DisplayLink" }
        }

        logger.info { "running: ${Thread.currentThread()}, fvsync=$fvsync" }

        if (Platform.isMac) {
            try {
                registerGestureListener()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

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
        logger.info { "completed.running=$running" }
        //timer.stop()

        if (Platform.isMac && displayLink != Pointer.NULL) {
            CoreVideo.CVDisplayLinkStop(displayLink)
        }

        dispatchDestroyEvent()

        component.isVisible = false
        frameDispose()

        if (exitProcessOnExit) { // Don't do this since we might continue in the e2e test
            exitProcess(this.exitCode)
        }
    }

    private fun registerGestureListener() {
        logger.info { "MacOS registering gesture listener..." }

        val gestureListener = java.lang.reflect.Proxy.newProxyInstance(
            ClassLoader.getSystemClassLoader(),
            arrayOf(
                Class.forName("com.apple.eawt.event.GestureListener"),
                Class.forName("com.apple.eawt.event.MagnificationListener"),
                Class.forName("com.apple.eawt.event.RotationListener"),
                Class.forName("com.apple.eawt.event.SwipeListener"),
            )
        ) { proxy, method, args ->
            try {
                when (method.name) {
                    "magnify" -> {
                        val magnification = args[0].dyn.dynamicInvoke("getMagnification")
                        //println("magnify: $magnification")
                        queue {
                            dispatch(gestureEvent.also {
                                it.type = GestureEvent.Type.MAGNIFY
                                it.id = 0
                                it.amount = magnification.double
                            })
                        }
                    }

                    "rotate" -> {
                        val rotation = args[0].dyn.dynamicInvoke("getRotation")
                        //println("rotate: $rotation")
                        queue {
                            dispatch(gestureEvent.also {
                                it.type = GestureEvent.Type.ROTATE
                                it.id = 0
                                it.amount = rotation.double
                            })
                        }
                    }

                    "swipedUp", "swipedDown", "swipedLeft", "swipedRight" -> {
                        queue {
                            dispatch(gestureEvent.also {
                                it.type = GestureEvent.Type.SWIPE
                                it.id = 0
                                it.amountX = 0.0
                                it.amountY = 0.0
                                when (method.name) {
                                    "swipedUp" -> it.amountY = -1.0
                                    "swipedDown" -> it.amountY = +1.0
                                    "swipedLeft" -> it.amountX = -1.0
                                    "swipedRight" -> it.amountX = +1.0
                                }
                            })
                        }
                    }

                    else -> {
                        //println("gestureListener: $method, ${args.toList()}")
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            args[0].dyn.dynamicInvoke("consume")
        }

        val clazz = Dyn.global["com.apple.eawt.event.GestureUtilities"]
        logger.info { " -- GestureUtilities=$clazz" }
        clazz.dynamicInvoke("addGestureListenerTo", contentComponent, gestureListener)

        //val value = (contentComponent as JComponent).getClientProperty("com.apple.eawt.event.internalGestureHandler");
        //println("value $value");
        //GestureUtilities.addGestureListenerTo(p, ga);
    }

    override fun computeDisplayRefreshRate(): Int {
        return window?.getScreenDevice()?.cachedRefreshRate?.takeIf { it > 0 } ?: 60
    }

    val clipboard: Clipboard by lazy { Toolkit.getDefaultToolkit().systemClipboard }

    suspend fun <T> eventQueueLater(block: () -> T): T {
        val deferred = CompletableDeferred<T>()
        EventQueue.invokeLater {
            deferred.completeWith(runCatching(block))
        }
        return deferred.await()
    }

    override suspend fun clipboardWrite(data: ClipboardData) {
        eventQueueLater {
            when (data) {
                is TextClipboardData -> {
                    clipboard.setContents(StringSelection(data.text), this)
                }
            }
        }
    }

    override suspend fun clipboardRead(): ClipboardData? {
        return eventQueueLater {
            val str = clipboard.getData(DataFlavor.stringFlavor) as? String?
            str?.let { TextClipboardData(it) }
        }
    }

    override fun lostOwnership(clipboard: Clipboard?, contents: Transferable?) {
    }

    override val hapticFeedbackGenerateSupport: Boolean get() = true
    override fun hapticFeedbackGenerate(kind: HapticFeedbackKind) {
        when {
            Platform.os.isMac -> {
                val KIND_GENERIC = 0
                val KIND_ALIGNMENT = 1
                val KIND_LEVEL_CHANGE = 2

                val PERFORMANCE_TIME_DEFAULT = 0
                val PERFORMANCE_TIME_NOW = 1
                val PERFORMANCE_TIME_DRAW_COMPLETED = 2

                val kindInt = when (kind) {
                    HapticFeedbackKind.GENERIC -> KIND_GENERIC
                    HapticFeedbackKind.ALIGNMENT -> KIND_ALIGNMENT
                    HapticFeedbackKind.LEVEL_CHANGE -> KIND_LEVEL_CHANGE
                }
                val performanceTime = PERFORMANCE_TIME_NOW

                NSClass("NSHapticFeedbackManager")
                    .msgSend("defaultPerformer")
                    .msgSend("performFeedbackPattern:performanceTime:", kindInt.toLong(), performanceTime.toLong())
            }
            else -> {
                super.hapticFeedbackGenerate(kind)
            }
        }
    }
}
