package korlibs.korge.ui

import korlibs.korge.render.*
import korlibs.korge.style.*
import korlibs.korge.view.*
import korlibs.image.bitmap.*
import korlibs.io.async.*
import korlibs.math.geom.*

class UIRadioButtonGroup {
    private var mutableButtons = hashSetOf<UIRadioButton>()
    val buttons: Set<UIRadioButton> get() = mutableButtons
    val onChanged = Signal<UIRadioButton?>()
    var selectedButton: UIRadioButton? = null
        internal set(value) {
            if (field != value) {
                field?.checked = false
                field = value
                field?.checked = true
                onChanged(value)
            }
        }

    internal fun addRadio(button: UIRadioButton) {
        mutableButtons += button
        if (selectedButton == null) {
            button.checked = true
        }
    }

    internal fun removeRadio(button: UIRadioButton) {
        mutableButtons -= button
    }
}

inline fun Container.uiRadioButton(
    width: Double = UI_DEFAULT_WIDTH,
    height: Double = UI_DEFAULT_HEIGHT,
    checked: Boolean = false,
    text: String = "Radio Button",
    group: UIRadioButtonGroup = UIRadioButtonGroup(),
    block: @ViewDslMarker UIRadioButton.() -> Unit = {}
): UIRadioButton = UIRadioButton(width, height, checked, group, text).addTo(this).apply(block)

open class UIRadioButton(
    width: Double = UI_DEFAULT_WIDTH,
    height: Double = UI_DEFAULT_HEIGHT,
    checked: Boolean = false,
    group: UIRadioButtonGroup = UIRadioButtonGroup(),
    text: String = "Radio Button",
) : UIBaseCheckBox<UIRadioButton>(width, height, checked, text, UIRadioButton) {
    companion object : Kind()

    var group: UIRadioButtonGroup = group
        set(value) {
            if (field !== value) {
                field.removeRadio(this)
                field = value
                value.addRadio(this)
                invalidate()
            }
        }

    override var checked: Boolean
        get() = super.checked
        set(value) {
            if (super.checked != value) {
                super.checked = value
                if (value) group.selectedButton = this
                invalidate()
            }
        }

    init {
        group.addRadio(this)
        if (checked) {
            group.selectedButton = this
        }
    }

    override fun onComponentClick() {
        checked = true
        focused = true
    }
}
