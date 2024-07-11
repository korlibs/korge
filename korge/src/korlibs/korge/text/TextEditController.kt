package korlibs.korge.text

import korlibs.datastructure.*
import korlibs.event.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.io.async.*
import korlibs.io.lang.*
import korlibs.io.util.length
import korlibs.korge.annotations.*
import korlibs.korge.component.*
import korlibs.korge.input.*
import korlibs.korge.time.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.debug.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.geom.bezier.*
import korlibs.math.interpolation.*
import korlibs.platform.*
import korlibs.render.*
import korlibs.time.*
import kotlin.math.*
import kotlin.text.isLetterOrDigit

@KorgeExperimental
class TextEditController(
    val textView: Text,
    val caretContainer: Container = textView,
    val eventHandler: View = textView,
    val bg: RenderableView? = null,
) : AutoCloseable, UIFocusable, ISoftKeyboardConfig by SoftKeyboardConfig() {
    init {
        textView.focusable = this
    }

    val stage: Stage? get() = textView.stage
    var initialText: String = textView.text
    private val closeables = CancellableGroup()
    override val UIFocusManager.Scope.focusView: View get() = this@TextEditController.textView
    val onEscPressed = Signal<TextEditController>()
    val onReturnPressed = Signal<TextEditController>()
    val onTextUpdated = Signal<TextEditController>()
    val onFocused = Signal<TextEditController>()
    val onFocusLost = Signal<TextEditController>()
    val onOver = Signal<TextEditController>()
    val onOut = Signal<TextEditController>()
    val onSizeChanged = Signal<TextEditController>()

    //private val bg = ninePatch(NinePatchBmpSlice.createSimple(Bitmap32(3, 3) { x, y -> if (x == 1 && y == 1) Colors.WHITE else Colors.BLACK }.slice(), 1, 1, 2, 2), width, height).also { it.smoothing = false }
    /*
    private val bg = renderableView(width, height, skin)
    var skin by bg::viewRenderer
    private val container = clipContainer(0.0, 0.0)
    //private val container = fixedSizeContainer(width - 4.0, height - 4.0).position(2.0, 3.0)
    private val textView = container.text(initialText, 16.0, color = Colors.BLACK)
     */

    private val caret = caretContainer.debugVertexView().also {
        it.blendMode = BlendMode.INVERT
        it.visible = false
    }

    var padding: Margin = Margin(3f, 2f, 2f, 2f)
        set(value) {
            field = value
            onSizeChanged(this)
        }

    init {
        closeables += textView.onNewAttachDetach(onAttach = {
            this.stage.uiFocusManager
        })
        onSizeChanged(this)
    }

    //override fun onSizeChanged() {
    //    bg.setSize(width, height)
    //    container.bounds(Rectangle(0.0, 0.0, width, height).without(padding))
    //}

    data class TextSnapshot(var text: String, var selectionRange: IntRange) {
        fun apply(out: TextEditController) {
            out.setTextNoSnapshot(text)
            out.select(selectionRange)
        }
    }

    private val textSnapshots = HistoryStack<TextSnapshot>()

    private fun setTextNoSnapshot(text: String, out: TextSnapshot = TextSnapshot("", 0..0)): TextSnapshot? {
        if (!acceptTextChange(textView.text, text)) return null
        out.text = textView.text
        out.selectionRange = selectionRange
        textView.text = text
        reclampSelection()
        onTextUpdated(this)
        return out
    }

    var text: String
        get() = textView.text
        set(value) {
            val snapshot = setTextNoSnapshot(value)
            if (snapshot != null) {
                textSnapshots.push(snapshot)
            }
        }

    fun undo() {
        textSnapshots.undo()?.apply(this)
    }

    fun redo() {
        textSnapshots.redo()?.apply(this)
    }

    fun insertText(substr: String) {
        text = text
            .withoutRange(selectionRange)
            .withInsertion(min(selectionStart, selectionEnd), substr)
        cursorIndex += substr.length
    }

    var font: Font
        get() = textView.font as Font
        set(value) {
            textView.font = value
            updateCaretPosition()
        }

    var textSize: Double
        get() = textView.textSize
        set(value) {
            textView.textSize = value
            updateCaretPosition()
        }
    var textColor: RGBA by textView::color

    private var _selectionStart: Int = initialText.length
    private var _selectionEnd: Int = _selectionStart

    private fun clampIndex(index: Int) = index.clamp(0, text.length)

    private fun reclampSelection() {
        select(selectionStart, selectionEnd)
        selectionStart = selectionStart
    }

    var selectionStart: Int
        get() = _selectionStart
        set(value) {
            _selectionStart = clampIndex(value)
            updateCaretPosition()
        }

    var selectionEnd: Int
        get() = _selectionEnd
        set(value) {
            _selectionEnd = clampIndex(value)
            updateCaretPosition()
        }

    var cursorIndex: Int
        get() = selectionStart
        set(value) {
            val value = clampIndex(value)
            _selectionStart = value
            _selectionEnd = value
            updateCaretPosition()
        }

    fun select(start: Int, end: Int) {
        _selectionStart = clampIndex(start)
        _selectionEnd = clampIndex(end)
        updateCaretPosition()
    }

    fun select(range: IntRange) {
        select(range.first, range.last + 1)
    }

    fun selectAll() {
        select(0, text.length)
    }

    val selectionLength: Int get() = (selectionEnd - selectionStart).absoluteValue
    val selectionText: String get() = text.substring(min(selectionStart, selectionEnd), max(selectionStart, selectionEnd))
    var selectionRange: IntRange
        get() = min(selectionStart, selectionEnd) until max(selectionStart, selectionEnd)
        set(value) {
            select(value)
        }

    private val gameWindow get() = textView.stage!!.views.gameWindow

    fun getCaretAtIndex(index: Int): Bezier {
        val glyphPositions = textView.getGlyphMetrics().glyphs
        if (glyphPositions.isEmpty()) return Bezier(Point(), Point())
        val glyph = glyphPositions[min(index, glyphPositions.size - 1)]
        return when {
            index < glyphPositions.size -> glyph.caretStart
            else -> glyph.caretEnd
        }
    }

    /*
    init {
        caretContainer.gpuShapeView {
            for (g in textView.getGlyphMetrics().glyphs) {
                fill(Colors.WHITE) {
                    write(g.boundsPath)
                }
            }
        }
    }
    */

    fun getIndexAtPos(pos: Point): Int {
        val glyphPositions = textView.getGlyphMetrics().glyphs

        var index = 0
        var minDist = Double.POSITIVE_INFINITY

        if (glyphPositions.isNotEmpty()) {
            for (n in 0..glyphPositions.size) {
                val glyph = glyphPositions[min(glyphPositions.size - 1, n)]
                val dist = glyph.distToPath(pos)
                if (minDist > dist) {
                    minDist = dist
                    //println("[$n] dist=$dist")
                    index = when {
                        n >= glyphPositions.size - 1 && dist != 0.0 && glyph.distToPath(pos, startEnd = false) < glyph.distToPath(pos, startEnd = true) -> n + 1
                        else -> n
                    }
                }
            }
        }

        return index
    }

    fun updateCaretPosition() {
        val range = selectionRange
        //val startX = getCaretAtIndex(range.start)
        //val endX = getCaretAtIndex(range.endExclusive)

        val array = PointArrayList(if (range.isEmpty()) 2 else (range.length + 1) * 2)
        if (range.isEmpty()) {
            val last = (range.first >= this.text.length)
            val caret = getCaretAtIndex(range.first)
            val sign = if (last) -1.0 else +1.0
            val normal = caret.normal(Ratio.ZERO) * (2.0 * sign)
            val p0 = caret.points.first
            val p1 = caret.points.last
            array.add(p0)
            array.add(p1)
            array.add(p0 + normal)
            array.add(p1 + normal)
        } else {
            for (n in range.first..range.last + 1) {
                val caret = getCaretAtIndex(n)
                array.add(caret.points.first)
                array.add(caret.points.last)
                //println("caret[$n]=$caret")
            }
        }
        caret.colorMul = Colors.WHITE
        caret.pointsList = listOf(array)
        /*
        caret.x = startX.x0
        caret.scaledWidth = endX - startX + (if (range.isEmpty()) 2.0 else 0.0)
        */
        caret.visible = focused
        textView.invalidateRender()
    }

    fun moveToIndex(selection: Boolean, index: Int) {
        if (selection) selectionStart = index else cursorIndex = index
    }

    fun nextIndex(index: Int, direction: Int, word: Boolean): Int {
        val dir = direction.sign
        if (word) {
            val sidx = index + dir
            var idx = sidx
            while (true) {
                if (idx !in text.indices) {
                    return when {
                        dir < 0 -> idx - dir
                        else -> idx
                    }
                }
                if (!text[idx].isLetterOrDigit()) {
                    return when {
                        dir < 0 -> if (idx == sidx) idx else idx - dir
                        else -> idx
                    }
                }
                idx += dir
            }
        }
        return index + dir
    }

    fun leftIndex(index: Int, word: Boolean): Int = nextIndex(index, -1, word)
    fun rightIndex(index: Int, word: Boolean): Int = nextIndex(index, +1, word)

    override var tabIndex: Int = 0
    override val isFocusable: Boolean get() = true
    var acceptTextChange: (old: String, new: String) -> Boolean = { old, new -> true }

    override fun focusChanged(value: Boolean) {
        bg?.isFocused = value

        if (value) {
            caret.visible = true
            //println("stage?.gameWindow?.showSoftKeyboard(): ${stage?.gameWindow}")
            stage?.uiFocusManager?.requestToggleSoftKeyboard(true, this)
        } else {
            caret.visible = false
            if (stage?.uiFocusedView !is ISoftKeyboardConfig) {
                stage?.uiFocusManager?.requestToggleSoftKeyboard(false, null)
            }
        }

        if (value) {
            onFocused(this)
        } else {
            onFocusLost(this)
        }
    }

    //override var focused: Boolean
    //    set(value) {
    //        if (value == focused) return
//
    //        bg?.isFocused = value
//
    //
    //    }
    //    get() = stage?.uiFocusedView == this

    init {
        //println(metrics)

        this.eventHandler.cursor = Cursor.TEXT

        closeables += this.eventHandler.timers.interval(0.5.seconds) {
            if (!focused) {
                caret.visible = false
            } else {
                if (selectionLength == 0) {
                    caret.visible = !caret.visible
                } else {
                    caret.visible = true
                }
            }
        }

        closeables += this.eventHandler.newKeys {
            typed {
                //println("focused=$focused, focus=${textView.stage?.uiFocusManager?.uiFocusedView}")
                if (!focused) return@typed
                if (it.meta) return@typed
                val code = it.character.code
                when (code) {
                    8, 127 -> Unit // backspace, backspace (handled by down event)
                    9, 10, 13 -> { // tab & return: Do nothing in single line text inputs
                        if (code == 10 || code == 13) {
                            onReturnPressed(this@TextEditController)
                        }
                    }
                    27 -> {
                        onEscPressed(this@TextEditController)
                    }
                    else -> {
                        insertText(it.characters())
                    }
                }
                //println(it.character.toInt())
                //println("NEW TEXT[${it.character.code}]: '${text}'")
            }
            down {
                if (!focused) return@down
                when (it.key) {
                    Key.C, Key.V, Key.Z, Key.A, Key.X -> {
                        if (it.isNativeCtrl()) {
                            when (it.key) {
                                Key.Z -> {
                                    if (it.shift) redo() else undo()
                                }
                                Key.C, Key.X -> {
                                    if (selectionText.isNotEmpty()) {
                                        gameWindow.clipboardWrite(TextClipboardData(selectionText))
                                    }
                                    if (it.key == Key.X) {
                                        val selection = selectionRange
                                        text = text.withoutRange(selectionRange)
                                        moveToIndex(false, selection.first)
                                    }
                                }
                                Key.V -> {
                                    val rtext = (gameWindow.clipboardRead() as? TextClipboardData?)?.text
                                    if (rtext != null) insertText(rtext)
                                }
                                Key.A -> {
                                    selectAll()
                                }
                                else -> Unit
                            }
                        }
                    }
                    Key.BACKSPACE, Key.DELETE -> {
                        val range = selectionRange
                        if (range.length > 0) {
                            text = text.withoutRange(range)
                            cursorIndex = range.first
                        } else {
                            if (it.key == Key.BACKSPACE) {
                                if (cursorIndex > 0) {
                                    val oldCursorIndex = cursorIndex
                                    text = text.withoutIndex(cursorIndex - 1)
                                    cursorIndex = oldCursorIndex - 1 // This [oldCursorIndex] is required since changing text might change the cursorIndex already in some circumstances
                                }
                            } else {
                                if (cursorIndex < text.length) {
                                    text = text.withoutIndex(cursorIndex)
                                }
                            }
                        }
                    }
                    Key.LEFT -> {
                        when {
                            it.isStartFinalSkip() -> moveToIndex(it.shift, 0)
                            else -> moveToIndex(it.shift, leftIndex(selectionStart, it.isWordSkip()))
                        }
                    }
                    Key.RIGHT -> {
                        when {
                            it.isStartFinalSkip() -> moveToIndex(it.shift, text.length)
                            else -> moveToIndex(it.shift, rightIndex(selectionStart, it.isWordSkip()))
                        }
                    }
                    Key.HOME -> moveToIndex(it.shift, 0)
                    Key.END -> moveToIndex(it.shift, text.length)
                    else -> Unit
                }
            }
        }

        closeables += this.eventHandler.newMouse {
        //container.mouse {
            var dragging = false
            bg?.isOver = false
            onOut(this@TextEditController)
            over {
                onOver(this@TextEditController)
                bg?.isOver = true
            }
            out {
                onOut(this@TextEditController)
                bg?.isOver = false
            }
            downImmediate {
                cursorIndex = getIndexAtPos(it.currentPosLocal)
                dragging = false
                focused = true
            }
            down {
                //println("UiTextInput.down")
                cursorIndex = getIndexAtPos(it.currentPosLocal)
                dragging = false
            }
            downOutside {
                //println("UiTextInput.downOutside")
                dragging = false
                if (focused) {
                    focused = false
                    blur()
                }
            }
            moveAnywhere {
                //println("UiTextInput.moveAnywhere: focused=$focused, pressing=${it.pressing}")
                if (focused && it.pressing) {
                    dragging = true
                    selectionEnd = getIndexAtPos(it.currentPosLocal)
                    it.preventDefault()
                }
            }
            upOutside {
                //println("UiTextInput.upOutside: dragging=$dragging, isFocusedView=$isFocusedView, view=${it.view}, stage?.uiFocusedView=${stage?.uiFocusedView}")
                if (!dragging && focused) {
                    //println(" -- BLUR")
                    blur()
                }
                dragging = false
            }
            doubleClick {
                //println("UiTextInput.doubleClick")
                val index = getIndexAtPos(it.currentPosLocal)
                select(leftIndex(index, true), rightIndex(index, true))
            }
        }

        updateCaretPosition()
    }

    fun KeyEvent.isWordSkip(): Boolean = if (Platform.os.isApple) this.alt else this.ctrl
    fun KeyEvent.isStartFinalSkip(): Boolean = this.meta && Platform.os.isApple
    fun KeyEvent.isNativeCtrl(): Boolean = this.metaOrCtrl

    override fun close() {
        this.textView.cursor = null
        closeables.cancel()
        textView.focusable = null
    }
}

fun Text.editText(caretContainer: Container = this): TextEditController =
    TextEditController(this, caretContainer)
