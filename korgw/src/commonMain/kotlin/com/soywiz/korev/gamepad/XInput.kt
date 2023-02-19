package com.soywiz.korev.gamepad

import com.soywiz.kmem.*
import com.soywiz.korev.*

object XInputMapping {
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

    private fun GamepadInfo.setDigital(button: GameButton, buttons: Int, bit: Int) {
        this.rawButtons[button.index] = if (buttons.hasBitSet(bit)) 1f else 0f
    }

    fun setController(
        gamepad: GamepadInfo,
        wButtons: Short = 0,
        bLeftTrigger: Byte = 0,
        bRightTrigger: Byte = 0,
        sThumbLX: Short = 0,
        sThumbLY: Short = 0,
        sThumbRX: Short = 0,
        sThumbRY: Short = 0,
    ) {
        val buttons = wButtons.toInt() and 0xFFFF

        gamepad.setDigital(GameButton.UP, buttons, XINPUT_GAMEPAD_DPAD_UP)
        gamepad.setDigital(GameButton.DOWN, buttons, XINPUT_GAMEPAD_DPAD_DOWN)
        gamepad.setDigital(GameButton.LEFT, buttons, XINPUT_GAMEPAD_DPAD_LEFT)
        gamepad.setDigital(GameButton.RIGHT, buttons, XINPUT_GAMEPAD_DPAD_RIGHT)
        gamepad.setDigital(GameButton.BACK, buttons, XINPUT_GAMEPAD_BACK)
        gamepad.setDigital(GameButton.START, buttons, XINPUT_GAMEPAD_START)
        gamepad.setDigital(GameButton.LEFT_THUMB, buttons, XINPUT_GAMEPAD_LEFT_THUMB)
        gamepad.setDigital(GameButton.RIGHT_THUMB, buttons, XINPUT_GAMEPAD_RIGHT_THUMB)
        gamepad.setDigital(GameButton.LEFT_SHOULDER, buttons, XINPUT_GAMEPAD_LEFT_SHOULDER)
        gamepad.setDigital(GameButton.RIGHT_SHOULDER, buttons, XINPUT_GAMEPAD_RIGHT_SHOULDER)
        gamepad.setDigital(GameButton.XBOX_A, buttons, XINPUT_GAMEPAD_A)
        gamepad.setDigital(GameButton.XBOX_B, buttons, XINPUT_GAMEPAD_B)
        gamepad.setDigital(GameButton.XBOX_X, buttons, XINPUT_GAMEPAD_X)
        gamepad.setDigital(GameButton.XBOX_Y, buttons, XINPUT_GAMEPAD_Y)
        gamepad.rawButtons[GameButton.LEFT_TRIGGER.index] = convertUByteRangeToDouble(bLeftTrigger)
        gamepad.rawButtons[GameButton.RIGHT_TRIGGER.index] = convertUByteRangeToDouble(bRightTrigger)
        gamepad.rawButtons[GameButton.LX.index] = convertShortRangeToDouble(sThumbLX)
        gamepad.rawButtons[GameButton.LY.index] = convertShortRangeToDouble(sThumbLY)
        gamepad.rawButtons[GameButton.RX.index] = convertShortRangeToDouble(sThumbRX)
        gamepad.rawButtons[GameButton.RY.index] = convertShortRangeToDouble(sThumbRY)
    }

    private fun convertShortRangeToDouble(value: Short): Float = value.toFloat().convertRangeClamped(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat(), -1f, +1f)
    private fun convertUByteRangeToDouble(value: Byte): Float = (value.toInt() and 0xFF).toFloat().convertRangeClamped(0f, 255f, 0f, +1f)
}
