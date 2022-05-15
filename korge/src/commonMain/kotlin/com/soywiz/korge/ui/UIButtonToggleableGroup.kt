package com.soywiz.korge.ui

import com.soywiz.korge.input.onClick

inline fun <T : UIButton> T.group(group: UIButtonToggleableGroup, pressed: Boolean = false): T = group.add(this, pressed)

class UIButtonToggleableGroup {
    //private val buttons = arrayListOf<UIButton>()
    var pressedButton: UIButton? = null
        set(value) {
            if (field != null) {
                field?.forcePressed = false
            }
            field = value
            field?.forcePressed = true
        }


    fun <T : UIButton> add(button: T, pressed: Boolean = false): T {
        //buttons.add(button)
        if (pressed) {
            pressedButton = button
        }
        button.onClick { pressedButton = button }
        return button
    }
}
