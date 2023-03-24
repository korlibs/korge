package korlibs.korge.ui

import korlibs.memory.clamp
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.input.cursor
import korlibs.korge.input.mouse
import korlibs.korge.input.onMouseDrag
import korlibs.korge.view.Container
import korlibs.korge.view.ViewDslMarker
import korlibs.korge.view.append
import korlibs.korge.view.size
import korlibs.render.GameWindow
import korlibs.io.async.Signal
import korlibs.io.util.toStringDecimal
import korlibs.math.geom.*
import kotlin.math.absoluteValue

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
    width: Double = 64.0,
    height: Double = 18.0,
    block: @ViewDslMarker UIEditableNumber.() -> Unit = {}
): UIEditableNumber = append(UIEditableNumber(value, min, max, decimals, clamped, width, height)).apply(block)

// @TODO: lock cursor while dragging
@KorgeExperimental
class UIEditableNumber(value: Double = 0.0, min: Double = 0.0, max: Double = 1.0, var decimals: Int = 2, var clamped: Boolean = true, width: Double = 64.0, height: Double = 18.0) : UIView(width, height) {
    private val textView = uiText("", width, height)
    private val textInputView = uiTextInput("", width, height)
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
            it.mouseEvents.stopPropagation()
        }
    }
}