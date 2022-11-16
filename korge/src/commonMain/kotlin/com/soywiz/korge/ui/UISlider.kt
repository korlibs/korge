package com.soywiz.korge.ui

import com.soywiz.kmem.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.property.*
import com.soywiz.korim.color.*
import com.soywiz.korim.text.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*

inline fun Container.uiSlider(
    value: Number = UISlider.DEFAULT_VALUE,
    min: Number = UISlider.DEFAULT_MIN,
    max: Number = UISlider.DEFAULT_MAX,
    step: Number = UISlider.DEFAULT_STEP,
    decimalPlaces: Int = UISlider.decimalPlacesFromStep(step.toDouble()),
    width: Double = UISlider.DEFAULT_WIDTH,
    height: Double = UISlider.DEFAULT_HEIGHT,
    block: @ViewDslMarker UISlider.() -> Unit = {}
): UISlider = UISlider(value, min, max, step, decimalPlaces, width, height).addTo(this).apply(block)

class UISlider(
    value: Number = DEFAULT_VALUE, min: Number = DEFAULT_MIN, max: Number = DEFAULT_MAX, step: Number = DEFAULT_STEP,
    decimalPlaces: Int = DEFAULT_DECIMAL_PLACES,
    width: Double = DEFAULT_WIDTH, height: Double = DEFAULT_HEIGHT
) : UIView(width, height) {
    companion object {
        const val DEFAULT_VALUE = 0
        const val DEFAULT_MIN = 0
        const val DEFAULT_MAX = 100
        const val DEFAULT_STEP = 1.0
        const val DEFAULT_DECIMAL_PLACES = 1
        const val DEFAULT_WIDTH = 128.0
        const val DEFAULT_HEIGHT = 16.0
        const val NO_STEP = 0.0

        fun decimalPlacesFromStep(step: Double): Int = when {
            step >= 1.0 -> 0
            step > 0.01 -> 1
            else -> 2
        }
    }

    @ViewProperty
    val bg = solidRect(width, height, RGBA(32, 32, 32))
    val button = solidRect(height, height, Colors.DARKGREY)
    val text = text("", alignment = TextAlignment.TOP_LEFT, color = Colors.BLACK)

    val onChange: Signal<Double> = Signal()

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
                onChange(rvalue)
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
            this@UISlider.value = (localMouseX(views) - button.width / 2).convertRange(0.0, maxXPos, this@UISlider.min, this@UISlider.max)
        }
    }
}

fun <T : UISlider> T.changed(block: (Double) -> Unit): T {
    onChange.add(block)
    return this
}
