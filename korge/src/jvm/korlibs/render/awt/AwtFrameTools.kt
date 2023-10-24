package korlibs.render.awt

import korlibs.datastructure.*
import korlibs.event.*
import korlibs.event.MouseEvent
import korlibs.ffi.osx.*
import korlibs.graphics.*
import korlibs.image.awt.*
import korlibs.io.dynamic.*
import korlibs.io.file.*
import korlibs.io.file.std.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.memory.*
import korlibs.platform.*
import korlibs.render.*
import korlibs.render.MenuItem
import korlibs.render.platform.*
import java.awt.*
import java.awt.Point
import java.awt.datatransfer.*
import java.awt.dnd.*
import java.awt.event.*
import java.awt.image.*
import java.io.*
import javax.imageio.*
import javax.swing.*
import java.awt.event.KeyEvent as JKeyEvent
import java.awt.event.MouseEvent as JMouseEvent

object AwtFrameTools

fun JFrame.setIconIncludingTaskbarFromResource(path: String) {
    runCatching {
        val awtImageURL = AwtFrameTools::class.java.getResource("/$path")
            ?: AwtFrameTools::class.java.getResource(path)
            ?: ClassLoader.getSystemResource(path)
        setIconIncludingTaskbarFromImage(awtImageURL?.let { ImageIO.read(it) })
    }
}

fun JFrame.setIconIncludingTaskbarFromImage(awtImage: BufferedImage?) {
    val frame = this
    runCatching {
        frame.iconImage = awtImage?.getScaledInstance(32, 32, Image.SCALE_SMOOTH)
        Dyn.global["java.awt.Taskbar"].dynamicInvoke("getTaskbar").dynamicInvoke("setIconImage", awtImage)
    }
}

var JFrame._isAlwaysOnTop: Boolean
    get() = isAlwaysOnTop
    set(value) {
        isAlwaysOnTop = value
    }

var Component.visible: Boolean
    get() = isVisible
    set(value) {
        isVisible = value
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

fun Component.setKorgeDropTarget(dispatcher: EventListener) {
    val dropFileEvent = DropFileEvent()
    fun dispatchDropfileEvent(type: DropFileEvent.Type, files: List<VfsFile>?) = dispatcher.dispatch(dropFileEvent.reset {
        this.type = type
        this.files = files
    })
    dropTarget = object : DropTarget() {
        init {
            this.addDropTargetListener(object : DropTargetAdapter() {
                override fun drop(dtde: DropTargetDropEvent) {
                    //println("drop")
                    dtde.acceptDrop(DnDConstants.ACTION_COPY)
                    dispatchDropfileEvent(DropFileEvent.Type.DROP, (dtde.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>).map { it.toVfs() })
                    dispatchDropfileEvent(DropFileEvent.Type.END, null)
                }
            })
        }

        override fun dragEnter(dtde: DropTargetDragEvent) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY)
            //dispatchDropfileEvent(DropFileEvent.Type.ENTER, null)
            dispatchDropfileEvent(DropFileEvent.Type.START, null)
            //println("dragEnter")
            super.dragEnter(dtde)
        }

        override fun dragOver(dtde: DropTargetDragEvent) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY)
            super.dragOver(dtde)
        }

        override fun dragExit(dte: DropTargetEvent) {
            //dispatchDropfileEvent(DropFileEvent.Type.EXIT, null)
            dispatchDropfileEvent(DropFileEvent.Type.END, null)
            super.dragExit(dte)
        }
    }
}

val Component.devicePixelRatio: Double get() {
    if (GraphicsEnvironment.isHeadless()) {
        return 1.0
    } else {
        // transform
        // https://stackoverflow.com/questions/20767708/how-do-you-detect-a-retina-display-in-java
        val config = graphicsConfiguration
            ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
        return config.defaultTransform.scaleX
    }
}

val Component.pixelsPerInch: Double get() {
    if (GraphicsEnvironment.isHeadless()) {
        return AG.defaultPixelsPerInch
    } else {
        // maybe this is not just windows specific :
        // https://stackoverflow.com/questions/32586883/windows-scaling
        // somehow this value is not update when you change the scaling in the windows settings while the jvm is running :(
        return Toolkit.getDefaultToolkit().screenResolution.toDouble()
    }
}


