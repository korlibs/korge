package korlibs.render.macos

import korlibs.event.*
import korlibs.math.geom.*
import korlibs.memory.*
import korlibs.render.*
import kotlinx.cinterop.*
import platform.AppKit.*
import platform.Foundation.*
import platform.darwin.*

class OpenGLView(
    private val defaultGameWindow: MyDefaultGameWindow,
    frame: CValue<NSRect>,
    pixelFormat: NSOpenGLPixelFormat
) : NSOpenGLView(frame, pixelFormat), NSTextInputProtocol {

    override fun acceptsFirstResponder(): Boolean = true
    override fun becomeFirstResponder(): Boolean = true

    //fun getHeight() = openglView.bounds.height
    private fun getHeight() = bounds.height

    private var lastModifierFlags: Int = 0

    //var customCursor: NSCursor? = null

    // @TODO: Broken in Kotlin 1.8.0: https://youtrack.jetbrains.com/issue/KT-55653/Since-Kotlin-1.8.0-NSView.resetCursorRects-doesnt-exist-anymore-and-cannot-override-it
    //override fun resetCursorRects() {
    //    val cursor = defaultGameWindow.cursor
    //    val nsCursor = cursor.nsCursor
    //    addCursorRect(bounds, nsCursor)
    //    println("MyNSOpenGLView.resetCursorRects: bounds=${bounds.toRectangle()}, cursor=$cursor, nsCursor=$nsCursor")
    //}

    fun dispatchFlagIfRequired(event: NSEvent, mask: Int, key: Key) {
        val old = (lastModifierFlags and mask) != 0
        val new = (event.modifierFlags.toInt() and mask) != 0
        if (old == new) return

        defaultGameWindow.dispatchKeyEventEx(
            type = if (new) KeyEvent.Type.DOWN else KeyEvent.Type.UP,
            id = 0,
            character = ' ',
            key = key,
            keyCode = key.ordinal,
            shift = event.shift,
            ctrl = event.ctrl,
            alt = event.alt,
            meta = event.meta
        )
    }

    override fun flagsChanged(event: NSEvent) {
        dispatchFlagIfRequired(event, NSShiftKeyMask.toInt(), Key.LEFT_SHIFT)
        dispatchFlagIfRequired(event, NSControlKeyMask.toInt(), Key.LEFT_CONTROL)
        dispatchFlagIfRequired(event, NSAlternateKeyMask.toInt(), Key.LEFT_ALT)
        dispatchFlagIfRequired(event, NSCommandKeyMask.toInt(), Key.META)
        dispatchFlagIfRequired(event, NSFunctionKeyMask.toInt(), Key.FUNCTION)
        dispatchFlagIfRequired(event, NSEventModifierFlagCapsLock.toInt(), Key.CAPS_LOCK)

        lastModifierFlags = event.modifierFlags.toInt()
    }

    private val gestureEvent = GestureEvent()
    override fun magnifyWithEvent(event: NSEvent) {
        defaultGameWindow.dispatch(gestureEvent.also {
            it.type = GestureEvent.Type.MAGNIFY
            it.id = 0
            it.amount = event.magnification().toFloat()
        })
        super.magnifyWithEvent(event)
    }

    // https://developer.apple.com/documentation/appkit/nsevent
    override fun rotateWithEvent(event: NSEvent) {
        defaultGameWindow.dispatch(gestureEvent.also {
            it.type = GestureEvent.Type.ROTATE
            it.id = 0
            it.amount = event.rotation.toFloat()
        })

        super.rotateWithEvent(event)
    }

    override fun swipeWithEvent(event: NSEvent) {
        defaultGameWindow.dispatch(gestureEvent.also {
            it.type = GestureEvent.Type.SWIPE
            it.id = 0
            it.amountX = event.deltaX.toFloat()
            it.amountY = event.deltaY.toFloat()
        })
        super.swipeWithEvent(event)
    }

    override fun smartMagnifyWithEvent(event: NSEvent) {
        defaultGameWindow.dispatch(gestureEvent.also {
            it.type = GestureEvent.Type.SMART_MAGNIFY
            it.id = 0
            it.amount = 1f
        })
        super.smartMagnifyWithEvent(event)
    }

    override fun scrollWheel(event: NSEvent) {
        mouseEvent(MouseEvent.Type.SCROLL, event)
    }

    override fun mouseUp(event: NSEvent) = mouseEvent(MouseEvent.Type.UP, event)
    override fun rightMouseUp(event: NSEvent) = mouseEvent(MouseEvent.Type.UP, event)
    override fun otherMouseUp(event: NSEvent) = mouseEvent(MouseEvent.Type.UP, event)

    override fun mouseDown(event: NSEvent) = mouseEvent(MouseEvent.Type.DOWN, event)
    override fun rightMouseDown(event: NSEvent) = mouseEvent(MouseEvent.Type.DOWN, event)
    override fun otherMouseDown(event: NSEvent) = mouseEvent(MouseEvent.Type.DOWN, event)

    override fun mouseDragged(event: NSEvent) = mouseEvent(MouseEvent.Type.DRAG, event)
    override fun rightMouseDragged(event: NSEvent) = mouseEvent(MouseEvent.Type.DRAG, event)
    override fun otherMouseDragged(event: NSEvent) = mouseEvent(MouseEvent.Type.DRAG, event)

    override fun mouseMoved(event: NSEvent) = mouseEvent(MouseEvent.Type.MOVE, event)

    private fun mouseEvent(etype: MouseEvent.Type, e: NSEvent) {
        val ex = e.locationInWindow.x.toInt()
        val ey = (getHeight() - e.locationInWindow.y).toInt()
        val ebutton = e.buttonNumber.toInt()

        val factor = defaultGameWindow.backingScaleFactor
        val sx = ex * factor
        val sy = ey * factor

        defaultGameWindow.dispatchMouseEvent(
            id = 0,
            type = etype,
            x = sx.toInt(),
            y = sy.toInt(),
            button = when (etype) {
                MouseEvent.Type.SCROLL -> MouseButton.BUTTON_WHEEL
                else -> button(ebutton)
            },
            buttons = when (etype) {
                MouseEvent.Type.SCROLL -> 0
                else -> buttonMask(e.buttonMask.toInt())
            },
            scrollDeltaX = -e.deltaX.toFloat(), scrollDeltaY = -e.deltaY.toFloat(), scrollDeltaZ = -e.deltaZ.toFloat(),
            isShiftDown = e.shift, isCtrlDown = e.ctrl, isAltDown = e.alt, isMetaDown = e.meta,
            scrollDeltaMode = MouseEvent.ScrollDeltaMode.PIXEL
        )
    }

    private fun buttonMask(mask: Int): Int {
        var out = 0
        for (n in 0 until 8) {
            if (mask.extractBool(n)) out = out or button(n).bits
        }
        return out
    }

    private fun button(index: Int): MouseButton {
        return when (index) {
            0 -> MouseButton.LEFT
            1 -> MouseButton.RIGHT
            2 -> MouseButton.MIDDLE
            else -> MouseButton[index]
        }
    }

    var lastFn = false
    var lastShift = false
    var lastCtrl = false
    var lastAlt = false
    var lastMeta = false

    private fun keyDownUp(event: NSEvent, pressed: Boolean, e: NSEvent) {
        val str = event.charactersIgnoringModifiers ?: "\u0000"
        val c = str.getOrNull(0) ?: '\u0000'
        val cc = c.toInt().toChar()
        val char = cc
        val keyCode = event.keyCode.toInt()

        val rawKey = KeyCodesToKeys[keyCode] ?: CharToKeys[char] ?: Key.UNKNOWN
        val key = when {
            (rawKey == Key.BACKSPACE || rawKey == Key.DELETE) -> if (event.fn) Key.DELETE else Key.BACKSPACE
            else -> rawKey
        }

        lastModifierFlags = event.modifierFlags.toInt()

        //println("keyDownUp: char=$char, keyCode=${keyCode.toInt()}, key=$key, pressed=$pressed, shift=${e.shift}, ctrl=${e.ctrl}, alt=${e.alt}, meta=${e.meta}, characters=${event.characters}, event.willBeHandledByComplexInputMethod()=${event.willBeHandledByComplexInputMethod()}")

        defaultGameWindow.dispatchKeyEventEx(
            type = if (pressed) KeyEvent.Type.DOWN else KeyEvent.Type.UP,
            id = 0,
            character = char,
            key = key,
            keyCode = keyCode,
            shift = e.shift,
            ctrl = e.ctrl,
            alt = e.alt,
            meta = e.meta
        )
    }

    override fun keyDown(event: NSEvent) {
        lastFn = event.fn
        lastShift = event.shift
        lastCtrl = event.ctrl
        lastAlt = event.alt
        lastMeta = event.meta

        interpretKeyEvents(listOf(event))
        keyDownUp(event, true, event)
    }

    override fun keyUp(event: NSEvent) {
        keyDownUp(event, false, event)
    }

    override fun insertText(string: Any?) {
        if (string == null) return
        for (char in string.toString()) {
            defaultGameWindow.dispatchKeyEventEx(
                type = KeyEvent.Type.TYPE,
                id = 0,
                character = char,
                key = Key.UNKNOWN,
                keyCode = char.code,
                shift = lastShift,
                ctrl = lastCtrl,
                alt = lastAlt,
                meta = lastMeta
            )
        }
    }

    var inputRect: Rectangle = Rectangle.ZERO

    fun setInputRectangle(windowRect: Rectangle) {
        this.inputRect = windowRect
    }

    // @TODO: Used for example when partially typing japanese. We need to display partial text while typing somehow
    override fun setMarkedText(string: Any?, selectedRange: CValue<NSRange>) = Unit//.also { println("setMarkedText: '$string', $selectedRange") }
    // @TODO: We should set the rectangle of the text input so IME places completion box at the right place
    override fun firstRectForCharacterRange(range: CValue<NSRange>): CValue<NSRect> = NSMakeRect(
        0.0.cg, 0.0.cg, 0.0.cg, 0.0.cg
        //(this.bounds.left + inputRect.x).toCgFloat(),
        //(this.bounds.top + inputRect.y).toCgFloat(),
        //(inputRect.width).toCgFloat(),
        //(inputRect.height).toCgFloat()
    )//.also { println("firstRectForCharacterRange: $range") }
    override fun attributedSubstringFromRange(range: CValue<NSRange>): NSAttributedString? = null//.also { println("attributedSubstringFromRange: $range") }
    override fun characterIndexForPoint(point: CValue<NSPoint>): NSUInteger = 0u//.also { println("characterIndexForPoint: $point") }
    override fun conversationIdentifier(): NSInteger = 0//.also { println("conversationIdentifier") }
    override fun doCommandBySelector(selector: COpaquePointer?) = Unit//.also { println("doCommandBySelector: $selector") }
    override fun hasMarkedText(): Boolean = false//.also { println("hasMarkedText") }
    override fun markedRange(): CValue<NSRange> = NSMakeRange(0u, 0u)//.also { println("markedRange") }
    override fun selectedRange(): CValue<NSRange> = NSMakeRange(0u, 0u)//.also { println("selectedRange") }
    override fun unmarkText() = Unit//.also { println("unmarkText") }
    override fun validAttributesForMarkedText(): List<*>? = null//.also { println("validAttributesForMarkedText") }
}
