package com.soywiz.korgw

import android.view.*
import android.view.KeyEvent
import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korev.*

class AndroidGameController {
    private val activeGamepads = IntMap<GamepadInfo>()
    private fun getGamepadInfo(deviceId: Int): GamepadInfo = activeGamepads.getOrPut(deviceId) { GamepadInfo() }

    private fun GamepadInfo.setButton(button: GameButton, value: Float) {
        rawButtons[button.index] = value
    }
    private fun GamepadInfo.setButton(button: GameButton, value: Boolean) {
        setButton(button, value.toInt().toFloat())
    }

    private fun GamepadInfo.setButtonAxis(button: GameButton, event: MotionEvent, axis1: Int, axis2: Int = -1, axis3: Int = -1, reverse: Boolean = false, deadRange: Boolean = false) {
        val v1 = if (axis1 >= 0) event.getAxisValue(axis1) else 0f
        val v2 = if (axis2 >= 0) event.getAxisValue(axis2) else 0f
        val v3 = if (axis3 >= 0) event.getAxisValue(axis3) else 0f
        val value = if (v1 != 0f) v1 else if (v2 != 0f) v2 else if (v3 != 0f) v3 else 0f
        val rvalue = if (deadRange) GamepadInfo.withoutDeadRange(value) else value
        setButton(button, if (reverse) -rvalue else rvalue)
    }

    fun onKey(keyCode: Int, event: KeyEvent, type: com.soywiz.korev.KeyEvent.Type, long: Boolean): Boolean {
        val key = AndroidKeyMap.KEY_MAP[keyCode] ?: Key.UNKNOWN
        val sources = event.device?.sources ?: 0
        val isGamepad = sources.hasBits(InputDevice.SOURCE_GAMEPAD)

        //println("onKey: char=$char, keyCode=$keyCode, key=$key, sources=$sources, isGamepad=$isGamepad")

        //if (event.source.hasBits(InputDevice.SOURCE_GAMEPAD)) {
        if (!isGamepad) return false

        //println("GAMEPAD: $key")
        val info = getGamepadInfo(event.deviceId)
        val press = type == com.soywiz.korev.KeyEvent.Type.DOWN
        val button = when (key) {
            Key.LEFT -> GameButton.LEFT
            Key.RIGHT -> GameButton.RIGHT
            Key.UP -> GameButton.UP
            Key.DOWN -> GameButton.DOWN
            Key.XBUTTON_L1 -> GameButton.L1
            Key.XBUTTON_L2 -> GameButton.L2
            Key.XBUTTON_THUMBL -> GameButton.L3
            Key.XBUTTON_R1 -> GameButton.R1
            Key.XBUTTON_R2 -> GameButton.R2
            Key.XBUTTON_THUMBR -> GameButton.R3
            Key.XBUTTON_A -> GameButton.XBOX_A
            Key.XBUTTON_B -> GameButton.XBOX_B
            Key.XBUTTON_X -> GameButton.XBOX_X
            Key.XBUTTON_Y -> GameButton.XBOX_Y
            Key.XBUTTON_SELECT -> GameButton.SELECT
            Key.XBUTTON_START -> GameButton.START
            Key.XBUTTON_MODE -> GameButton.SYSTEM
            Key.MEDIA_RECORD -> GameButton.RECORD
            else -> {
                println(" - UNHANDLED GAMEPAD KEY: $key (keyCode=$keyCode)")
                null
            }
        }
        if (button != null) {
            info.setButton(button, press)
            return true
        }
        return false
    }

    fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val sources = event.device?.sources ?: 0
        val isGamepad = sources.hasBits(InputDevice.SOURCE_GAMEPAD)

        //println("onGenericMotionEvent: sources=$sources, isGamepad=$isGamepad")

        if (!isGamepad) return false

        val info = getGamepadInfo(event.deviceId)
        //println(" -> " + (0 until 47).associateWith { event.getAxisValue(it) }.filter { it.value != 0f })

        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

        info.setButton(GameButton.LEFT, (hatX < 0f))
        info.setButton(GameButton.RIGHT, (hatX > 0f))
        info.setButton(GameButton.UP, (hatY < 0f))
        info.setButton(GameButton.DOWN, (hatY > 0f))
        info.setButtonAxis(GameButton.LX, event, MotionEvent.AXIS_X, deadRange = true)
        info.setButtonAxis(GameButton.LY, event, MotionEvent.AXIS_Y, deadRange = true, reverse = true)
        info.setButtonAxis(GameButton.RX, event, MotionEvent.AXIS_RX, MotionEvent.AXIS_Z, deadRange = true)
        info.setButtonAxis(GameButton.RY, event, MotionEvent.AXIS_RY, MotionEvent.AXIS_RZ, deadRange = true, reverse = true)
        info.setButtonAxis(GameButton.LEFT_TRIGGER, event, MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_BRAKE)
        info.setButtonAxis(GameButton.RIGHT_TRIGGER, event, MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_GAS)
        return true
    }

    fun runPreFrame(gameWindow: GameWindow) {
        try {
            val gamepads = InputDevice.getDeviceIds().map { InputDevice.getDevice(it) }
                .filter { it.sources.hasBits(InputDevice.SOURCE_GAMEPAD) }.sortedBy { it.id }

            gameWindow.dispatchGamepadUpdateStart()
            gamepads.fastForEach {
                val info = getGamepadInfo(it.id)
                info.name = it.name
                gameWindow.dispatchGamepadUpdateAdd(info)
            }
            gameWindow.dispatchGamepadUpdateEnd()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