val korlibs.render.Cursor.jvmCursor: java.awt.Cursor get() = java.awt.Cursor(when (this) {
    korlibs.render.Cursor.DEFAULT -> java.awt.Cursor.DEFAULT_CURSOR
    korlibs.render.Cursor.CROSSHAIR -> java.awt.Cursor.CROSSHAIR_CURSOR
    korlibs.render.Cursor.TEXT -> java.awt.Cursor.TEXT_CURSOR
    korlibs.render.Cursor.HAND -> java.awt.Cursor.HAND_CURSOR
    korlibs.render.Cursor.MOVE -> java.awt.Cursor.MOVE_CURSOR
    korlibs.render.Cursor.WAIT -> java.awt.Cursor.WAIT_CURSOR
    korlibs.render.Cursor.RESIZE_EAST -> java.awt.Cursor.E_RESIZE_CURSOR
    korlibs.render.Cursor.RESIZE_SOUTH -> java.awt.Cursor.S_RESIZE_CURSOR
    korlibs.render.Cursor.RESIZE_WEST -> java.awt.Cursor.W_RESIZE_CURSOR
    korlibs.render.Cursor.RESIZE_NORTH -> java.awt.Cursor.N_RESIZE_CURSOR
    korlibs.render.Cursor.RESIZE_NORTH_EAST -> java.awt.Cursor.NE_RESIZE_CURSOR
    korlibs.render.Cursor.RESIZE_NORTH_WEST -> java.awt.Cursor.NW_RESIZE_CURSOR
    korlibs.render.Cursor.RESIZE_SOUTH_EAST -> java.awt.Cursor.SE_RESIZE_CURSOR
    korlibs.render.Cursor.RESIZE_SOUTH_WEST -> java.awt.Cursor.SW_RESIZE_CURSOR
    else -> java.awt.Cursor.DEFAULT_CURSOR
})

val CustomCursor.jvmCursor: java.awt.Cursor by extraPropertyThis {
    val toolkit = Toolkit.getDefaultToolkit()
    val size = toolkit.getBestCursorSize(bounds.width.toIntCeil(), bounds.height.toIntCeil())
    val result = this.createBitmap(Size(size.width, size.height))
    //println("BITMAP SIZE=${result.bitmap.size}, hotspot=${result.hotspot}")
    val hotspotX = result.hotspot.x.coerceIn(0, result.bitmap.width - 1)
    val hotspotY = result.hotspot.y.coerceIn(0, result.bitmap.height - 1)
    toolkit.createCustomCursor(result.bitmap.toAwt(), Point(hotspotX, hotspotY), name).also {
        //println("CUSTOM CURSOR: $it")
    }
}

val ICursor.jvmCursor: java.awt.Cursor get() {
    return when (this) {
        is korlibs.render.Cursor -> this.jvmCursor
        is CustomCursor -> this.jvmCursor
        else -> java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR)
    }
}

fun Component.hapticFeedbackGenerate(kind: HapticFeedbackKind) {
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
                .msgSendRef("defaultPerformer")
                .msgSendVoid("performFeedbackPattern:performanceTime:", kindInt.toLong(), performanceTime.toLong())
        }
        else -> {
            Unit
        }
    }
}

