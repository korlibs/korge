package com.soywiz.korui.light

import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.log.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.files.*
import kotlin.RuntimeException
import kotlin.coroutines.*
import kotlin.reflect.*

var windowInputFile: HTMLInputElement? = null
var selectedFiles = arrayOf<File>()
var mainFrame: HTMLElement? = null

@Suppress("unused")
class HtmlLightComponents : LightComponents() {
    val window: Window? = if (OS.isJsNodeJs) null else kotlin.js.js("self")
    val document = window?.document

	val tDevicePixelRatio = window?.devicePixelRatio?.toDouble() ?: 1.0
	val devicePixelRatio = when {
		tDevicePixelRatio <= 0.0 -> 1.0
		tDevicePixelRatio.isNaN() -> 1.0
		tDevicePixelRatio.isInfinite() -> 1.0
		else -> tDevicePixelRatio
	}

	val windowWidth: Int get() = document?.documentElement?.clientWidth ?: window?.innerWidth ?: 128
	val windowHeight: Int get() = document?.documentElement?.clientHeight ?: window?.innerHeight ?: 128

	override val xScale: Double get() = if (quality == LightQuality.QUALITY) devicePixelRatio else 1.0
    override val yScale: Double get() = if (quality == LightQuality.QUALITY) devicePixelRatio else 1.0

	//val xEventScale: Double get() = 1.0 / xScale
	//val yEventScale: Double get() = 1.0 / yScale

    override val xEventScale: Double get() = xScale
    override val yEventScale: Double get() = yScale

	//val xEventScale: Double get() = 1.0
	//val yEventScale: Double get() = 1.0

    object Dummy

    init {
		addStyles(
			"""
			body {
				font: 11pt Arial;
			}
			.BUTTON {
				-moz-box-shadow:inset 0px 1px 0px 0px #ffffff;
				-webkit-box-shadow:inset 0px 1px 0px 0px #ffffff;
				box-shadow:inset 0px 1px 0px 0px #ffffff;
				background:linear-gradient(to bottom, #ffffff 5%, #f6f6f6 100%);
				background-color:#ffffff;
				-moz-border-radius:6px;
				-webkit-border-radius:6px;
				border-radius:6px;
				border:1px solid #dcdcdc;
				display:inline-block;
				cursor:pointer;
				color:#666666;
				font-family:Arial;
				font-size:15px;
				font-weight:bold;
				padding:6px 24px;
				text-decoration:none;
				text-shadow:0px 1px 0px #ffffff;
			}
			.BUTTON:hover {
				background:linear-gradient(to bottom, #f6f6f6 5%, #ffffff 100%);
				background-color:#f6f6f6;
			}
			.BUTTON:active {
				padding-top: 7px;
				padding-bottom: 5px;

				background:linear-gradient(to bottom, #f0f0f0 5%, #f6f6f6 100%);
				background-color:#f6f6f6;
			}
			.BUTTON:focus {
				/*outline: auto 5px -webkit-focus-ring-color;*/
				outline: auto 1px black;
			}
			.TEXT_AREA {
				white-space: nowrap;
				resize: none;
			}
		"""
		)

        if (document != null) {
            document.body?.style?.background = "#f0f0f0"
            val inputFile = document.createElement("input").unsafeCast<HTMLInputElement>()
            inputFile.type = "file"
            inputFile.style.visibility = "hidden"
            windowInputFile = inputFile
            selectedFiles = arrayOf()
            document.body?.appendChild(inputFile)
        }
	}

	fun addStyles(css: String) {
        val document = this.document ?: return
		val head: HTMLHeadElement = document.head ?: document.getElementsByTagName("head")[0].unsafeCast<HTMLHeadElement>()
		val style = document.createElement("style").unsafeCast<HTMLStyleElement>()

        style.type = "text/css"
        if (style.asDynamic().styleSheet != null) {
            style.asDynamic().styleSheet.cssText = css
        } else {
            style.appendChild(document.createTextNode(css))
        }

        head.appendChild(style)
	}

