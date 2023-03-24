package korlibs.render

import korlibs.memory.*
import korlibs.graphics.*
import korlibs.graphics.gl.*
import korlibs.event.*
import korlibs.event.Touch
import korlibs.image.bitmap.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.math.geom.*
import kotlinx.browser.*
import kotlinx.coroutines.*
import org.w3c.dom.*
import org.w3c.dom.TouchEvent
import org.w3c.dom.events.*
import org.w3c.dom.events.MouseEvent
import kotlin.coroutines.*

private external val navigator: dynamic

open class JsGameWindow : GameWindow() {
    override fun <T> runBlockingNoJs(coroutineContext: CoroutineContext, block: suspend () -> T): T {
        error("GameWindow.unsafeRunBlocking not implemented on JS")
    }
}

open class BrowserCanvasJsGameWindow(
    val canvas: HTMLCanvasElement = AGDefaultCanvas()
) : JsGameWindow() {
    val tDevicePixelRatio get() = window.devicePixelRatio.toDouble()
    override val devicePixelRatio get() = when {
        tDevicePixelRatio <= 0.0 -> 1.0
        tDevicePixelRatio.isNaN() -> 1.0
        tDevicePixelRatio.isInfinite() -> 1.0
        else -> tDevicePixelRatio
    }
    // @TODO: Improve this: https://gist.github.com/scryptonite/5242987
    //override val pixelsPerInch: Double get() = 96.0 * devicePixelRatio

    override val ag: AGOpengl = AGWebgl(AGConfig(), canvas)
    override val dialogInterface: DialogInterfaceJs = DialogInterfaceJs()
    private var isTouchDeviceCache: Boolean? = null
    fun is_touch_device(): Boolean {
        if (isTouchDeviceCache == null) {
            isTouchDeviceCache = try {
                document.createEvent("TouchEvent")
                true
            } catch (e: dynamic) {
                false
            }
        }
        return isTouchDeviceCache!!
    }

    // https://blog.teamtreehouse.com/wp-content/uploads/2014/03/standardgamepad.png
    val BUTTONS_MAPPING = arrayOf(
        GameButton.BUTTON_SOUTH, // 0
        GameButton.BUTTON_EAST, // 1
        GameButton.BUTTON_WEST, // 2
        GameButton.BUTTON_NORTH, // 3
        GameButton.L1,      // 4
        GameButton.R1,      // 5
        GameButton.L2,      // 6
        GameButton.R2,      // 7
        GameButton.SELECT,  // 8
        GameButton.START,   // 9
        GameButton.L3,      // 10
        GameButton.R3,      // 11
        GameButton.UP,      // 12
        GameButton.DOWN,    // 13
        GameButton.LEFT,    // 14
        GameButton.RIGHT,   // 15
        GameButton.SYSTEM,  // 16
    )

    val AXES_MAPPING = arrayOf(
        GameButton.LX, GameButton.LY,
        GameButton.RX, GameButton.RY,
        GameButton.L2, GameButton.R2,
        GameButton.DPADX, GameButton.DPADY,
    )

    private val gamepad = GamepadInfo()
    @Suppress("UNUSED_PARAMETER")
    override fun updateGamepads() {
        try {
            if (navigator.getGamepads != null) {
                val gamepads = navigator.getGamepads().unsafeCast<JsArray<JsGamePad?>>()
                dispatchGamepadUpdateStart()
                for (gamepadId in 0 until gamepads.length) {
                    val controller = gamepads[gamepadId] ?: continue
                    if (controller.mapping != "standard") continue
                    val gamepad = this@BrowserCanvasJsGameWindow.gamepad
                    gamepad.name = controller.id
                    for (n in 0 until kotlin.math.min(controller.buttons.length, BUTTONS_MAPPING.size)) {
                        gamepad.rawButtons[BUTTONS_MAPPING[n].index] = controller.buttons[n].value.toFloat()
                    }
                    for (n in 0 until kotlin.math.min(controller.axes.length, AXES_MAPPING.size)) {
                        val value = controller.axes[n].toFloat()
                        gamepad.rawButtons[AXES_MAPPING[n].index] = GamepadInfo.withoutDeadRange(value, apply = n <= 3)
                    }
                    dispatchGamepadUpdateAdd(gamepad)
                }
                dispatchGamepadUpdateEnd()
            }
        } catch (e: dynamic) {
            logger.error { e }
        }
    }

    override var quality: Quality = Quality.AUTOMATIC
        set(value) {
            if (field != value) {
                field = value
                onResized()
            }
        }

    @PublishedApi
    internal var canvasRatio = 1.0

    private fun onResized() {
        isTouchDeviceCache = null
        if (isCanvasCreatedAndHandled) {
            val scale = quality.computeTargetScale(window.innerWidth, window.innerHeight, devicePixelRatio)
            val canvasWidth = (window.innerWidth * scale).toInt()
            val canvasHeight = (window.innerHeight * scale).toInt()
            canvas.width = canvasWidth
            canvas.height = canvasHeight
            canvas.style.position = "absolute"
            canvas.style.left = "0"
            canvas.style.right = "0"
            canvas.style.width = "${window.innerWidth}px"
            canvas.style.height = "${window.innerHeight}px"
            canvasRatio = scale

            //ag.resized(canvas.width, canvas.height)
            //dispatchReshapeEvent(0, 0, window.innerWidth, window.innerHeight)
        } else {
            canvasRatio = (canvas.width.toDouble() / canvas.clientWidth.toDouble())
        }
        //canvasRatio = (canvas.width.toDouble() / canvas.clientWidth.toDouble())

        dispatchReshapeEvent(0, 0, canvas.width, canvas.height)
    }

    inline fun transformEventX(x: Double): Double = x * canvasRatio
    inline fun transformEventY(y: Double): Double = y * canvasRatio

    private fun keyEvent(me: KeyboardEvent) {
        val key = when (me.key) {
            "0" -> Key.N0; "1" -> Key.N1; "2" -> Key.N2; "3" -> Key.N3
            "4" -> Key.N4; "5" -> Key.N5; "6" -> Key.N6; "7" -> Key.N7
            "8" -> Key.N8; "9" -> Key.N9
            "a" -> Key.A; "b" -> Key.B; "c" -> Key.C; "d" -> Key.D
            "e" -> Key.E; "f" -> Key.F; "g" -> Key.G; "h" -> Key.H
            "i" -> Key.I; "j" -> Key.J; "k" -> Key.K; "l" -> Key.L
            "m" -> Key.M; "n" -> Key.N; "o" -> Key.O; "p" -> Key.P
            "q" -> Key.Q; "r" -> Key.R; "s" -> Key.S; "t" -> Key.T
            "u" -> Key.U; "v" -> Key.V; "w" -> Key.W; "x" -> Key.X
            "y" -> Key.Y; "z" -> Key.Z
            "F1" -> Key.F1; "F2" -> Key.F2; "F3" -> Key.F3; "F4" -> Key.F4
            "F5" -> Key.F5; "F6" -> Key.F6; "F7" -> Key.F7; "F8" -> Key.F8
            "F9" -> Key.F9; "F10" -> Key.F10; "F11" -> Key.F11; "F12" -> Key.F12
            "F13" -> Key.F13; "F14" -> Key.F14; "F15" -> Key.F15; "F16" -> Key.F16
            "F17" -> Key.F17; "F18" -> Key.F18; "F19" -> Key.F19; "F20" -> Key.F20
            "F21" -> Key.F21; "F22" -> Key.F22; "F23" -> Key.F23; "F24" -> Key.F24
            "F25" -> Key.F25
            "+" -> Key.PLUS
            "-" -> Key.MINUS
            "'" -> Key.APOSTROPHE
            "\"" -> Key.QUOTE
            else -> when (me.code) {
                "MetaLeft" -> Key.LEFT_SUPER
                "MetaRight" -> Key.RIGHT_SUPER
                "ShiftLeft" -> Key.LEFT_SHIFT
                "ShiftRight" -> Key.RIGHT_SHIFT
                "ControlLeft" -> Key.LEFT_CONTROL
                "ControlRight" -> Key.RIGHT_CONTROL
                "AltLeft" -> Key.LEFT_ALT
                "AltRight" -> Key.RIGHT_ALT
                "Space" -> Key.SPACE
                "ArrowUp" -> Key.UP
                "ArrowDown" -> Key.DOWN
                "ArrowLeft" -> Key.LEFT
                "ArrowRight" -> Key.RIGHT
                "PageUp" -> Key.PAGE_UP
                "PageDown" -> Key.PAGE_DOWN
                "Home" -> Key.HOME
                "End" -> Key.END
                "Enter" -> Key.ENTER
                "Escape" -> Key.ESCAPE
                "Backspace" -> Key.BACKSPACE
                "Delete" -> Key.DELETE
                "Insert" -> Key.INSERT
                "Period" -> Key.PERIOD
                "Comma" -> Key.COMMA
                "Semicolon" -> Key.SEMICOLON
                "Slash" -> Key.SLASH
                "Tab" -> Key.TAB
                else -> {
                    if (window.asDynamic().korgwShowUnsupportedKeys) {
                        logger.info { "Unsupported key key=${me.key}, code=${me.code}" }
                    }
                    Key.UNKNOWN
                }
            }
        }
        dispatch(keyEvent {
            this.type = when (me.type) {
                "keydown" -> KeyEvent.Type.DOWN
                "keyup" -> KeyEvent.Type.UP
                "keypress" -> KeyEvent.Type.TYPE
                else -> error("Unsupported event type ${me.type}")
            }
            this.id = 0
            this.keyCode = me.keyCode
            this.key = key
            this.shift = me.shiftKey
            this.ctrl = me.ctrlKey
            this.alt = me.altKey
            this.meta = me.metaKey
            this.character = me.charCode.toChar()
        })

        // @TODO: preventDefault on all causes keypress to not happen?
        if (key == Key.TAB || key.isFunctionKey) {
            me.preventDefault()
        }
    }

    // JS TouchEvent contains only active touches (ie. touchend just return the list of non ended-touches)
    private fun touchEvent(e: TouchEvent, type: korlibs.event.TouchEvent.Type) {
        val canvasBounds = canvas.getBoundingClientRect()
        dispatch(touchBuilder.frame(TouchBuilder.Mode.JS, type) {
            for (n in 0 until e.touches.length) {
                val touch = e.touches.item(n) ?: continue
                val touchId = touch.identifier
                touch(
                    id = touchId,
                    x = transformEventX(touch.clientX.toDouble() - canvasBounds.left),
                    y = transformEventY(touch.clientY.toDouble() - canvasBounds.top),
                    force = touch.asDynamic().force.unsafeCast<Double?>() ?: 1.0,
                    kind = Touch.Kind.FINGER
                )
            }
        }.also {
            //println("touchEvent=$it")
        })
    }

    private fun mouseEvent(e: MouseEvent, type: korlibs.event.MouseEvent.Type, pressingType: korlibs.event.MouseEvent.Type = type) {
        val canvasBounds = canvas.getBoundingClientRect()

        val tx = transformEventX(e.clientX.toDouble() - canvasBounds.left).toInt()
        val ty = transformEventY(e.clientY.toDouble() - canvasBounds.top).toInt()
        //console.log("mouseEvent", type.toString(), e.clientX, e.clientY, tx, ty)
        mouseEvent {
            this.type = if (e.buttons.toInt() != 0) pressingType else type
            this.scaleCoords = false
            this.id = 0
            this.x = tx
            this.y = ty
            this.button = MouseButton[e.button.toInt()]
            this.buttons = e.buttons.toInt()
            this.isShiftDown = e.shiftKey
            this.isCtrlDown = e.ctrlKey
            this.isAltDown = e.altKey
            this.isMetaDown = e.metaKey
            if (type == korlibs.event.MouseEvent.Type.SCROLL) {
                val we = e.unsafeCast<WheelEvent>()
                val mode = when (we.deltaMode) {
                    WheelEvent.DOM_DELTA_PIXEL -> korlibs.event.MouseEvent.ScrollDeltaMode.PIXEL
                    WheelEvent.DOM_DELTA_LINE -> korlibs.event.MouseEvent.ScrollDeltaMode.LINE
                    WheelEvent.DOM_DELTA_PAGE -> korlibs.event.MouseEvent.ScrollDeltaMode.PAGE
                    else -> korlibs.event.MouseEvent.ScrollDeltaMode.LINE
                }

                //println("scrollDeltaMode: ${we.deltaMode}: $mode, ${we.deltaX}, ${we.deltaY}, ${we.deltaZ}")

                val sensitivity = 0.05
                //val sensitivity = 0.1

                this.setScrollDelta(
                    mode,
                    x = we.deltaX * sensitivity,
                    y = we.deltaY * sensitivity,
                    z = we.deltaZ * sensitivity,
                )
            }
        }

        // If we are in a touch device, touch events will be dispatched, and then we don't want to emit mouse events, that would be duplicated
        if (!is_touch_device() || type == korlibs.event.MouseEvent.Type.SCROLL) {
            dispatch(mouseEvent)
        }
    }

    override var title: String
        get() = document.title
        set(value) { document.title = value }
    override val width: Int get() = canvas.clientWidth
    override val height: Int get() = canvas.clientHeight
    override val bufferWidth: Int get() = canvas.width
    override val bufferHeight: Int get() = canvas.height

    override var cursor: ICursor = Cursor.DEFAULT
        set(value) {
            field = value
            canvas.style.cursor = when (value) {
                is Cursor -> {
                    when (value) {
                        Cursor.DEFAULT -> "default"
                        Cursor.CROSSHAIR -> "crosshair"
                        Cursor.TEXT -> "text"
                        Cursor.HAND -> "pointer"
                        Cursor.MOVE -> "move"
                        Cursor.WAIT -> "wait"
                        Cursor.RESIZE_EAST -> "e-resize"
                        Cursor.RESIZE_WEST -> "w-resize"
                        Cursor.RESIZE_SOUTH -> "s-resize"
                        Cursor.RESIZE_NORTH -> "n-resize"
                        Cursor.RESIZE_NORTH_EAST -> "ne-resize"
                        Cursor.RESIZE_NORTH_WEST -> "nw-resize"
                        Cursor.RESIZE_SOUTH_EAST -> "se-resize"
                        Cursor.RESIZE_SOUTH_WEST -> "sw-resize"
                        //Cursor.ZOOM_IN -> "zoom-in"
                        //Cursor.ZOOM_OUT -> "zoom-out"
                        else -> "default"
                    }
                }
                else -> "default"
            }
        }

    override var icon: Bitmap? = null
        set(value) {
            field = value
            if (value != null) {
                val link: HTMLLinkElement = document.querySelector("link[rel*='icon']").unsafeCast<HTMLLinkElement>()
                link.type = "image/png"
                link.rel = "shortcut icon"
                //link.href = "data:image/png;base64," + PNG.encode(value).toBase64()
                link.href = value.toHtmlNative().toDataURL()
                document.getElementsByTagName("head")[0]?.appendChild(link)
            } else {
                document.querySelector("link[rel*='icon']")?.remove()
            }
        }
    override var fullscreen: Boolean
        get() = document.fullscreenElement != null
        set(value) {
            if (fullscreen != value) {
                kotlin.runCatching {
                    if (value) {
                        canvas.requestFullscreen()
                    } else {
                        document.exitFullscreen()
                    }
                }
            }
        }
    override var visible: Boolean
        get() = canvas.style.visibility == "visible"
        set(value) { canvas.style.visibility = if (value) "visible" else "hidden" }

    override fun setSize(width: Int, height: Int) {
        // Do nothing!
    }

    internal var loopJob: Job? = null

    override fun close(exitCode: Int) {
        MainScope().launchImmediately {
            loopJob?.cancelAndJoin()
            window.close()
        }
        loopJob = null
    }

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        loopJob = launchImmediately(getCoroutineDispatcherWithCurrentContext()) {
            entry()
        }
        jsFrame(0.0)
    }

    private lateinit var jsFrame: (Double) -> Unit

    init {
        window.asDynamic().canvas = canvas
        window.asDynamic().ag = ag
        window.asDynamic().gl = ag.gl
        if (isCanvasCreatedAndHandled) {
            document.body?.appendChild(canvas)
            document.body?.style?.margin = "0px"
            document.body?.style?.padding = "0px"
            document.body?.style?.overflowX = "hidden"
            document.body?.style?.overflowY = "hidden"
        }

        canvas.addEventListener("wheel", { mouseEvent(it.unsafeCast<WheelEvent>(), korlibs.event.MouseEvent.Type.SCROLL) })

        canvas.addEventListener("mouseenter", { mouseEvent(it.unsafeCast<MouseEvent>(), korlibs.event.MouseEvent.Type.ENTER) })
        canvas.addEventListener("mouseleave", { mouseEvent(it.unsafeCast<MouseEvent>(), korlibs.event.MouseEvent.Type.EXIT) })
        canvas.addEventListener("mouseover", { mouseEvent(it.unsafeCast<MouseEvent>(), korlibs.event.MouseEvent.Type.MOVE, korlibs.event.MouseEvent.Type.DRAG) })
        canvas.addEventListener("mousemove", { mouseEvent(it.unsafeCast<MouseEvent>(), korlibs.event.MouseEvent.Type.MOVE, korlibs.event.MouseEvent.Type.DRAG) })
        canvas.addEventListener("mouseout", { mouseEvent(it.unsafeCast<MouseEvent>(), korlibs.event.MouseEvent.Type.EXIT) })
        canvas.addEventListener("mouseup", { mouseEvent(it.unsafeCast<MouseEvent>(), korlibs.event.MouseEvent.Type.UP) })
        canvas.addEventListener("mousedown", { mouseEvent(it.unsafeCast<MouseEvent>(), korlibs.event.MouseEvent.Type.DOWN) })
        canvas.addEventListener("click", { mouseEvent(it.unsafeCast<MouseEvent>(), korlibs.event.MouseEvent.Type.CLICK) })

        canvas.addEventListener("touchstart", { touchEvent(it.unsafeCast<TouchEvent>(), korlibs.event.TouchEvent.Type.START) })
        canvas.addEventListener("touchmove", { touchEvent(it.unsafeCast<TouchEvent>(), korlibs.event.TouchEvent.Type.MOVE) })
        canvas.addEventListener("touchend", { touchEvent(it.unsafeCast<TouchEvent>(), korlibs.event.TouchEvent.Type.END) })
        //canvas.addEventListener("touchcancel", { touchEvent(it, korlibs.event.TouchEvent.Type.CANCEL) })

        window.addEventListener("keypress", { keyEvent(it.unsafeCast<KeyboardEvent>()) })
        window.addEventListener("keydown", { keyEvent(it.unsafeCast<KeyboardEvent>()) })
        window.addEventListener("keyup", { keyEvent(it.unsafeCast<KeyboardEvent>()) })

        //window.addEventListener("gamepadconnected", { e ->
        //    //console.log("gamepadconnected")
        //    val e = e.unsafeCast<JsGamepadEvent>()
        //    dispatch(gamePadConnectionEvent.apply {
        //        this.type = GamePadConnectionEvent.Type.CONNECTED
        //        this.gamepad = e.gamepad.index
        //    })
        //})
        //window.addEventListener("gamepaddisconnected", { e ->
        //    //console.log("gamepaddisconnected")
        //    val e = e.unsafeCast<JsGamepadEvent>()
        //    dispatch(gamePadConnectionEvent.apply {
        //        this.type = GamePadConnectionEvent.Type.DISCONNECTED
        //        this.gamepad = e.gamepad.index
        //    })
        //})
        window.addEventListener("resize", { onResized() })
        canvas.ondragenter = { dispatchDropfileEvent(DropFileEvent.Type.START, null) }
        canvas.ondragexit = { dispatchDropfileEvent(DropFileEvent.Type.END, null) }
        canvas.ondragleave = { dispatchDropfileEvent(DropFileEvent.Type.END, null) }
        canvas.ondragover = { it.preventDefault() }
        canvas.ondragstart = { dispatchDropfileEvent(DropFileEvent.Type.START, null) }
        canvas.ondragend = { dispatchDropfileEvent(DropFileEvent.Type.END, null) }
        canvas.ondrop = {
            it.preventDefault()
            dispatchDropfileEvent(DropFileEvent.Type.END, null)
            val items = it.dataTransfer!!.items
            val files = (0 until items.length).mapNotNull { items[it]?.getAsFile()?.toVfs() }
            dispatchDropfileEvent(DropFileEvent.Type.DROP, files)
        }
        onResized()

        jsFrame = { step: Double ->
            window.requestAnimationFrame(jsFrame) // Execute first to prevent exceptions breaking the loop, not triggering again
            frame()
        }
    }


    override val isSoftKeyboardVisible: Boolean
        get() = super.isSoftKeyboardVisible

    private var softKeyboardInput: HTMLInputElement? = null
    private fun ensureSoftKeyboardInput() {
        if (softKeyboardInput == null) {
            softKeyboardInput = document.createElement("input").unsafeCast<HTMLInputElement>()
            softKeyboardInput?.id = "softKeyboardInput"
            softKeyboardInput?.type = "input"
            softKeyboardInput?.style?.let { style ->
                style.zIndex = "10000000"
                style.position = "absolute"
                style.top = "0"
                style.left = "0"
                style.width = "200px"
                style.height = "24px"
                style.background = "transparent"
                //style.visibility = "hidden"
            }

            //val softKeyboardInput2 = document.createElement("input").unsafeCast<HTMLInputElement>()
            //softKeyboardInput2?.id = "softKeyboardInput"
            //softKeyboardInput2?.type = "input"
            //softKeyboardInput2?.style?.zIndex = "10000000"
            //softKeyboardInput2?.style?.position = "absolute"
            //softKeyboardInput2?.style?.top = "0"
            //softKeyboardInput2?.style?.left = "24px"
            //softKeyboardInput2?.style?.width = "200px"
            //softKeyboardInput2?.style?.height = "24px"
            //softKeyboardInput2?.style?.background = "white"
            //document.body?.appendChild(softKeyboardInput2!!)
            //enterDebugger()
        }
    }

    override fun setInputRectangle(windowRect: Rectangle) {
        ensureSoftKeyboardInput()
        softKeyboardInput?.style?.let { style ->
            style.left = "${(windowRect.left / canvasRatio)}px"
            style.top = "${(windowRect.top / canvasRatio) - 16}px"
            style.width = "${(windowRect.width / canvasRatio)}px"
            style.font = "32px Arial"
            //style.height = "${windowRect.height / canvasRatio}px"
            style.height = "1px"
            style.opacity = "0"
            style.background = "transparent"
            style.color = "transparent"
            //style.visibility = "hidden"
            println("BOUNDS.setInputRectangle:${style.left},${style.top},${style.width},${style.height}")
        }
    }

    override fun showSoftKeyboard(force: Boolean, config: ISoftKeyboardConfig?) {
        document.body?.appendChild(softKeyboardInput!!)
        softKeyboardInput?.focus()
    }

    override fun hideSoftKeyboard() {
        softKeyboardInput?.blur()
        document.body?.removeChild(softKeyboardInput!!)
        //canvas.focus()
    }
}

