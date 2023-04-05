package korlibs.korge.ui

import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.korge.input.*
import korlibs.korge.render.*
import korlibs.korge.style.*
import korlibs.korge.view.*
import korlibs.math.geom.*

// @TODO: Replace with TextBlock
inline fun Container.uiText(
    text: String,
    width: Float = 128f,
    height: Float = 18f,
    block: @ViewDslMarker UIText.() -> Unit = {}
): UIText = UIText(text, width, height).addTo(this).apply(block)

class UIText(
    text: String,
    width: Float = 128f,
    height: Float = 64f,
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
        textBounds.setTo(0.0, 0.0, widthD, heightD)
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