	override fun create(type: LightType, config: Any?): LightComponentInfo {
        val document = this.document ?: return LightComponentInfo(Dummy).apply {
            if (type == LightType.AGCANVAS) {
                ag = DummyAG(640, 480)
            }
        }
        var agg: AG? = null
		val handle: HTMLElement = when (type) {
			LightType.FRAME -> {
				(document.createElement("article").unsafeCast<HTMLElement>()).apply {
					this.className = "FRAME"
					document.body?.appendChild(this)
					mainFrame = this
					mainFrame?.style?.visibility = "hidden"
				}
			}
			LightType.CONTAINER -> {
				(document.createElement("div").unsafeCast<HTMLElement>()).apply {
					this.className = "CONTAINER"
				}
			}
			LightType.SCROLL_PANE -> {
				(document.createElement("div").unsafeCast<HTMLElement>()).apply {
					this.className = "SCROLL_PANE"
				}
			}
			LightType.BUTTON -> {
				(document.createElement("input").unsafeCast<HTMLInputElement>()).apply {
					this.className = "BUTTON"
					this.type = "button"
				}
			}
			LightType.PROGRESS -> {
				(document.createElement("progress").unsafeCast<HTMLElement>()).apply {
					this.className = "PROGRESS"
				}
			}
			LightType.IMAGE -> {
				(document.createElement("canvas").unsafeCast<HTMLCanvasElement>())!!.apply {
					this.className = "IMAGE"
					this.style.imageRendering = "pixelated"
				}
			}
			LightType.LABEL -> {
				(document.createElement("label").unsafeCast<HTMLElement>()).apply {
					this.className = "LABEL"
				}
			}
			LightType.TEXT_FIELD -> {
				(document.createElement("input").unsafeCast<HTMLInputElement>())!!.apply {
					this.className = "TEXT_FIELD"
					this.type = "text"
				}
			}
			LightType.TEXT_AREA -> {
				(document.createElement("textarea").unsafeCast<HTMLElement>()).apply {
					this.className = "TEXT_AREA"
					//this["type"] = "text"
				}
			}
			LightType.CHECK_BOX -> {
				(document.createElement("label").unsafeCast<HTMLElement>()).apply {
					this.className = "CHECK_BOX"
					this.asDynamic()["data-type"] = "checkbox"
					val input: HTMLInputElement = document.createElement("input").unsafeCast<HTMLInputElement>()
					input.apply {
						this.className = "TEXT_FIELD"
						this.type = "checkbox"
					}
					this.appendChild(input)
					this.appendChild(document.createElement("span")!!)
				}
			}
			LightType.AGCANVAS -> {
				agg = AGOpenglFactory.create(null).create(null, (config as? AGConfig?) ?: AGConfig())
				val cc = agg.nativeComponent.unsafeCast<HTMLCanvasElement>()
				cc.tabIndex = 1
				cc.style.outline = "none"
				cc
			}
			else -> {
				(document.createElement("div").unsafeCast<HTMLElement>()).apply {
					this.className = "UNKNOWN"
				}
			}
		}

		handle.apply {
			val style = this.style
			style.position = "absolute"

			val overflow = when (type) {
				LightType.SCROLL_PANE, LightType.TEXT_AREA, LightType.TEXT_FIELD -> true
				else -> false
			}

			style.overflowY = if (overflow) "auto" else "hidden"
			style.overflowX = if (overflow) "auto" else "hidden"
			style.left = "0px"
			style.top = "0px"
			style.width = "100px"
			style.height = "100px"
		}

		return LightComponentInfo(handle).apply {
			if (agg != null) this.ag = agg
		}
	}

	override fun setParent(c: Any, parent: Any?) {
        if (c.isInvalid()) return
		val child = c.unsafeCast<HTMLElement>()
		child.parentNode?.removeChild(child)
		if (parent != null) {
			(parent.unsafeCast<HTMLElement>()).appendChild(child)
		}
	}

	private fun EventTarget.addCloseableEventListener(name: String, func: (Event) -> Unit): Closeable {
		this.addEventListener(name, func)
		return Closeable { this.removeEventListener(name, func) }
	}