//fun Component.registerGestureListeners(dispatcher: EventListener) {
fun Component.registerGestureListeners(dispatcher: GameWindow) {
    if (!Platform.isMac) return

    val contentComponent = this
    val gestureEvent = GestureEvent()
    try {
        GameWindow.logger.info { "MacOS registering gesture listener..." }

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
                        dispatcher.queue {
                            dispatcher.dispatch(gestureEvent.also {
                                it.type = GestureEvent.Type.MAGNIFY
                                it.id = 0
                                it.amount = magnification.float
                            })
                        }
                    }

                    "rotate" -> {
                        val rotation = args[0].dyn.dynamicInvoke("getRotation")
                        //println("rotate: $rotation")
                        dispatcher.queue {
                            dispatcher.dispatch(gestureEvent.also {
                                it.type = GestureEvent.Type.ROTATE
                                it.id = 0
                                it.amount = rotation.float
                            })
                        }
                    }

                    "swipedUp", "swipedDown", "swipedLeft", "swipedRight" -> {
                        dispatcher.queue {
                            dispatcher.dispatch(gestureEvent.also {
                                it.type = GestureEvent.Type.SWIPE
                                it.id = 0
                                it.amountX = 0f
                                it.amountY = 0f
                                when (method.name) {
                                    "swipedUp" -> it.amountY = -1f
                                    "swipedDown" -> it.amountY = +1f
                                    "swipedLeft" -> it.amountX = -1f
                                    "swipedRight" -> it.amountX = +1f
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
        GameWindow.logger.info { " -- GestureUtilities=$clazz" }
        clazz.dynamicInvoke("addGestureListenerTo", contentComponent, gestureListener)

        //val value = (contentComponent as JComponent).getClientProperty("com.apple.eawt.event.internalGestureHandler");
        //println("value $value");
        //GestureUtilities.addGestureListenerTo(p, ga);
    } catch (e: Throwable) {
        e.printStackTrace()
    }
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

fun Component.registerMouseEvents(gameWindow: GameWindow) {
    val contentComponent = this
    // Here all the robot events have been already processed, so they won't be processed
    //println("END ROBOT2")

    var lastMouseX: Int = 0
    var lastMouseY: Int = 0
    var lockingX: Int = 0
    var lockingY: Int = 0
    var locking = false
    var mouseX: Int = 0
    var mouseY: Int = 0

    gameWindow.events.requestLock = {
        val location = MouseInfo.getPointerInfo().location
        lockingX = location.x
        lockingY = location.y
        locking = true
    }

    fun handleMouseEvent(e: JMouseEvent) {
        val factor = getDisplayScalingFactor(contentComponent)

        val ev = when (e.id) {
            JMouseEvent.MOUSE_DRAGGED -> MouseEvent.Type.DRAG
            JMouseEvent.MOUSE_MOVED -> MouseEvent.Type.MOVE
            JMouseEvent.MOUSE_CLICKED -> MouseEvent.Type.CLICK
            JMouseEvent.MOUSE_PRESSED -> MouseEvent.Type.DOWN
            JMouseEvent.MOUSE_RELEASED -> MouseEvent.Type.UP
            JMouseEvent.MOUSE_ENTERED -> MouseEvent.Type.ENTER
            JMouseEvent.MOUSE_EXITED -> MouseEvent.Type.EXIT
            else -> MouseEvent.Type.MOVE
        }

        //println("MOUSE EVENT: $ev : ${e.button} : ${MouseButton[e.button - 1]}")
        gameWindow.queue {
            val button = if (e.button == 0) MouseButton.NONE else MouseButton[e.button - 1]

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
            //println("ev=$ev")
            gameWindow.dispatchMouseEventQueued(
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
                isShiftDown = modifiers hasFlags JMouseEvent.SHIFT_DOWN_MASK,
                isCtrlDown = modifiers hasFlags JMouseEvent.CTRL_DOWN_MASK,
                isAltDown = modifiers hasFlags JMouseEvent.ALT_DOWN_MASK,
                isMetaDown = modifiers hasFlags JMouseEvent.META_DOWN_MASK,
                scaleCoords = false,
                simulateClickOnUp = false
            )
        }
    }

    fun handleMouseWheelEvent(e: MouseWheelEvent) {
        val factor = getDisplayScalingFactor(contentComponent)

        gameWindow.queue {
            val ev = korlibs.event.MouseEvent.Type.SCROLL
            val button = MouseButton[8]
            val factor = factor
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

            val isShiftDown = modifiers hasFlags JMouseEvent.SHIFT_DOWN_MASK
            gameWindow.dispatchMouseEventQueued(
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
                isCtrlDown = modifiers hasFlags JMouseEvent.CTRL_DOWN_MASK,
                isAltDown = modifiers hasFlags JMouseEvent.ALT_DOWN_MASK,
                isMetaDown = modifiers hasFlags JMouseEvent.META_DOWN_MASK,
                scaleCoords = false,
                simulateClickOnUp = false
            )
        }
    }

    contentComponent.addMouseMotionListener(object : MouseMotionAdapter() {
        override fun mouseMoved(e: JMouseEvent) = handleMouseEvent(e)
        override fun mouseDragged(e: JMouseEvent) = handleMouseEvent(e)
    })

    contentComponent.addMouseListener(object : MouseAdapter() {
        override fun mouseReleased(e: JMouseEvent) = handleMouseEvent(e)
        override fun mouseMoved(e: JMouseEvent) = handleMouseEvent(e)
        override fun mouseEntered(e: JMouseEvent) = handleMouseEvent(e)
        override fun mouseDragged(e: JMouseEvent) = handleMouseEvent(e)
        override fun mouseClicked(e: JMouseEvent) = handleMouseEvent(e)
        override fun mouseExited(e: JMouseEvent) = handleMouseEvent(e)
        override fun mousePressed(e: JMouseEvent) = handleMouseEvent(e)
    })

    contentComponent.addMouseWheelListener { e -> handleMouseWheelEvent(e) }
}

fun Component.registerKeyEvents(gameWindow: GameWindow) {
    val component = this

    fun handleKeyEvent(e: JKeyEvent) {
        gameWindow.queue {
            val ev = when (e.id) {
                JKeyEvent.KEY_TYPED -> korlibs.event.KeyEvent.Type.TYPE
                JKeyEvent.KEY_PRESSED -> korlibs.event.KeyEvent.Type.DOWN
                JKeyEvent.KEY_RELEASED -> korlibs.event.KeyEvent.Type.UP
                else -> korlibs.event.KeyEvent.Type.TYPE
            }
            val id = 0
            val char = e.keyChar
            val keyCode = e.keyCode
            val key = awtKeyCodeToKey(e.keyCode)

            gameWindow.dispatchKeyEventExQueued(ev, id, char, key, keyCode, e.isShiftDown, e.isControlDown, e.isAltDown, e.isMetaDown)
        }
    }

    component.focusTraversalKeysEnabled = false
    component.addKeyListener(object : KeyAdapter() {
        override fun keyTyped(e: JKeyEvent) = handleKeyEvent(e)
        override fun keyPressed(e: JKeyEvent) = handleKeyEvent(e)
        override fun keyReleased(e: JKeyEvent) = handleKeyEvent(e)
    })
}

fun JFrame.computeDimensionsForSize(width: Int, height: Int): Dimension {
    val insets = this.insets
    return Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom)
}
