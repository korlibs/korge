package com.soywiz.korev.gamepad

import com.soywiz.korev.*

object XInputMapping : GamepadMapping() {
    const val XINPUT_GAMEPAD_DPAD_UP = 0
    const val XINPUT_GAMEPAD_DPAD_DOWN = 1
    const val XINPUT_GAMEPAD_DPAD_LEFT = 2
    const val XINPUT_GAMEPAD_DPAD_RIGHT = 3
    const val XINPUT_GAMEPAD_START = 4
    const val XINPUT_GAMEPAD_BACK = 5
    const val XINPUT_GAMEPAD_LEFT_THUMB = 6
    const val XINPUT_GAMEPAD_RIGHT_THUMB = 7
    const val XINPUT_GAMEPAD_LEFT_SHOULDER = 8
    const val XINPUT_GAMEPAD_RIGHT_SHOULDER = 9
    const val XINPUT_GAMEPAD_UNKNOWN_10 = 10
    const val XINPUT_GAMEPAD_UNKNOWN_11 = 11
    const val XINPUT_GAMEPAD_A = 12
    const val XINPUT_GAMEPAD_B = 13
    const val XINPUT_GAMEPAD_X = 14
    const val XINPUT_GAMEPAD_Y = 15

    const val SIZE = 16

    override val id = "XInput"

    override fun get(button: GameButton, info: GamepadInfo): Double = when (button) {
        GameButton.L2, GameButton.R2 -> info.rawButtonsPressure[button.index]
        else -> super.get(button, info)
    }

    override fun getButtonIndex(button: GameButton): Int = when (button) {
        GameButton.XBOX_A -> XINPUT_GAMEPAD_A
        GameButton.XBOX_B -> XINPUT_GAMEPAD_B
        GameButton.XBOX_X -> XINPUT_GAMEPAD_X
        GameButton.XBOX_Y -> XINPUT_GAMEPAD_Y
        GameButton.L1     -> XINPUT_GAMEPAD_LEFT_SHOULDER
        GameButton.R1     -> XINPUT_GAMEPAD_RIGHT_SHOULDER
        GameButton.LEFT_THUMB -> XINPUT_GAMEPAD_LEFT_THUMB
        GameButton.RIGHT_THUMB -> XINPUT_GAMEPAD_RIGHT_THUMB
        GameButton.BACK -> XINPUT_GAMEPAD_BACK
        GameButton.START -> XINPUT_GAMEPAD_START
        GameButton.UP -> XINPUT_GAMEPAD_DPAD_UP
        GameButton.DOWN -> XINPUT_GAMEPAD_DPAD_DOWN
        GameButton.LEFT -> XINPUT_GAMEPAD_DPAD_LEFT
        GameButton.RIGHT -> XINPUT_GAMEPAD_DPAD_RIGHT
        GameButton.SYSTEM -> -1
        else -> -1
    }
}