	override fun <T : com.soywiz.korev.Event> registerEventKind(
		c: Any, clazz: KClass<T>, ed: EventDispatcher
	): Closeable {
        if (c.isInvalid()) return DummyCloseable
        val document = this.document ?: return DummyCloseable
        val window = this.window ?: return DummyCloseable

		val node = c.unsafeCast<HTMLElement>()

		when (clazz) {
			com.soywiz.korev.MouseEvent::class -> {
				val event = com.soywiz.korev.MouseEvent()

				fun dispatchMouseEvent(e: Event) {
					val me = e.unsafeCast<MouseEvent>()
					//console.error("MOUSE EVENT!")
					//console.error(me)
					ed.dispatch(event.apply {
						this.id = 0
						this.x = (me.offsetX * xEventScale).toInt()
						this.y = (me.offsetY * yEventScale).toInt()
						this.button = MouseButton[me.button.toInt()]
						this.buttons = me.buttons.toInt()
						this.isAltDown = me.altKey
						this.isCtrlDown = me.ctrlKey
						this.isShiftDown = me.shiftKey
						this.isMetaDown = me.metaKey
						this.scaleCoords = false
						this.type = when (me.type) {
							"click" -> com.soywiz.korev.MouseEvent.Type.CLICK
							"mousemove" -> {
								if (me.button.toInt() == 0) {
									com.soywiz.korev.MouseEvent.Type.MOVE
								} else {
									com.soywiz.korev.MouseEvent.Type.DRAG
								}
							}
							"mouseup" -> com.soywiz.korev.MouseEvent.Type.UP
							"mousedown" -> com.soywiz.korev.MouseEvent.Type.DOWN
							"mouseenter" -> com.soywiz.korev.MouseEvent.Type.DOWN
							"mouseover" -> com.soywiz.korev.MouseEvent.Type.ENTER
							"mouseout" -> com.soywiz.korev.MouseEvent.Type.EXIT
							else -> error("Unsupported event type ${me.type}")
						}
					})
				}

				return listOf("click", "mouseover", "mousemove", "mouseup", "mousedown")
					.map { node.addCloseableEventListener(it) { dispatchMouseEvent(it) } }
					.closeable()
			}
			KeyEvent::class -> {
				val event = KeyEvent()

				fun dispatchMouseEvent(e: Event) {
					val me = e.unsafeCast<KeyboardEvent>()
					//console.error("MOUSE EVENT!")
					//console.error(me)
					ed.dispatch(event.apply {
						this.id = 0
						this.keyCode = me.keyCode
                        // https://keycode.info/
						this.key = when (me.key) {
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
								"Enter" -> Key.ENTER
								"Escape" -> Key.ESCAPE
								"Backspace" -> Key.BACKSPACE
								"Period" -> Key.PERIOD
								"Comma" -> Key.COMMA
								"Semicolon" -> Key.SEMICOLON
								"Slash" -> Key.SLASH
								"Tab" -> Key.TAB
								else -> Key.UNKNOWN
							}
						}
						this.character = me.charCode.toChar()
						this.type = when (me.type) {
							"keydown" -> KeyEvent.Type.DOWN
							"keyup" -> KeyEvent.Type.UP
							"keypress" -> KeyEvent.Type.TYPE
							else -> error("Unsupported event type ${me.type}")
						}
					})
				}

				return listOf("keydown", "keyup", "keypress")
					.map { node.addCloseableEventListener(it) { dispatchMouseEvent(it) } }
					.closeable()
			}
			ChangeEvent::class -> {
				val event = ChangeEvent()

				fun dispatchChangeEvent(e: Event) {
					ed.dispatch(event.apply {
						this.oldValue = null
						this.newValue = null
					})
				}

				listOf("change", "keypress", "input", "textInput", "paste")
					.map { node.addCloseableEventListener(it) { dispatchChangeEvent(it) } }
					.closeable().cancellable()
			}
			ReshapeEvent::class -> {
				val node = window
				val info = ReshapeEvent()

				var lastWidth = -1
				var lastHeight = -1
				var closed = false

				fun update() {
					if (lastWidth != windowWidth || lastHeight != windowHeight) {
						lastWidth = windowWidth
						lastHeight = windowHeight

						if (mainFrame != null) {
							mainFrame?.style?.width = "${lastWidth}px"
							mainFrame?.style?.height = "${lastHeight}px"
						}

						ed.dispatch(info.apply {
							width = lastWidth
							height = lastHeight
						})
					}
				}

				fun timer() {
					update()
					if (!closed) {
						window.setTimeout(::timer, 100)
					}
				}

				timer()

				return listOf(
					node.addCloseableEventListener("resize") { update() },
					node.addCloseableEventListener("deviceorientation") { update() },
					object : Closeable {
						override fun close() {
							closed = true
						}
					}
				).closeable()
			}
			GamePadConnectionEvent::class -> {
				val info = GamePadConnectionEvent()

				val rnode: HTMLElement = if (node.tagName.toUpperCase() == "CANVAS") window.asDynamic() else node

				fun process(connected: Boolean, e: Event) {
					ed.dispatch(info.apply {
						this.type = when {
							connected -> GamePadConnectionEvent.Type.CONNECTED
							else -> GamePadConnectionEvent.Type.DISCONNECTED
						}
						this.gamepad = e.asDynamic().gamepad.index
					})
				}

				return listOf(
					rnode.addCloseableEventListener("gamepadconnected") { process(true, it) },
					rnode.addCloseableEventListener("gamepaddisconnected") { process(false, it) }
				).closeable()
			}
			//com.soywiz.korui.event.GamePadButtonEvent::class -> {
			//	val info = GamePadButtonEvent()

			//	@Suppress("UNUSED_PARAMETER")
			//	fun frame(e: Double) {
			//		window.requestAnimationFrame(::frame)
			//		if (navigator.getGamepads != null) {
			//			val gamepads = navigator.getGamepads().unsafeCast<Array<dynamic>>()
			//			for (gamepadId in 0 until gamepads.asDynamic().length.unsafeCast<Int>()) {
			//				val controller = gamepads[gamepadId] ?: continue
			//				val buttonsArray = controller.buttons
			//				val axesArray = controller.axes
			//				val controllerName = controller.id.unsafeCast<String?>() ?: "unknown"
			//				val controllerIndex = controller.index.unsafeCast<Int>()
			//				var buttons = 0
			//				val igamepad = info
			//				for (i in 0 until buttonsArray.length.unsafeCast<Int>()) {
			//					if (buttonsArray[i].pressed.unsafeCast<Boolean>()) buttons = buttons or (1 shl i)
			//				}
			//				for (i in 0 until min(igamepad.axes.size, axesArray.length.unsafeCast<Int>())) {
			//					igamepad.axes[i] = axesArray[i].unsafeCast<Double>()
			//				}
			//				igamepad.connected = true
			//				igamepad.index = controllerIndex
			//				igamepad.name = controllerName
			//				igamepad.mapping = knownControllers[controllerName] ?: StandardGamepadMapping
			//				igamepad.buttons = buttons
			//				ed.update(info)
			//			}
			//		}
			//	}
			//	frame(0.0)

			//	window.addEventListener("gamepadconnected", { e ->
			//		info.gamepad.connected = true
			//		listener.connection(info)
			//	})
			//	window.addEventListener("gamepaddisconnected", { e ->
			//		info.gamepad.connected = false
			//		listener.connection(info)
			//	})

			//	return super.addHandler(c, listener)
			//
			TouchEvent::class -> {
				fun process(type: TouchEvent.Type, e: Event, preventDefault: Boolean): List<TouchEvent> {
					//console.error("TOUCH EVENT!")
					val out = arrayListOf<TouchEvent>()

					val touches = e.unsafeCast<dynamic>().changedTouches
					val touchesLength: Int = touches.length.unsafeCast<Int>()
					for (n in 0 until touchesLength) {
						val touch = touches[n].unsafeCast<dynamic>()
						//console.error(touch)
						out += TouchEvent().apply {
							this.type = type
							this.touch.current.x = (touch.pageX * xEventScale).unsafeCast<Double>()
							this.touch.current.y = (touch.pageY * yEventScale).unsafeCast<Double>()
							this.touch.id = touch.identifier.unsafeCast<Int>()
							this.scaleCoords = false
						}
					}
					if (preventDefault) e.preventDefault()
					return out
				}

				return listOf(
					node.addCloseableEventListener("touchstart") {
						for (info in process(TouchEvent.Type.START, it, preventDefault = true)) ed.dispatch(info)
					},
					node.addCloseableEventListener("touchend") {
						for (info in process(TouchEvent.Type.END, it, preventDefault = true)) ed.dispatch(info)
					},
					node.addCloseableEventListener("touchmove") {
						for (info in process(TouchEvent.Type.MOVE, it, preventDefault = true)) ed.dispatch(info)
					}
				).closeable()
			}
			DropFileEvent::class -> {
				fun ondrop(e: DragEvent) {
					e.preventDefault()
					//console.log("ondrop", e)
					val dt = e.dataTransfer ?: return
					val files = arrayListOf<File>()
					for (n in 0 until dt.items.length) {
						val item = dt.items[n] ?: continue
						val file = item.getAsFile() ?: continue
						files += file
						//console.log("ondrop", file)
					}
					//jsEmptyArray()
					val fileSystem = JsFilesVfs(files)
					ed.dispatch(DropFileEvent(DropFileEvent.Type.ENTER, files.map { fileSystem[it.name] }))
				}

				fun ondragenter(e: DragEvent) {
					e.preventDefault()
					ed.dispatch(DropFileEvent(DropFileEvent.Type.ENTER, null))
				}

				fun ondragexit(e: DragEvent) {
					e.preventDefault()
					ed.dispatch(DropFileEvent(DropFileEvent.Type.EXIT, null))
				}

				return listOf(
					node.addCloseableEventListener("drop") {
						//console.log("html5drop")
						ondrop(it.unsafeCast<DragEvent>())
					},
					node.addCloseableEventListener("dragenter") { ondragenter(it.unsafeCast<DragEvent>()) },
					node.addCloseableEventListener("dragover") { it.preventDefault() },
					node.addCloseableEventListener("dragleave") { ondragexit(it.unsafeCast<DragEvent>()) }
				).closeable()
			}
		}
		return DummyCloseable
	}

	override fun <T> callAction(c: Any, key: LightAction<T>, param: T) {
        if (c.isInvalid()) return
		when (key) {
			LightAction.FOCUS -> {
				val child = c.asDynamic()
				child.focus()
			}
		}
	}

	override fun <T> setProperty(c: Any, key: LightProperty<T>, value: T) {
        if (c.isInvalid()) return
        val document = this.document ?: return
		val child = c.unsafeCast<HTMLElement>()
		val childOrDocumentBody = if (child.nodeName.toLowerCase() == "article") document.body else child
		val nodeName = child.nodeName.toLowerCase()
		when (key) {
			LightProperty.TEXT -> {
				val v = key[value]
				if (nodeName == "article") {
					document.title = v
				} else if (nodeName == "input" || nodeName == "textarea") {
					(child.unsafeCast<HTMLInputElement>()).value = v
				} else {
					if ((child.asDynamic()["data-type"]) == "checkbox") {
						(child.querySelector("span") as? HTMLSpanElement?)?.innerText = v
					} else {
						child.innerText = v
					}
				}
			}
			LightProperty.PROGRESS_CURRENT -> {
				val v = key[value]
				(child.unsafeCast<HTMLInputElement>()).value = "$v"
			}
			LightProperty.PROGRESS_MAX -> {
				val v = key[value]
				(child.unsafeCast<HTMLInputElement>()).max = "$v"
			}
			LightProperty.BGCOLOR -> {
				val v = key[value]
				childOrDocumentBody?.style?.background = v.htmlColor
			}
			LightProperty.IMAGE_SMOOTH -> {
				val v = key[value]
				child.style.imageRendering = if (v) "auto" else "pixelated"
			}
			LightProperty.ICON -> {
				val v = key[value]
				if (v != null) {
					val href = HtmlImage.htmlCanvasToDataUrl(HtmlImage.bitmapToHtmlCanvas(v.toBMP32()))

					var link: HTMLLinkElement? =
						document.querySelector("link[rel*='icon']").unsafeCast<HTMLLinkElement>()
					if (link == null) {
						link = document.createElement("link").unsafeCast<HTMLLinkElement>()
					}
					link.type = "image/x-icon"
					link.rel = "shortcut icon"
					link.href = href
					document.getElementsByTagName("head")[0]?.appendChild(link)
				}
			}
			LightProperty.IMAGE -> {
				val bmp = key[value]
				if (bmp is HtmlNativeImage) {
					setCanvas(c, bmp.lazyCanvasElement.asDynamic())
				} else {
					setImage32(c, bmp?.toBMP32())
				}
			}
			LightProperty.VISIBLE -> {
				val v = key[value]
				child.style.display = if (v) "block" else "none"
			}
			LightProperty.CHECKED -> {
				val v = key[value]
				(child.querySelector("input[type=checkbox]").unsafeCast<HTMLInputElement>()).checked = v
			}
		}
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T> getProperty(c: Any, key: LightProperty<T>): T {
        if (c.isInvalid()) return super.getProperty(c, key)
		val child = c.unsafeCast<HTMLElement>()

		when (key) {
			LightProperty.TEXT -> {
				return (child.unsafeCast<HTMLInputElement>()).value.unsafeCast<T>()
			}
			LightProperty.CHECKED -> {
				val input = (child.unsafeCast<HTMLInputElement>()).querySelector("input[type=checkbox]")
				val checked: Boolean = input.asDynamic().checked
				return checked.unsafeCast<T>()
			}
		}
		return super.getProperty(c, key)
	}

	private fun setCanvas(c: Any, bmp: HTMLCanvasElement?) {
        if (c.isInvalid()) return
		val targetCanvas = c.unsafeCast<HTMLCanvasElement>()
		if (bmp != null) {
			targetCanvas.width = bmp.width
			targetCanvas.height = bmp.height
			val ctx = targetCanvas.getContext("2d").unsafeCast<CanvasRenderingContext2D>()
			HtmlImage.htmlCanvasClear(targetCanvas.asDynamic())
			ctx.drawImage(bmp, 0.0, 0.0)
			//println("DRAW CANVAS")
		} else {
			HtmlImage.htmlCanvasClear(targetCanvas.asDynamic())
			//println("DRAW CANVAS CLEAR")
		}
	}

    private fun Any.isInvalid(): Boolean = this is Dummy

	private fun setImage32(c: Any, bmp: Bitmap32?) {
        if (c.isInvalid()) return
		val canvas = c.unsafeCast<HTMLCanvasElement>()
		if (bmp != null) {
            canvas.width = bmp.width
            canvas.height = bmp.height
			HtmlImage.renderToHtmlCanvas(bmp, canvas.asDynamic())
			//println("DRAW IMAGE")
		} else {
			HtmlImage.htmlCanvasClear(canvas.asDynamic())
			//println("DRAW IMAGE CLEAR")
		}
	}

	override fun setBounds(c: Any, x: Int, y: Int, width: Int, height: Int) {
        if (c.isInvalid()) return
		val child = c.unsafeCast<HTMLElement>()
		val childStyle = child.style
		childStyle.left = "${x}px"
		childStyle.top = "${y}px"
		childStyle.width = "${width}px"
		childStyle.height = "${height}px"

		if (child is HTMLCanvasElement) {
			//lightLog.error { "CANVAS size: $x,$y,$width,$height" }
			child.width = (width * xScale).toInt()
			child.height = (height * yScale).toInt()

            //println("HtmlLightComponents.devicePixelRatio=$devicePixelRatio")
            //println("HtmlLightComponents.xScale=$xScale")
            //println("HtmlLightComponents.xEventScale=$xEventScale")
        }
	}

	override fun repaint(c: Any) {
		mainFrame?.style?.visibility = "visible"
	}

	override suspend fun dialogAlert(c: Any, message: String) {
        if (c.isInvalid()) return
        val window = this.window ?: return

        return suspendCancellableCoroutine { c ->
            window.alert(message)
            window.setTimeout({
                c.resume(Unit)
            }, 0)
        }
    }

	override suspend fun dialogPrompt(c: Any, message: String, initialValue: String): String {
        if (c.isInvalid()) return "cancelled"
        val window = this.window ?: return "cancelled"

		return suspendCancellableCoroutine { c ->
			val result = window.prompt(message, initialValue)
			window.setTimeout({
				if (result == null) {
					c.resumeWithException(CancellationException("cancelled"))
				} else {
					c.resume(result)
				}
			}, 0)
		}
    }

	override suspend fun dialogOpenFile(c: Any, filter: String): VfsFile {
        if (c.isInvalid()) throw CancellationException("cancel")
        val window = this.window ?: throw CancellationException("cancel")
        val document = this.document ?: throw CancellationException("cancel")

        return suspendCancellableCoroutine { continuation ->
            val inputFile = windowInputFile
            var completedOnce = false
            var files = arrayOf<File>()

            val completed = {
                if (!completedOnce) {
                    completedOnce = true

                    selectedFiles = files

                    //console.log('completed', files);
                    if (files.size > 0.0) {
                        val fileName = files[0].name
                        val ff = arrayListOf<File>()
                        for (n in 0 until selectedFiles.asDynamic().length) ff += selectedFiles[n].unsafeCast<File>()
                        val sf = JsFilesVfs(ff)
                        continuation.resume(sf[fileName])
                    } else {
                        continuation.resumeWithException(CancellationException("cancel"))
                    }
                }
            }

            windowInputFile?.value = ""

            windowInputFile?.onclick = {
                document.body?.onfocus = {
                    document.body?.onfocus = null
                    window.setTimeout({
                        completed()
                    }, 2000)
                }
                Unit
            }

            windowInputFile?.onchange = { e ->
                files = e.target.asDynamic()["files"]
                //var v = this.value;
                //console.log(v);
                completed()
            }

            inputFile?.click()
        }
    }

	override fun openURL(url: String): Unit {
        val window = this.window ?: return
		window.open(url, "_blank")
	}

	override fun getDpi(): Double {
        val window = this.window ?: return 1.0
		return (window.devicePixelRatio.toInt() * 96).toDouble()
	}

	override fun getDevicePixelRatio(): Double {
        val window = this.window ?: return 1.0
		return window.devicePixelRatio ?: 1.0
	}

	override fun configuredFrame(handle: Any) {
		getEventListener(handle).dispatch(ReshapeEvent(windowWidth, windowHeight))
	}
}

