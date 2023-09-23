package korlibs.korge.ui

import korlibs.image.color.*
import korlibs.image.text.*
import korlibs.io.async.*
import korlibs.io.util.*
import korlibs.korge.input.*
import korlibs.korge.view.*
import korlibs.korge.view.property.*
import korlibs.math.*
import korlibs.math.geom.*

inline fun Container.uiSlider(
    value: Number = UISlider.DEFAULT_VALUE,
    min: Number = UISlider.DEFAULT_MIN,
    max: Number = UISlider.DEFAULT_MAX,
    step: Number = UISlider.DEFAULT_STEP,
    decimalPlaces: Int = UISlider.decimalPlacesFromStep(step.toDouble()),
    size: Size = UISlider.DEFAULT_SIZE,
    block: @ViewDslMarker UISlider.() -> Unit = {}
): UISlider = UISlider(value, min, max, step, decimalPlaces, size).addTo(this).apply(block)

class UISlider(
    value: Number = DEFAULT_VALUE, min: Number = DEFAULT_MIN, max: Number = DEFAULT_MAX, step: Number = DEFAULT_STEP,
    decimalPlaces: Int = DEFAULT_DECIMAL_PLACES,
    size: Size = UISlider.DEFAULT_SIZE,
    //width: Float = DEFAULT_WIDTH, height: Float = DEFAULT_HEIGHT
) : UIView(size) {
    companion object {
        const val DEFAULT_VALUE = 0
        const val DEFAULT_MIN = 0
        const val DEFAULT_MAX = 100
        const val DEFAULT_STEP = 1f
        const val DEFAULT_DECIMAL_PLACES = 1
        val DEFAULT_SIZE = Size(128, 16)
        const val NO_STEP = 0f

        fun decimalPlacesFromStep(step: Double): Int = when {
            step >= 1.0 -> 0
            step > 0.01 -> 1
            else -> 2
        }
    }

    val bg = solidRect(size, RGBA(32, 32, 32))
    val button = solidRect(size, Colors.DARKGREY)
    val text = text("", alignment = TextAlignment.TOP_LEFT, color = Colors.BLACK)

    val onChange: Signal<Float> = Signal()

    @ViewProperty
    var min: Double = min.toDouble()
        set(value) {
            if (field != value) {
                field = value
                reposition()
            }
        }

    @ViewProperty
    var max: Double = max.toDouble()
        set(value) {
            if (field != value) {
                field = value
                reposition()
            }
        }

    @ViewProperty
    var step: Double = step.toDouble()
        set(value) {
            if (field != value) {
                field = value
                reposition()
            }
        }

    @ViewProperty
    var value: Double = value.toDouble()
        set(value) {
            val rvalue = value.clamp(min, max).nearestAlignedTo(step)
            if (rvalue != field) {
                field = rvalue
                reposition()
                valueChanged()
                onChange(rvalue.toFloat())
            }
        }

    @ViewProperty
    var decimalPlaces: Int = decimalPlaces
        set(value) {
            field = value
            valueChanged()
        }

    private val maxXPos: Double get() = (bg.width - button.width)

    //val clampedValue: Int get() = value.clamp(min, max)

    private fun reposition() {
        this@UISlider.button.x = value.convertRange(min, max, 0.0, maxXPos).toDouble()
    }

    override fun onSizeChanged() {
        bg.size(width - 16.0, height)
        button.size(height, height)
        text.xy(width - 16.0, 0.0)
        reposition()
    }

    private fun valueChanged() {
        //text.text = value.toStringDecimal(decimalPlaces = decimalPlaces, skipTrailingZeros = true)
        text.text = value.toStringDecimal(decimalPlaces = decimalPlaces, skipTrailingZeros = false)
    }

    init {
        onSizeChanged()
        valueChanged()
    }

    init {
        this.onMouseDrag {
            this@UISlider.value = (localMousePos(views).x - button.width / 2).convertRange(0.0, maxXPos, this@UISlider.min, this@UISlider.max)
        }
    }
}

fun <T : UISlider> T.changed(block: (Float) -> Unit): T {
    onChange.add(block)
    return this
}
