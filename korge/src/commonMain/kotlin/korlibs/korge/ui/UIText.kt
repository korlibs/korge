package korlibs.korge.ui

import korlibs.korge.input.*
import korlibs.korge.render.*
import korlibs.korge.style.*
import korlibs.korge.view.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.text.*
import korlibs.math.geom.*

// @TODO: Replace with TextBlock
inline fun Container.uiText(
    text: String,
    width: Double = 128.0,
    height: Double = 18.0,
    block: @ViewDslMarker UIText.() -> Unit = {}
): UIText = UIText(text, width, height).addTo(this).apply(block)

class UIText(
    text: String,
    width: Double = 128.0,
    height: Double = 64.0,
) : UIView(width, height) {
    protected var bover by uiObservable(false) { updateState() }
    protected var bpressing by uiObservable(false) { updateState() }

    private val background = solidRect(width, height, Colors.TRANSPARENT)
    private val textView = text(text, font = DefaultTtfFontAsBitmap)
    var bgcolor: RGBA = Colors.TRANSPARENT

    var text: String by textView::text

    init {
        mouse {
            onOver {
                simulateOver()
            }
            onOut {
                simulateOut()
            }
            onDown {
                simulateDown()
            }
            onUpAnywhere {
                simulateUp()
            }
        }
    }

    fun simulateOver() {
        bover = true
    }

    fun simulateOut() {
        bover = false
    }

    fun simulatePressing(value: Boolean) {
        bpressing = value
    }

    fun simulateDown() {
        bpressing = true
    }

    fun simulateUp() {
        bpressing = false
    }

    private val textBounds = MRectangle()

    override fun renderInternal(ctx: RenderContext) {
        background.visible = bgcolor.a != 0
        background.colorMul = bgcolor
        textBounds.setTo(0.0, 0.0, width, height)
        textView.setFormat(face = styles.textFont, size = styles.textSize.toInt(), color = styles.textColor, align = styles.textAlignment)
        textView.setTextBounds(textBounds.immutable)
        //background.size(width, height)
        textView.text = text
        super.renderInternal(ctx)
    }

    override fun updateState() {
        super.updateState()
    }
}