class JsFileAsyncStreamBase(val jsfile: File) : AsyncStreamBase() {
	//val alength = jsfile.size.unsafeCast<Double>().toLong()
//
	//init {
	//	console.log("JsFileAsyncStreamBase.Opened ", jsfile)
	//	console.log("JsFileAsyncStreamBase.Length: " + alength)
	//}

	override suspend fun getLength(): Long {
		return jsfile.size.unsafeCast<Double>().toLong()
	}

	suspend fun readBytes(position: Double, len: Int): ByteArray = suspendCoroutine { c ->
		val reader = FileReader()

		reader.onload = {
			val result = reader.result
			c.resume(Int8Array(result.unsafeCast<ArrayBuffer>()).unsafeCast<ByteArray>())
		}

		reader.onerror = {
			c.resumeWithException(RuntimeException("error reading file"))
		}

		reader.readAsArrayBuffer(jsfile.asDynamic().slice(position, (position + len)))
	}

	override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
		val data = readBytes(position.toDouble(), len)
		arraycopy(data, 0, buffer, offset, data.size)
		//console.log("JsFileAsyncStreamBase.read.requested", buffer)
		//console.log("JsFileAsyncStreamBase.read.requested", position, offset, len)
		//console.log("JsFileAsyncStreamBase.read", data)
		//console.log("JsFileAsyncStreamBase.read.result:", data.size)
		return data.size
	}
}

