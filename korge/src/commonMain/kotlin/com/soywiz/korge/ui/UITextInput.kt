package com.soywiz.korge.ui

import com.soywiz.klock.seconds
import com.soywiz.kmem.Platform
import com.soywiz.kmem.clamp
import com.soywiz.korev.Key
import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.component.onAttachDetach
import com.soywiz.korge.input.cursor
import com.soywiz.korge.input.doubleClick
import com.soywiz.korge.input.keys
import com.soywiz.korge.input.mouse
import com.soywiz.korge.time.timers
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.ViewDslMarker
import com.soywiz.korge.view.ViewRenderer
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.bounds
import com.soywiz.korge.view.clipContainer
import com.soywiz.korge.view.renderableView
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.text
import com.soywiz.korgw.GameWindow
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.Font
import com.soywiz.korio.async.Signal
import com.soywiz.korio.lang.withInsertion
import com.soywiz.korio.lang.withoutIndex
import com.soywiz.korio.lang.withoutRange
import com.soywiz.korio.util.endExclusive
import com.soywiz.korio.util.length
import com.soywiz.korma.geom.Margin
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.bounds
import com.soywiz.korma.geom.without
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

@KorgeExperimental
inline fun Container.uiTextInput(
    initialText: String = "",
    width: Double = 128.0,
    height: Double = 24.0,
    skin: ViewRenderer = BoxUISkin(),
    block: @ViewDslMarker UITextInput.() -> Unit = {}
): UITextInput = UITextInput(initialText, width, height, skin)
    .addTo(this).also { block(it) }

/**
 * Simple Single Line Text Input
 */
@KorgeExperimental
class UITextInput(initialText: String = "", width: Double = 128.0, height: Double = 24.0, skin: ViewRenderer = BoxUISkin()) : UIView(width, height), UIFocusable {

    //private val bg = ninePatch(NinePatchBmpSlice.createSimple(Bitmap32(3, 3) { x, y -> if (x == 1 && y == 1) Colors.WHITE else Colors.BLACK }.slice(), 1, 1, 2, 2), width, height).also { it.smoothing = false }
    private val bg = renderableView(width, height, skin)
    var skin by bg::viewRenderer
    private val container = clipContainer(0.0, 0.0)
    //private val container = fixedSizeContainer(width - 4.0, height - 4.0).position(2.0, 3.0)
    private val textView = container.text(initialText, 16.0, color = Colors.BLACK)
    private val caret = container.solidRect(2.0, 16.0, Colors.WHITE).also {
        it.blendMode = BlendMode.INVERT
        it.visible = false
    }

    var padding: Margin = Margin(3.0, 2.0, 2.0, 2.0)
        set(value) {
            field = value
            onSizeChanged()
        }

    init {
        onAttachDetach(onAttach = {
            this.stage.uiFocusManager
        })
        onSizeChanged()
    }

    override fun onSizeChanged() {
        bg.setSize(width, height)
        container.bounds(Rectangle(0.0, 0.0, width, height).without(padding))
    }

    val onEscPressed = Signal<UITextInput>()
    val onReturnPressed = Signal<UITextInput>()
    val onTextUpdated = Signal<UITextInput>()
    val onFocused = Signal<UITextInput>()
    val onFocusLost = Signal<UITextInput>()

    var text: String
        get() = textView.text
        set(value) {
            textView.text = value
            reclampSelection()
            onTextUpdated(this)
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

    fun selectAll() {
        select(0, text.length)
    }

    val selectionLength get() = (selectionEnd - selectionStart).absoluteValue
    val selectionText get() = text.substring(min(selectionStart, selectionEnd), max(selectionStart, selectionEnd))
    val selectionRange get() = min(selectionStart, selectionEnd) until max(selectionStart, selectionEnd)

    private val gameWindow get() = stage!!.views.gameWindow

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

    override var focused: Boolean
        set(value) {
            if (value == focused) return

            bg.isFocused = value

            if (value) {
                if (stage?.uiFocusedView != this) {
                    (stage?.uiFocusedView as? UIFocusable?)?.blur()
                    stage?.uiFocusedView = this
                }
                caret.visible = true
                //println("stage?.gameWindow?.showSoftKeyboard(): ${stage?.gameWindow}")
                stage?.uiFocusManager?.requestToggleSoftKeyboard(true, this)
            } else {
                if (stage?.uiFocusedView == this) {
                    stage?.uiFocusedView = null
                    caret.visible = false
                    if (stage?.uiFocusedView !is UITextInput) {
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

        cursor = GameWindow.Cursor.TEXT

        mouse {
            over { bg.isOver = true }
            out { bg.isOver = false }
        }

        timers.interval(0.5.seconds) {
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

        keys {
            this.typed {
                if (!focused) return@typed
                val code = it.character.code
                when (code) {
                    8, 127 -> Unit // backspace, backspace (handled by down event)
                    9, 10, 13 -> { // tab & return: Do nothing in single line text inputs
                        if (code == 10 || code == 13) {
                            onReturnPressed(this@UITextInput)
                        }
                    }
                    27 -> {
                        onEscPressed(this@UITextInput)
                    }
                    else -> {
                        val range = selectionRange
                        text = text.withoutRange(range).withInsertion(min(selectionStart, selectionEnd), it.characters())
                        cursorIndex++
                    }
                }
                //println(it.character.toInt())
                //println("NEW TEXT[${it.character.code}]: '${text}'")
            }
            down {
                if (!focused) return@down
                when (it.key) {
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
                        if (it.meta && Platform.os.isApple) {
                            moveToIndex(it.shift, 0)
                        } else {
                            moveToIndex(it.shift, leftIndex(selectionStart, it.ctrl))
                        }
                    }
                    Key.RIGHT -> {
                        if (it.meta && Platform.os.isApple) {
                            moveToIndex(it.shift, text.length)
                        } else {
                            moveToIndex(it.shift, rightIndex(selectionStart, it.ctrl))
                        }
                    }
                    Key.HOME -> moveToIndex(it.shift, 0)
                    Key.END -> moveToIndex(it.shift, text.length)
                    else -> Unit
                }
            }
        }

        container.mouse {
            var dragging = false
            bg.alpha = 0.7
            over {
                bg.alpha = 1.0
            }
            out {
                bg.alpha = 0.7
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
                val isFocusedView = stage?.uiFocusedView == this@UITextInput
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
}
