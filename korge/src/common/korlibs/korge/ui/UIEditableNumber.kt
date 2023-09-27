package korlibs.korge.ui

import korlibs.io.async.*
import korlibs.io.util.*
import korlibs.korge.annotations.*
import korlibs.korge.input.*
import korlibs.korge.view.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.render.*
import kotlin.math.*

@KorgeExperimental
class UIEditableNumberProps(
    prop: UIProperty<Double>,
    val min: Double = 0.0,
    val max: Double = 1.0,
    val decimals: Int = 2,
    val clamped: Boolean = true,
) : UIObservableProperty<Double>(prop)

class UIEditableBooleanProps(
    prop: UIProperty<Boolean>,
) : UIObservableProperty<Boolean>(prop)

@KorgeExperimental
inline fun Container.uiEditableNumber(
    value: Double = 0.0, min: Double = 0.0, max: Double = 1.0, decimals: Int = 2, clamped: Boolean = true,
    size: Size = Size(64, 18),
    block: @ViewDslMarker UIEditableNumber.() -> Unit = {}
): UIEditableNumber = append(UIEditableNumber(value, min, max, decimals, clamped, size)).apply(block)

// @TODO: lock cursor while dragging
@KorgeExperimental
class UIEditableNumber(value: Double = 0.0, min: Double = 0.0, max: Double = 1.0, var decimals: Int = 2, var clamped: Boolean = true, size: Size = Size(64, 18)) : UIView(size) {
    private val textView = uiText("", size)
    private val textInputView = uiTextInput("", size)
        .also { it.visible = false }
        .also { it.padding = Margin.ZERO }
    var min: Double = min
    var max: Double = max

    override fun onSizeChanged() {
        super.onSizeChanged()
        textView.size(width, height)
        textInputView.size(width, height)
    }

    private fun getValueText(value: Double = this.value): String {
        return value.toStringDecimal(decimals)
    }

    val onSetValue = Signal<UIEditableNumber>()
    var value: Double = Double.NaN
        set(value) {
            val clampedValue = if (clamped) value.clamp(min, max) else value
            if (field != clampedValue || textView.text.isEmpty()) {
                field = clampedValue
                textView.text = getValueText()
                onSetValue(this)
            }
        }

    private var oldValue: Double = value

    val isEditing get() = textInputView.visible

    private fun setTextInputVisible(visible: Boolean, useValue: Boolean = true) {
        textView.visible = !visible
        textInputView.visible = visible
        if (textInputView.visible) {
            oldValue = value
            textView.text = getValueText()
            textInputView.text = getValueText()
            textInputView.focus()
            textInputView.selectAll()
        } else {
            value = if (useValue) textInputView.text.toDoubleOrNull() ?: oldValue else oldValue
        }
    }

    init {
        this.value = value
        cursor = GameWindow.Cursor.RESIZE_EAST
        var start = 0.0
        textInputView.onReturnPressed { setTextInputVisible(false, useValue = true) }
        textInputView.onEscPressed { setTextInputVisible(false, useValue = false) }
        textInputView.onFocusLost { setTextInputVisible(false, useValue = true) }
        mouse {
            down {
                //currentEvent?.requestLock?.invoke()
                //views.gameWindow.lockMousePointer()
            }
            click {
                setTextInputVisible(!textInputView.visible)
            }
        }
        onMouseDrag {
            if (textInputView.visible) return@onMouseDrag
            if (it.start) {
                start = this@UIEditableNumber.value
            }
            val dist = (max - min).absoluteValue
            this@UIEditableNumber.value = (start + dist * (it.dx / (width * 2)))
            it.mouseEvents.preventDefault()
        }
    }
}
