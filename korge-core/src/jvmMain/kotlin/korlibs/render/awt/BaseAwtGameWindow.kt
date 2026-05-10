package korlibs.render.awt

import korlibs.datastructure.*
import korlibs.event.*
import korlibs.graphics.*
import korlibs.graphics.gl.*
import korlibs.image.color.*
import korlibs.io.async.*
import korlibs.kgl.*
import korlibs.memory.*
import korlibs.platform.*
import korlibs.render.*
import korlibs.render.osx.*
import korlibs.render.platform.*
import korlibs.render.win32.*
import korlibs.render.x11.*
import korlibs.time.*
import kotlinx.coroutines.*
import java.awt.*
import java.awt.datatransfer.*
import java.awt.event.*
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import kotlin.time.Duration
import javax.swing.*
import kotlin.system.*
import kotlin.time.measureTime

abstract class BaseAwtGameWindow(
    override val ag: AGOpengl
) : GameWindow(), ClipboardOwner {
    override val devicePixelRatio: Double get() = component.devicePixelRatio
    override val pixelsPerInch: Double get() = component.pixelsPerInch
    override val pixelsPerLogicalInchRatio: Double by lazy(LazyThreadSafetyMode.PUBLICATION) {
        pixelsPerInch / AG.defaultPixelsPerInch
    }

    //val fvsync get() = vsync
    val fvsync get() = false
    open val ctx: BaseOpenglContext? = null

    abstract val component: Component
    abstract val contentComponent: Component
    private var lastFactor = 0f

    override val dialogInterface: DialogInterface = DialogInterfaceAwt { component }

    private var _window: Window? = null
    val window: Window? get() {
        if (_window == null) {
            _window = SwingUtilities.getWindowAncestor(component) ?: (component as? Window?)
        }
        return _window
    }
    val windowOrComponent get() = window ?: component

    override var cursor: ICursor = Cursor.DEFAULT
        set(value) {
            if (field == value) return
            field = value
            component.cursor = value.jvmCursor
        }

    fun MenuItem?.toJMenuItem(): JComponent {
        val item = this
        return when {
            item?.text == null -> JSeparator()
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
            var gamePadTime: Duration = 0.milliseconds
            var frameTime: Duration = 0.milliseconds
            var finishTime: Duration = 0.milliseconds
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
        DesktopGamepadUpdater.updateGamepads(this)
    }

    val frameScaleFactor: Float get() = getDisplayScalingFactor(component)

    val nonScaledWidth get() = contentComponent.width.toDouble()
    val nonScaledHeight get() = contentComponent.height.toDouble()

    val scaledWidth get() = contentComponent.width * frameScaleFactor
    val scaledHeight get() = contentComponent.height * frameScaleFactor

    override val width: Int get() = (scaledWidth).toInt()
    override val height: Int get() = (scaledHeight).toInt()
    override var visible: Boolean by LazyDelegate { component::visible }
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

    var displayLinkLock: java.lang.Object? = null
    private val dl = OSXDisplayLink {
        displayLinkLock?.let { displayLock ->
            synchronized(displayLock) {
                displayLock.notify()
            }
        }
    }

    open fun frameDispose() {
    }

    var reshaped = false

    protected var mouseX: Int = 0
    protected var mouseY: Int = 0

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        launchImmediately(getCoroutineDispatcherWithCurrentContext() + CoroutineName("BaseAwtGameWindow.loop")) {
            entry()
        }

//frame.setBounds(0, 0, width, height)

        //val timer= Timer(40, ActionListener {
        //})

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
                MouseEvent.MOUSE_DRAGGED -> korlibs.event.MouseEvent.Type.DRAG
                MouseEvent.MOUSE_MOVED -> korlibs.event.MouseEvent.Type.MOVE
                MouseEvent.MOUSE_CLICKED -> korlibs.event.MouseEvent.Type.CLICK
                MouseEvent.MOUSE_PRESSED -> korlibs.event.MouseEvent.Type.DOWN
                MouseEvent.MOUSE_RELEASED -> korlibs.event.MouseEvent.Type.UP
                MouseEvent.MOUSE_ENTERED -> korlibs.event.MouseEvent.Type.ENTER
                MouseEvent.MOUSE_EXITED -> korlibs.event.MouseEvent.Type.EXIT
                else -> korlibs.event.MouseEvent.Type.MOVE
            }
            //println("MOUSE EVENT: $ev : ${e.button} : ${MouseButton[e.button - 1]}")
            queue {
                val button = if (e.button == 0) MouseButton.NONE else MouseButton[e.button - 1]
                val factor = frameScaleFactor

                if (locking) {
                    Robot().mouseMove(lockingX, lockingY)
                    if (ev == korlibs.event.MouseEvent.Type.UP) {
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
                    scrollDeltaX = 0f,
                    scrollDeltaY = 0f,
                    scrollDeltaZ = 0f,
                    scrollDeltaMode = korlibs.event.MouseEvent.ScrollDeltaMode.PIXEL,
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
                val ev = korlibs.event.MouseEvent.Type.SCROLL
                val button = MouseButton[8]
                val factor = frameScaleFactor
                val sx = e.x * factor
                val sy = e.y * factor
                val modifiers = e.modifiersEx
                //TODO: check this on linux and macos
                //val scrollDelta = e.scrollAmount * e.preciseWheelRotation // * e.unitsToScroll
                val osfactor = when {
                    Platform.isMac -> 0.25f
                    else -> 1.0f
                }
                val scrollDelta = e.preciseWheelRotation.toFloat() * osfactor

                val isShiftDown = modifiers hasFlags MouseEvent.SHIFT_DOWN_MASK
                dispatchMouseEvent(
                    type = ev,
                    id = 0,
                    x = sx.toInt(),
                    y = sy.toInt(),
                    button = button,
                    buttons = 0,
                    scrollDeltaX = if (isShiftDown) scrollDelta else 0f,
                    scrollDeltaY = if (isShiftDown) 0f else scrollDelta,
                    scrollDeltaZ = 0f,
                    scrollDeltaMode = korlibs.event.MouseEvent.ScrollDeltaMode.PIXEL,
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
                    KeyEvent.KEY_TYPED -> korlibs.event.KeyEvent.Type.TYPE
                    KeyEvent.KEY_PRESSED -> korlibs.event.KeyEvent.Type.DOWN
                    KeyEvent.KEY_RELEASED -> korlibs.event.KeyEvent.Type.UP
                    else -> korlibs.event.KeyEvent.Type.TYPE
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

        if (Platform.isMac) dl.start()
        val displayLock = this.displayLinkLock
        logger.info { if (displayLock != null) "Using DisplayLink" else "NOT Using DisplayLink" }
        logger.info { "running: ${Thread.currentThread()}, fvsync=$fvsync" }
        contentComponent.registerGestureListeners(this@BaseAwtGameWindow)

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

        if (Platform.isMac) {
            dl.stop()
        }

        dispatchDestroyEvent()

        component.isVisible = false
        frameDispose()

        if (exitProcessOnClose) { // Don't do this since we might continue in the e2e test
            exitProcess(this.exitCode)
        }
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
    override fun hapticFeedbackGenerate(kind: HapticFeedbackKind) = component.hapticFeedbackGenerate(kind)
}