private external interface JsArray<T> {
    val length: Int
}

private inline operator fun <T> JsArray<T>.get(index: Int): T = this.asDynamic()[index]

private external interface JsGamepadButton {
    val value: Double
    val pressed: Boolean
}

private external interface JsGamePad {
    val axes: JsArray<Double>
    val buttons: JsArray<JsGamepadButton>
    val connected: Boolean
    val id: String
    val index: Int
    val mapping: String
    val timestamp: Double
}

@JsName("GamepadEvent")
private external interface JsGamepadEvent {
    val gamepad: JsGamePad
}

class NodeJsGameWindow : JsGameWindow()

actual fun CreateDefaultGameWindow(config: GameWindowCreationConfig): GameWindow = if (Platform.isJsNodeJs) NodeJsGameWindow() else BrowserCanvasJsGameWindow()

/*
public external open class TouchEvent(type: String, eventInitDict: MouseEventInit = definedExternally) : UIEvent {
    open val shiftKey: Boolean
    open val altKey: Boolean
    open val ctrlKey: Boolean
    open val metaKey: Boolean

    open val changedTouches: TouchList
    open val touches: TouchList
    open val targetTouches: TouchList
}

external class TouchList {
    val length: Int
    fun item(index: Int): Touch
}

external class Touch {
    val identifier: Int
    val screenX: Int
    val screenY: Int
    val clientX: Int
    val clientY: Int
    val pageX: Int
    val pageY: Int
    val target: dynamic
}
*/
