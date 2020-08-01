package com.soywiz.korgw.awt

import com.soywiz.kgl.*
import com.soywiz.klock.hr.*
import com.soywiz.kmem.*
import com.soywiz.korev.*
import com.soywiz.korgw.*
import com.soywiz.korgw.osx.*
import com.soywiz.korgw.platform.*
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

abstract class BaseAwtGameWindow : GameWindow() {
    abstract override val ag: AwtAg
    //val fvsync get() = vsync
    val fvsync get() = false
    open val ctx: BaseOpenglContext? = null

    abstract val component: Component
    abstract val contentComponent: Component
    private var lastFactor = 0.0

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

        ctx?.useContext(g, ag) { info ->
            ctx?.swapInterval(1)

            val gl = ag.gl
            val factor = frameScaleFactor
            if (lastFactor != factor) {
                lastFactor = factor
                dispatchReshapeEvent()
            }

            //println("RENDER[1]")

            val viewport = info.viewport
            val scissor = info.scissors
            gl.enableDisable(gl.SCISSOR_TEST, scissor != null)
            if (scissor != null) {
                //ag.forcedScissor = info.scissors
                //gl.scissor(scissor.x.toInt(), scissor.y.toInt(), scissor.width.toInt(), scissor.height.toInt())
                //println("SCISSOR: $scissor")
            }
            if (viewport != null) {
                //println("FACTOR: $factor, nonScaledWidth=$nonScaledWidth, nonScaledHeight=$nonScaledHeight, scaledWidth=$scaledWidth, scaledHeight=$scaledHeight")
                //gl.viewport(viewport.x.toInt(), viewport.y.toInt(), viewport.width.toInt(), viewport.height.toInt())
            } else {
                //gl.viewport(0, 0, scaledWidth.toInt().coerceAtLeast(1), scaledHeight.toInt().coerceAtLeast(1))
            }

            println(viewport)

            //gl.viewport(0, 0, scaledWidth.toInt().coerceAtLeast(1), scaledHeight.toInt().coerceAtLeast(1))

            //gl.clearColor(.2f, .4f, .9f, 1f)
            gl.clearColor(.3f, .3f, .3f, 1f)
            gl.clear(gl.COLOR_BUFFER_BIT)
            //println(gl.getString(gl.VERSION))
            //println(gl.versionString)
            frame()
            gl.flush()
            gl.finish()
        }
        //Toolkit.getDefaultToolkit().sync();
    }



    val frameScaleFactor: Double get() = run {
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
        val chooser = JFileChooser()
        //chooser.fileFilter = filter // @TODO: Filters
        chooser.isMultiSelectionEnabled = multi
        val result = if (write) {
            chooser.showSaveDialog(component)
        } else {
            chooser.showOpenDialog(component)
        } == JFileChooser.APPROVE_OPTION
        return if (result) chooser.selectedFiles.map { localVfs(it) } else listOf()
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
                queue {
                    dispatchReshapeEvent()
                    component.repaint()
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
                val key = AwtKeyMap[e.keyCode] ?: Key.UNKNOWN
                dispatchKeyEvent(ev, id, char, key, keyCode)
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
        }

        EventQueue.invokeLater {
            //println("repaint!")
            component.repaint()
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
}
