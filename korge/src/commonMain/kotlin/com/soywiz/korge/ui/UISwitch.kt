package com.soywiz.korge.ui

import com.soywiz.korge.view.*

inline fun Container.uiSwitch(
    width: Double = UI_DEFAULT_WIDTH,
    height: Double = UI_DEFAULT_HEIGHT,
    checked: Boolean = false,
    text: String = "Switch",
    block: @ViewDslMarker UISwitch.() -> Unit = {}
): UISwitch = UISwitch(width, height, checked, text).addTo(this).apply(block)

open class UISwitch(
    width: Double = UI_DEFAULT_WIDTH,
    height: Double = UI_DEFAULT_HEIGHT,
    checked: Boolean = false,
    text: String = "Switch",
) : UIBaseCheckBox<UISwitch>(width, height, checked, text, UIBaseCheckBoxSkin.Kind.SWITCH) {
}
