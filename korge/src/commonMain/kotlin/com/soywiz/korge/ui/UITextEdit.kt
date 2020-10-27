package com.soywiz.korge.ui

/**
 *
 * @author Matthias Wienand and Dr.D.H.Akehurst
 *
 */
/*
class UITextEdit(textArg: String, fontArg: TtfFont, sizeArg: Double, colorArg: RGBA) {
    var font = fontArg
    var size = sizeArg
    var color = colorArg
    var text = textArg

    val caret = Caret({ text.length }) {
        image.bitmap = render()
    }
    val selection = Selection()

    // initial rendering
    val image = Image(render())

    var _handle_input = false
    fun handleKeyboardInput() {
        if (_handle_input) return
        _handle_input = true
        image.onKeyUp { onKeyUp(it) }
        image.onKeyDown { onKeyDown(it) }
        // TODO: unregister (maybe replace Image)
    }

    var readOnly = false

    fun hideCaret() {
        caret.stopBlinking()
        if (caret.showCaret) {
            caret.showCaret = false
            image.bitmap = render()
        }
    }

    fun showCaret() {
        caret.stopBlinking()
        if (!caret.showCaret) {
            caret.showCaret = true
            image.bitmap = render()
        }
    }

    fun setReadOnly() {
        readOnly = true
    }

    fun setEditable() {
        readOnly = false
        handleKeyboardInput()
        showCaret()
    }

    class Caret(endPosArg: ()->Int, blinkActionArg: ()->Unit) {
        // TODO: clean-up relationship between TextEdit.text and Caret
        // -> Maybe have setter for the text and notify caret about text changes
        val _getEndPos = endPosArg
        val endpos get() = _getEndPos()
        var pos = endpos
        var blinkAction = blinkActionArg
        var caretColor = Colors.DARKGREY
        var showCaret = false

        interface CaretRenderer {
            fun render(ctxt: Context2d, height: Int, charWidth: Int, dsc: Int): Int
        }

        val Center = object : CaretRenderer {
            var width = 2
            override fun render(ctxt: Context2d, height: Int, charWidth: Int, dsc: Int): Int {
                var extra = 0
                if (pos == endpos) extra = width / 2
                val loc = if (pos == 0) 0 else -width / 2
                ctxt.fillRect(loc, 0, width, height)
                return extra
            }
        }
        val FillChar = object : CaretRenderer {
            override fun render(ctxt: Context2d, height: Int, charWidth: Int, dsc: Int): Int {
                ctxt.fillRect(0, 0, charWidth, height)
                return charWidth
            }
        }
        val Underscore = object : CaretRenderer {
            var width = 4
            override fun render(ctxt: Context2d, height: Int, charWidth: Int, dsc: Int): Int {
                ctxt.fillRect(0, height - dsc - width, charWidth, width)
                return charWidth
            }
        }

        var type = Center

        fun render(ctxt: Context2d, height: Int, charWidth: Int, dsc: Int): Int {
            if (!showCaret) return 0
            var extra = 0 // extra space needed on the right side of the rendering to show the caret
            ctxt.fillStyle(caretColor) {
                extra = type.render(ctxt, height, charWidth, dsc)
            }
            return extra
        }

        var blinkId = 0
        var blinkDuration = 500L
        var blinkTime = DateTime.now().unixMillisLong - blinkDuration

        fun resetBlinkTimer() {
            showCaret = true
            blinkTime = DateTime.now().unixMillisLong + blinkDuration
        }

        suspend fun blink() {
            suspend fun doBlink(id: Int) {
                val now = DateTime.now().unixMillisLong
                if (blinkTime <= now) {
                    // we need to blink immediately!
                    showCaret = !showCaret
                    blinkAction()
                    blinkTime = now + blinkDuration
                } else {
                    // sleep for the remaining time
                    var sleep = blinkTime - now
                    if (sleep > blinkDuration) sleep = blinkDuration
                    delay(TimeSpan(sleep.toDouble()))
                }
                // blink until stopped
                if (id == blinkId) doBlink(id)
            }
            doBlink(blinkId)
        }

        fun stopBlinking() {
            blinkId++
        }

        fun moveTo(xArg: Int, mods: ModSet, sel: Selection) {
            // sanitize inputs
            var x = xArg
            if (x < 0) x = 0
            else if (x > endpos) x = endpos

            // update selection
            if (mods.shift) {
                sel.change(pos, x - pos)
            } else {
                sel.reset()
            }

            // update caret position
            pos = x
        }

        fun moveBy(step: Int, mods: ModSet, selection: Selection) {
            moveTo(pos + step, mods, selection)
        }

        fun delete(text: String, direction: Int): String {
            val next = pos + direction
            if (next < 0 || next > endpos) return text
            val res = text.substring(0, minOf(pos, next)) + text.substring(maxOf(pos, next))
            pos += direction
            return res
        }
    }

    class Selection {
        var range = 0..0
        var foreground = Colors.BLACK
        var background = Colors.LIGHTBLUE

        val start get() = range.start
        val last get() = range.last
        fun isEmpty() = start == last
        fun isPresent() = start != last
        fun contains(x: Int) = range.contains(x)

        fun reset() {
            range = 0..0
        }

        fun change(x: Int, by: Int) {
            var next = x + by
            if (next < 0) next = 0
            if (next == x) return
            if (isEmpty()) range = minOf(x, next) .. maxOf(next, x)
            else {
                if (next <= x) {
                    if (start <= next) range = start .. next
                    else range = next .. last
                } else {
                    if (last < next) range = start .. next
                    else range = next .. last
                }
            }
        }

        fun cut(text: String): String {
            val res = text.substring(0, start) + text.substring(last)
            range = start .. start
            return res
        }

        fun replace(text: String, replacement: String): String {
            val res = text.substring(0, start) + replacement + text.substring(last)
            range = start .. start
            return res
        }
    }

    constructor(textArg: String, fontArg: TtfFont) : this(textArg, fontArg, 32.0, Colors.BLACK)
    constructor(fontArg: TtfFont) : this("", fontArg)

    fun render(): BitmapSlice<Bitmap> {
        val asc = font.ascender / 72
        val dsc = asc / 2
        val h = (size + asc + 0.5).toInt()
        val nativeImage = NativeImage(1024, h)
        val ctxt = nativeImage.getContext2d()

        // TODO: refactor rendering commands

        if (selection.contains(caret.pos)) {
            // selection rendering
            val preSel = text.substring(0, selection.start)
            val preCaret = text.substring(selection.start, caret.pos)
            val postCaret = text.substring(caret.pos, selection.last)
            val postSel = text.substring(selection.last)

            // render text before selection
            if (preSel.isNotEmpty()) {
                font.drawText(ctxt, text=preSel, size=size, x=0.0, y=0.0, paint=ColorPaint(color), TtfFont.Origin.TOP)
            }

            // save selection's x positions while rendering it the first time
            val selX1 = (ctxt.translate(0, 0).tx + 0.5).toInt()
            ctxt.save()
            val selText = preCaret + postCaret
            if (selText.isNotEmpty()) {
                font.drawText(ctxt, text=selText, size=size, x=0.0, y=0.0, paint=ColorPaint(color), TtfFont.Origin.TOP)
            }
            val selX2 = (ctxt.translate(0, 0).tx + 0.5).toInt()

            // render text after the selection and save final x position
            if (postSel.isNotEmpty()) {
                font.drawText(ctxt, text=postSel, size=size, x=0.0, y=0.0,paint=ColorPaint( color), TtfFont.Origin.TOP)
            }
            val finalX = (ctxt.translate(0, 0).tx + 0.5).toInt()

            // render selection background (drawing over the text)
            ctxt.fillStyle(selection.background) {
                ctxt.fillRect(-finalX + selX1, 0, selX2 - selX1, h)
            }

            // render the selected text (drawing over the selection background)
            ctxt.restore()
            if (preCaret.isNotEmpty()) {
                font.drawText(ctxt, text=preCaret, size=size,x= 0.0, y=0.0, paint=ColorPaint(selection.foreground), TtfFont.Origin.TOP)
            }
            val extra = caret.render(ctxt, h, _charWidth(), dsc)
            if (postCaret.isNotEmpty()) {
                font.drawText(ctxt, text=postCaret, size=size,x= 0.0, y=0.0, paint=ColorPaint(selection.foreground), TtfFont.Origin.TOP)
            }

            // return bitmap sliced to the actual text bounds
            var w = if (finalX < 1) 0 else finalX
            w += extra
            return nativeImage.sliceWithSize(0, 0, w, h)
        }

        // regular rendering
        val textPreCaret = text.substring(0, caret.pos)
        val textPostCaret = text.substring(caret.pos)
        if (textPreCaret.isNotEmpty()) {
            font.drawText(ctxt, size=size, text=textPreCaret, x=0.0, y=0.0, paint=ColorPaint(color), TtfFont.Origin.TOP)
        }
        val extra = caret.render(ctxt, h, _charWidth(), dsc)
        if (textPostCaret.isNotEmpty()) {
            font.drawText(ctxt, text=textPostCaret, size=size, x=0.0, y=0.0, paint=ColorPaint(color), TtfFont.Origin.TOP)
        }
        val w = (ctxt.translate(0, 0).tx + 0.5).toInt() + extra
        return nativeImage.sliceWithSize(0, 0, if (w < 1) 0 else w, h)
    }

    // TODO: real character width computation, test with mono font
    fun _charWidth() = (size / 2).toInt()

    // manage state of modifier keys
    class ModKey(vararg keySetArg: Key) {
        var isOn = false
        val keyMap = mutableMapOf<Key, Boolean>().apply {
            // TODO: initialize with actual keyboard state
            keySetArg.forEach { this[it] = false }
        }

        fun update(ev: KeyEvent) {
            if (ev.key in keyMap) {
                keyMap[ev.key] = ev.type == KeyEvent.Type.DOWN
                isOn = keyMap.any { it.value }
            }
        }
    }
    class ModSet {
        val modAlt = ModKey(Key.LEFT_ALT, Key.RIGHT_ALT)
        val modShift = ModKey(Key.LEFT_SHIFT, Key.RIGHT_SHIFT)

        val alt: Boolean get() = modAlt.isOn
        val shift: Boolean get() = modShift.isOn

        fun update(ev: KeyEvent) {
            modAlt.update(ev)
            modShift.update(ev)
        }
    }
    val mods = ModSet()

    // user interaction
    fun onKeyUp(ev: KeyEvent) {
        mods.update(ev)
    }

    fun _delete(direction: Int) {
        if (selection.isPresent()) {
            text = selection.cut(text)
            caret.pos = selection.start
        } else {
            text = caret.delete(text, direction)
        }
    }

    fun onKeyDown(ev: KeyEvent) {
        mods.update(ev)
        caret.resetBlinkTimer()
        //println("ev: " + ev.key + ", " + ev.keyCode + " <" + ev.character + ">")
        if (ev.key == Key.BACKSPACE) {
            if (!readOnly) _delete(-1)
        } else if (ev.key == Key.DELETE) {
            if (!readOnly) _delete(1)
        } else if (ev.key == Key.LEFT) {
            caret.moveBy(-1, mods, selection)
        } else if (ev.key == Key.RIGHT) {
            caret.moveBy(1, mods, selection)
        } else if (ev.character.isPrintable() && !readOnly) {
            if (selection.isPresent()) {
                text = selection.replace(text, ev.character.toString())
                caret.pos = selection.start + 1
            } else {
                text = text.substring(0, caret.pos) + ev.character + text.substring(caret.pos)
                caret.pos++
            }
        } else {
            // prevent unnecessary rendering
            return
            // TODO: word-by-word with ALT
            // TODO: full line with CMD
        }
        image.bitmap = render()
    }

    // TODO: restricted width + align (left,center,right) + ellipsis ("some text that is too lo...")
    // TODO: font styles (bold, underlined, overlined, striked-through, italic, etc.)
    // Maybe consider building stuff around this:
    // TextEditFlow: multi-line text (and multi-line selection)
    // . line-wrap (by word or by character)
    // . attributed sections (define_style('x', Style(Bold, Underlined)); set_style('x', 4, 6))
    // . optimized use of resources (if necessary)
}
*/
