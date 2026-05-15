package korlibs.korge.ui

import korlibs.korge.view.*
import korlibs.math.geom.*

inline fun Container.uiSwitch(
    size: Size = UI_DEFAULT_SIZE,
    checked: Boolean = false,
    text: String = "Switch",
    block: @ViewDslMarker UISwitch.() -> Unit = {}
): UISwitch = UISwitch(size, checked, text).addTo(this).apply(block)

open class UISwitch(
    size: Size = UI_DEFAULT_SIZE,
    checked: Boolean = false,
    text: String = "Switch",
) : UIBaseCheckBox<UISwitch>(size, checked, text, UISwitch) {
    companion object : Kind()
}
