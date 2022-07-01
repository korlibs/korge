package com.soywiz.korge.text

import com.soywiz.kds.HistoryStack
import com.soywiz.klock.seconds
import com.soywiz.kmem.Platform
import com.soywiz.kmem.clamp
import com.soywiz.korev.ISoftKeyboardConfig
import com.soywiz.korev.Key
import com.soywiz.korev.KeyEvent
import com.soywiz.korev.SoftKeyboardConfig
import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.component.onNewAttachDetach
import com.soywiz.korge.input.cursor
import com.soywiz.korge.input.doubleClick
import com.soywiz.korge.input.newKeys
import com.soywiz.korge.input.newMouse
import com.soywiz.korge.time.timers
import com.soywiz.korge.ui.UIFocusManager
import com.soywiz.korge.ui.UIFocusable
import com.soywiz.korge.ui.UITextInput
import com.soywiz.korge.ui.blur
import com.soywiz.korge.ui.uiFocusManager
import com.soywiz.korge.ui.uiFocusedView
import com.soywiz.korge.util.CancellableGroup
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.NinePatch
import com.soywiz.korge.view.NinePatchEx
import com.soywiz.korge.view.RenderableView
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.Text
import com.soywiz.korge.view.View
import com.soywiz.korge.view.solidRect
import com.soywiz.korgw.GameWindow
import com.soywiz.korgw.TextClipboardData
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.Font
import com.soywiz.korio.async.Signal
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.lang.cancel
import com.soywiz.korio.lang.withInsertion
import com.soywiz.korio.lang.withoutIndex
import com.soywiz.korio.lang.withoutRange
import com.soywiz.korio.util.endExclusive
import com.soywiz.korio.util.length
import com.soywiz.korma.geom.Margin
import com.soywiz.korma.geom.Point
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

@KorgeExperimental
class TextEditController(
    val textView: Text,
    val caretContainer: Container = textView,
    val eventHandler: View = textView,
    val bg: RenderableView? = null,
) : Closeable, UIFocusable, ISoftKeyboardConfig by SoftKeyboardConfig() {
    val stage: Stage? get() = textView.stage
    var initialText: String = textView.text
    private val closeables = CancellableGroup()
    override val UIFocusManager.focusView: View get() = this@TextEditController.textView
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

    private val caret = caretContainer.solidRect(2.0, 16.0, Colors.WHITE).also {
        it.blendMode = BlendMode.INVERT
        it.visible = false
    }

    var padding: Margin = Margin(3.0, 2.0, 2.0, 2.0)
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
            updateCaretSize()
        }

    var textSize: Double
        get() = textView.textSize
        set(value) {
            textView.textSize = value
            updateCaretSize()
        }
    var textColor: RGBA by textView::color

    private fun updateCaretSize() {
        val metrics = font.getFontMetrics(textSize)
        caret.scaledHeight = metrics.top.absoluteValue + metrics.bottom.absoluteValue
    }

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
        select(range.first, range.endExclusive)
    }

    fun selectAll() {
        select(0, text.length)
    }

    val selectionLength: Int get() = (selectionEnd - selectionStart).absoluteValue
    val selectionText: String get() = text.substring(min(selectionStart, selectionEnd), max(selectionStart, selectionEnd))
    val selectionRange: IntRange get() = min(selectionStart, selectionEnd) until max(selectionStart, selectionEnd)

    private val gameWindow get() = textView.stage!!.views.gameWindow

    fun getPosAtIndex(index: Int): Point {
        val glyphPositions = textView.getGlyphMetrics().glyphs
        if (glyphPositions.isEmpty()) return Point(0, 0)
        val glyph = glyphPositions[min(index, glyphPositions.size - 1)]
        var x = glyph.x
        if (index >= glyphPositions.size) {
            x += glyph.metrics.xadvance
        }
        return Point(x, glyph.y)
    }

    fun getIndexAtPos(pos: Point): Int {
        val glyphPositions = textView.getGlyphMetrics().glyphs

        var index = 0
        var minDist = Double.POSITIVE_INFINITY

        if (glyphPositions.isNotEmpty()) {
            for (n in 0 until glyphPositions.size + 1) {
                val glyph = glyphPositions[min(glyphPositions.size - 1, n)]
                var x = glyph.x
                if (n == glyphPositions.size) x += glyph.metrics.xadvance
                val dist = (pos.x - x).absoluteValue
                if (minDist > dist) {
                    minDist = dist
                    index = n
                }
            }
        }

        return index
    }

    fun getXAtIndex(index: Int): Double {
        return getPosAtIndex(index).x
    }

    fun updateCaretPosition() {
        val range = selectionRange
        val startX = getXAtIndex(range.start)
        val endX = getXAtIndex(range.endExclusive)

        caret.x = startX
        caret.scaledWidth = endX - startX + (if (range.isEmpty()) 2.0 else 0.0)
        caret.visible = focused
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
                    if (dir < 0) {
                        return idx - dir
                    } else {
                        return idx
                    }
                }
                if (!text[idx].isLetterOrDigit()) {
                    if (dir < 0) {
                        if (idx == sidx) return idx
                        return idx - dir
                    } else {
                        return idx
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

    var acceptTextChange: (old: String, new: String) -> Boolean = { old, new -> true }

    override var focused: Boolean
        set(value) {
            if (value == focused) return

            bg?.isFocused = value

            if (value) {
                if (stage?.uiFocusedView != this) {
                    stage?.uiFocusedView?.blur()
                    stage?.uiFocusedView = this
                }
                caret.visible = true
                //println("stage?.gameWindow?.showSoftKeyboard(): ${stage?.gameWindow}")
                stage?.uiFocusManager?.requestToggleSoftKeyboard(true, this)
            } else {
                if (stage?.uiFocusedView == this) {
                    stage?.uiFocusedView = null
                    caret.visible = false
                    if (stage?.uiFocusedView !is ISoftKeyboardConfig) {
                        stage?.uiFocusManager?.requestToggleSoftKeyboard(false, null)
                    }
                }
            }

            if (value) {
                onFocused(this)
            } else {
                onFocusLost(this)
            }
        }
        get() = stage?.uiFocusedView == this

    init {
        //println(metrics)

        this.eventHandler.cursor = GameWindow.Cursor.TEXT

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
                    Key.C, Key.V, Key.Z -> {
                        if (it.isNativeCtrl()) {
                            when (it.key) {
                                Key.Z -> {
                                    if (it.shift) redo() else undo()
                                }
                                Key.C -> {
                                    gameWindow.clipboardWrite(TextClipboardData(selectionText))
                                }
                                Key.V -> {
                                    val rtext = (gameWindow.clipboardRead() as? TextClipboardData?)?.text
                                    if (rtext != null) insertText(rtext)
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
                                    text = text.withoutIndex(cursorIndex - 1)
                                    if (text.length > cursorIndex) cursorIndex--
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
            down {
                //println("UiTextInput.down")
                cursorIndex = getIndexAtPos(it.currentPosLocal)
                focused = true
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
                    it.stopPropagation()
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
    fun KeyEvent.isNativeCtrl(): Boolean = if (Platform.os.isApple) this.meta else this.ctrl

    override fun close() {
        this.textView.cursor = null
        closeables.cancel()
    }
}

fun Text.editText(caretContainer: Container = this): TextEditController =
    TextEditController(this, caretContainer)