internal class JsFilesVfs(val files: List<File>) : Vfs() {
	private fun _locate(name: String): File? {
		val length = files.size
		for (n in 0 until length) {
			val file = files[n]
			if (file.name == name) {
				return file
			}
		}
		return null
	}

	private fun locate(path: String): File? = _locate(path.trim('/'))

	override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
		val jsfile = locate(path) ?: throw FileNotFoundException(path)
		return JsFileAsyncStreamBase(jsfile).toAsyncStream()
	}

	override suspend fun stat(path: String): VfsStat {
		val file = locate(path) ?: return createNonExistsStat(path)
		return createExistsStat(path, isDirectory = false, size = file.size.toDouble().toLong())
	}

	override suspend fun list(path: String): ReceiveChannel<VfsFile> {
		return this.files.map { this[it.name] }.toChannel()
	}

	override fun toString(): String = "JsFilesVfs"
}

object Nimbus_111_1420_Safari_GamepadMapping : GamepadMapping() {
	override val id = "111-1420-Nimbus"

	override fun get(button: GameButton, buttons: Int, axes: DoubleArray): Double {
		return when (button) {
			GameButton.BUTTON0 -> buttons.getButton(0)
			GameButton.BUTTON1 -> buttons.getButton(1)
			GameButton.BUTTON2 -> buttons.getButton(2)
			GameButton.BUTTON3 -> buttons.getButton(3)
			GameButton.L1 -> buttons.getButton(4)
			GameButton.R1 -> buttons.getButton(5)
			GameButton.L2 -> buttons.getButton(6)
			GameButton.R2 -> buttons.getButton(7)
			GameButton.LEFT -> buttons.getButton(8)
			GameButton.DOWN -> buttons.getButton(9)
			GameButton.RIGHT -> buttons.getButton(10)
			GameButton.UP -> buttons.getButton(11)
			GameButton.SELECT -> 0.0
			GameButton.START -> 0.0
			GameButton.SYSTEM -> 0.0
			GameButton.LX -> axes[0]
			GameButton.LY -> axes[1]
			GameButton.RX -> axes[2]
			GameButton.RY -> axes[3]
			else -> 0.0
		}
	}
}

val knownControllers by lazy {
    listOf(
        StandardGamepadMapping,
        Nimbus_111_1420_Safari_GamepadMapping
    ).associateBy { it.id }
}
