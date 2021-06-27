package com.soywiz.korgw

import com.soywiz.kds.iterators.*
import com.soywiz.klock.PerformanceCounter
import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korev.Touch
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.PNG
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.net.*
import com.soywiz.korio.util.*
import com.soywiz.krypto.encoding.*
import kotlinx.coroutines.*
import org.w3c.dom.events.*
import org.w3c.dom.events.MouseEvent
import kotlinx.browser.*
import org.w3c.dom.*
import org.w3c.dom.TouchEvent
import org.w3c.files.*

private external val navigator: dynamic

open class BrowserGameWindow : GameWindow() {
    override val ag: AGWebgl = AGWebgl(AGConfig())
    val canvas get() = ag.canvas

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

    @Suppress("UNUSED_PARAMETER")
    private fun updateGamepad() {
        try {
            if (navigator.getGamepads != null) {
                val gamepads = navigator.getGamepads().unsafeCast<JsArray<JsGamePad?>>()
                for (gp in gamePadUpdateEvent.gamepads) gp.connected = false
                gamePadUpdateEvent.gamepadsLength = gamepads.length
                for (gamepadId in 0 until gamepads.length) {
                    val controller = gamepads[gamepadId] ?: continue
                    val gamepad = gamePadUpdateEvent.gamepads.getOrNull(gamepadId) ?: continue
                    val mapping = knownControllers[controller.id] ?: knownControllers[controller.mapping] ?: StandardGamepadMapping
                    gamepad.apply {
                        this.connected = controller.connected
                        this.index = controller.index
                        this.name = controller.id
                        this.mapping = mapping
                        this.axesLength = controller.axes.length
                        this.buttonsLength = controller.buttons.length
                        this.rawButtonsPressed = 0
                        for (n in 0 until controller.buttons.length) {
                            val button = controller.buttons[n]
                            if (button.pressed) this.rawButtonsPressed = this.rawButtonsPressed or (1 shl n)
                            this.rawButtonsPressure[n] = button.value
                        }
                        for (n in 0 until controller.axes.length) {
                            this.rawAxes[n] = controller.axes[n]
                        }
                    }
                }
                dispatch(gamePadUpdateEvent)
            }
        } catch (e: dynamic) {
            console.error(e)
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
            val scale = quality.computeTargetScale(window.innerWidth, window.innerHeight, ag.devicePixelRatio)
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

    private fun doRender() {
        dispatch(renderEvent)
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
                        console.info("Unsupported key key=${me.key}, code=${me.code}")
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
    private fun touchEvent(e: TouchEvent, type: com.soywiz.korev.TouchEvent.Type) {
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

    private fun mouseEvent(e: MouseEvent, type: com.soywiz.korev.MouseEvent.Type, pressingType: com.soywiz.korev.MouseEvent.Type = type) {
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
            if (type == com.soywiz.korev.MouseEvent.Type.SCROLL) {
                val we = e.unsafeCast<WheelEvent>()

                this.scrollDeltaMode = when (we.deltaMode) {
                    WheelEvent.DOM_DELTA_PIXEL -> com.soywiz.korev.MouseEvent.ScrollDeltaMode.PIXEL
                    WheelEvent.DOM_DELTA_LINE -> com.soywiz.korev.MouseEvent.ScrollDeltaMode.LINE
                    WheelEvent.DOM_DELTA_PAGE -> com.soywiz.korev.MouseEvent.ScrollDeltaMode.PAGE
                    else -> com.soywiz.korev.MouseEvent.ScrollDeltaMode.LINE
                }

                this.scrollDeltaX = we.deltaX
                this.scrollDeltaY = we.deltaY
                this.scrollDeltaZ = we.deltaZ
            }
        }

        // If we are in a touch device, touch events will be dispatched, and then we don't want to emit mouse events, that would be duplicated
        if (!is_touch_device() || type == com.soywiz.korev.MouseEvent.Type.SCROLL) {
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
                link.href = "data:image/png;base64," + PNG.encode(value).toBase64()
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
        set(value) = run { canvas.style.visibility = if (value) "visible" else "hidden" }

    override fun setSize(width: Int, height: Int) {
        // Do nothing!
    }

    override suspend fun browse(url: URL) {
        document.open(url.fullUrl)
    }

    override suspend fun alert(message: String) {
        window.alert(message)
    }

    override suspend fun confirm(message: String): Boolean {
        return window.confirm(message)
    }

    override suspend fun prompt(message: String, default: String): String {
        return window.prompt(message, default) ?: throw CancellationException("cancelled")
    }

    override suspend fun openFileDialog(filter: FileFilter?, write: Boolean, multi: Boolean, currentDir: VfsFile?): List<VfsFile> {
        val deferred = CompletableDeferred<List<VfsFile>>()
        val input = document.createElement("input").unsafeCast<HTMLInputElement>()
        input.style.position = "absolute"
        input.style.top = "0px"
        input.style.left = "0px"
        input.style.visibility = "hidden"
        input.type = "file"
        input.multiple = multi
        input.onchange = {
            val files = input.files
            //document.body?.removeChild(input)
            if (files != null) {
                deferred.complete((0 until files.length).map { files[it]?.toVfs() }.filterNotNull())
            } else {
                deferred.complete(listOf())
            }
        }
        input.oncancel = {
            //document.body?.removeChild(input)
        }
        document.body?.appendChild(input)
        input.click()
        window.setTimeout({
            document.body?.removeChild(input)
        }, 100)
        return deferred.await()
    }

    private var loopJob: Job? = null

    override fun close() {
        super.close()
        launchImmediately(coroutineDispatcher) {
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

        canvas.addEventListener("wheel", { mouseEvent(it.unsafeCast<WheelEvent>(), com.soywiz.korev.MouseEvent.Type.SCROLL) })

        canvas.addEventListener("mouseenter", { mouseEvent(it.unsafeCast<MouseEvent>(), com.soywiz.korev.MouseEvent.Type.ENTER) })
        canvas.addEventListener("mouseleave", { mouseEvent(it.unsafeCast<MouseEvent>(), com.soywiz.korev.MouseEvent.Type.EXIT) })
        canvas.addEventListener("mouseover", { mouseEvent(it.unsafeCast<MouseEvent>(), com.soywiz.korev.MouseEvent.Type.MOVE, com.soywiz.korev.MouseEvent.Type.DRAG) })
        canvas.addEventListener("mousemove", { mouseEvent(it.unsafeCast<MouseEvent>(), com.soywiz.korev.MouseEvent.Type.MOVE, com.soywiz.korev.MouseEvent.Type.DRAG) })
        canvas.addEventListener("mouseout", { mouseEvent(it.unsafeCast<MouseEvent>(), com.soywiz.korev.MouseEvent.Type.EXIT) })
        canvas.addEventListener("mouseup", { mouseEvent(it.unsafeCast<MouseEvent>(), com.soywiz.korev.MouseEvent.Type.UP) })
        canvas.addEventListener("mousedown", { mouseEvent(it.unsafeCast<MouseEvent>(), com.soywiz.korev.MouseEvent.Type.DOWN) })
        canvas.addEventListener("click", { mouseEvent(it.unsafeCast<MouseEvent>(), com.soywiz.korev.MouseEvent.Type.CLICK) })

        canvas.addEventListener("touchstart", { touchEvent(it.unsafeCast<TouchEvent>(), com.soywiz.korev.TouchEvent.Type.START) })
        canvas.addEventListener("touchmove", { touchEvent(it.unsafeCast<TouchEvent>(), com.soywiz.korev.TouchEvent.Type.MOVE) })
        canvas.addEventListener("touchend", { touchEvent(it.unsafeCast<TouchEvent>(), com.soywiz.korev.TouchEvent.Type.END) })
        //canvas.addEventListener("touchcancel", { touchEvent(it, com.soywiz.korev.TouchEvent.Type.CANCEL) })

        window.addEventListener("keypress", { keyEvent(it.unsafeCast<KeyboardEvent>()) })
        window.addEventListener("keydown", { keyEvent(it.unsafeCast<KeyboardEvent>()) })
        window.addEventListener("keyup", { keyEvent(it.unsafeCast<KeyboardEvent>()) })

        window.addEventListener("gamepadconnected", { e ->
            //console.log("gamepadconnected")
            val e = e.unsafeCast<JsGamepadEvent>()
            dispatch(gamePadConnectionEvent.apply {
                this.type = GamePadConnectionEvent.Type.CONNECTED
                this.gamepad = e.gamepad.index
            })
        })
        window.addEventListener("gamepaddisconnected", { e ->
            //console.log("gamepaddisconnected")
            val e = e.unsafeCast<JsGamepadEvent>()
            dispatch(gamePadConnectionEvent.apply {
                this.type = GamePadConnectionEvent.Type.DISCONNECTED
                this.gamepad = e.gamepad.index
            })
        })
        window.addEventListener("resize", { onResized() })
        canvas.ondragenter = {
            dispatchDropfileEvent(DropFileEvent.Type.START, null)
        }
        canvas.ondragexit = {
            dispatchDropfileEvent(DropFileEvent.Type.END, null)
        }
        canvas.ondragleave = {
            dispatchDropfileEvent(DropFileEvent.Type.END, null)
        }
        canvas.ondragover = {
            it.preventDefault()
        }
        canvas.ondragstart = {
            dispatchDropfileEvent(DropFileEvent.Type.START, null)
        }
        canvas.ondragend = {
            dispatchDropfileEvent(DropFileEvent.Type.END, null)
        }
        canvas.ondrop = {
            it.preventDefault()
            dispatchDropfileEvent(DropFileEvent.Type.END, null)
            val items = it.dataTransfer!!.items
            val files = (0 until items.length).mapNotNull { items[it]?.getAsFile()?.toVfs() }
            dispatchDropfileEvent(DropFileEvent.Type.DROP, files)
        }
        onResized()

        jsFrame = { step: Double ->
            val startTime = PerformanceCounter.reference
            window.requestAnimationFrame(jsFrame) // Execute first to prevent exceptions breaking the loop
            updateGamepad()
            try {
                doRender()
            } finally {
                val elapsed = PerformanceCounter.reference - startTime
                val available = counterTimePerFrame - elapsed
                coroutineDispatcher.executePending(available)
            }
        }
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

class NodeJsGameWindow : GameWindow()

actual fun CreateDefaultGameWindow(): GameWindow = if (OS.isJsNodeJs) NodeJsGameWindow() else BrowserGameWindow()

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
object Nimbus_111_1420_Safari_GamepadMapping : GamepadMapping() {
    override val id = "111-1420-Nimbus"

    override fun get(button: GameButton, info: GamepadInfo): Double {
        return when (button) {
            GameButton.BUTTON0 -> info.getRawButton(0)
            GameButton.BUTTON1 -> info.getRawButton(1)
            GameButton.BUTTON2 -> info.getRawButton(2)
            GameButton.BUTTON3 -> info.getRawButton(3)
            GameButton.L1 -> info.getRawButton(4)
            GameButton.R1 -> info.getRawButton(5)
            GameButton.L2 -> info.getRawButton(6)
            GameButton.R2 -> info.getRawButton(7)
            GameButton.LEFT -> info.getRawButton(8)
            GameButton.DOWN -> info.getRawButton(9)
            GameButton.RIGHT -> info.getRawButton(10)
            GameButton.UP -> info.getRawButton(11)
            GameButton.SELECT -> 0.0
            GameButton.START -> 0.0
            GameButton.SYSTEM -> 0.0
            GameButton.LX -> info.getRawAxe(0)
            GameButton.LY -> info.getRawAxe(1)
            GameButton.RX -> info.getRawAxe(2)
            GameButton.RY -> info.getRawAxe(3)
            else -> 0.0
        }
    }
}

val knownControllers = listOf(
    StandardGamepadMapping,
    Nimbus_111_1420_Safari_GamepadMapping
).associateBy { it.id }
