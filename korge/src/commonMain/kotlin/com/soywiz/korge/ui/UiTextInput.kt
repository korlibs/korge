package com.soywiz.korge.ui

import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korev.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.component.*
import com.soywiz.korge.input.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import kotlin.math.*
import kotlin.text.isLetterOrDigit

@KorgeExperimental
inline fun Container.uiTextInput(
    initialText: String = "",
    width: Double = 128.0,
    height: Double = 24.0,
    block: @ViewDslMarker UiTextInput.() -> Unit = {}
): UiTextInput = UiTextInput(initialText, width, height)
    .addTo(this).also { block(it) }

/**
 * Simple Single Line Text Input
 */
@KorgeExperimental
class UiTextInput(initialText: String = "", width: Double = 128.0, height: Double = 24.0) : UIView(width, height), UiFocusable {

    private val bg = ninePatch(NinePatchBmpSlice.createSimple(Bitmap32(3, 3) { x, y -> if (x == 1 && y == 1) Colors.WHITE else Colors.BLACK }.slice(), 1, 1, 2, 2), width, height).also { it.smoothing = false }
    private val container = clipContainer(width - 4.0, height - 4.0).position(2.0, 3.0)
    //private val container = fixedSizeContainer(width - 4.0, height - 4.0).position(2.0, 3.0)
    private val textView = container.text(initialText, 16.0, color = Colors.BLACK)
    private val caret = container.solidRect(2.0, 16.0, Colors.WHITE).also {
        it.blendMode = BlendMode.INVERT
        it.visible = false
    }

    init {
        onAttachDetach(onAttach = {
            this.stage.uiFocusManager
        })
    }

    val onReturnPressed = Signal<UiTextInput>()
    val onTextUpdated = Signal<UiTextInput>()

    var text: String
        get() = textView.text
        set(value) {
            textView.text = value
            onTextUpdated(this)
        }

    var font: Font
        get() = textView.font as Font
        set(value) {
            textView.font = value
        }

    var textSize: Double by textView::textSize
    var textColor: RGBA by textView::color

    private var _selectionStart: Int = initialText.length
    private var _selectionEnd: Int = _selectionStart

    private fun clampIndex(index: Int) = index.clamp(0, text.length)

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

    fun setSelection(start: Int, end: Int) {
        _selectionStart = clampIndex(start)
        _selectionEnd = clampIndex(end)
        updateCaretPosition()
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
            x += glyph.metrics.width
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
                if (n == glyphPositions.size) x += glyph.metrics.width
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
            if (value) {
                if (stage?.uiFocusedView != this) {
                    (stage?.uiFocusedView as? UiFocusable?)?.blur()
                    stage?.uiFocusedView = this
                }
                caret.visible = true
                //println("stage?.gameWindow?.showSoftKeyboard(): ${stage?.gameWindow}")
                stage?.uiFocusManager?.requestToggleSoftKeyboard(true, this)
            } else {
                if (stage?.uiFocusedView == this) {
                    stage?.uiFocusedView = null
                    caret.visible = false
                    if (stage?.uiFocusedView !is UiTextInput) {
                        stage?.uiFocusManager?.requestToggleSoftKeyboard(false, null)
                    }
                }
            }
        }
        get() = stage?.uiFocusedView == this

    init {
        //println(metrics)

        mouse {
            over { gameWindow.cursor = GameWindow.Cursor.TEXT }
            out { gameWindow.cursor = GameWindow.Cursor.DEFAULT }
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
                            onReturnPressed(this@UiTextInput)
                        }
                    }
                    else -> {
                        val range = selectionRange
                        text = text.withoutRange(range).withInsertion(cursorIndex, "${it.character}")
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
                                    cursorIndex--
                                }
                            } else {
                                if (cursorIndex < text.length) {
                                    text = text.withoutIndex(cursorIndex)
                                }
                            }
                        }
                    }
                    Key.LEFT -> moveToIndex(it.shift, leftIndex(selectionStart, it.ctrl))
                    Key.RIGHT -> moveToIndex(it.shift, rightIndex(selectionStart, it.ctrl))
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
            moveAnywhere {
                //println("UiTextInput.moveAnywhere")
                if (focused) {
                    if (it.pressing) {
                        dragging = true
                        selectionEnd = getIndexAtPos(it.currentPosLocal)
                    }
                }
            }
            upOutside {
                val isFocusedView = stage?.uiFocusedView == this@UiTextInput
                //println("UiTextInput.upOutside: dragging=$dragging, isFocusedView=$isFocusedView, view=${it.view}, stage?.uiFocusedView=${stage?.uiFocusedView}")
                if (!dragging && isFocusedView) {
                    //println(" -- BLUR")
                    blur()
                }
                dragging = false
            }
            doubleClick {
                //println("UiTextInput.doubleClick")
                val index = getIndexAtPos(it.currentPosLocal)
                setSelection(leftIndex(index, true), rightIndex(index, true))
            }
        }

        updateCaretPosition()
    }
}
