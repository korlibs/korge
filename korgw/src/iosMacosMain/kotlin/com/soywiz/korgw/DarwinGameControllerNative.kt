package com.soywiz.korgw

import com.soywiz.kds.iterators.*
import com.soywiz.korev.*
import platform.GameController.*

class DarwinGameControllerNative {
    private val info = GamepadInfo()
    private val allControllers = Array<GCController?>(8) { null }

    private fun button(button: GameButton, value: Float) {
        info.rawButtons[button.index] = value
    }

    private fun button(button: GameButton, gcbutton: GCControllerButtonInput?) {
        if (gcbutton != null) button(button, gcbutton.value)
    }
    private fun stick(buttonX: GameButton, buttonY: GameButton, pad: GCControllerDirectionPad) {
        button(buttonX, GamepadInfo.withoutDeadRange(pad.xAxis.value))
        button(buttonY, GamepadInfo.withoutDeadRange(pad.yAxis.value))
    }

    @Suppress("RemoveRedundantCallsOfConversionMethods")
    fun updateGamepads(gameWindow: GameWindow) {
        for (n in allControllers.indices) allControllers[n] = null
        GCController.controllers().fastForEach {
            if (it is GCController) {
                val index = it.playerIndex.toInt()
                if (index in allControllers.indices) allControllers[index] = it
            }
        }

        gameWindow.dispatchGamepadUpdateStart()
        for (index in allControllers.indices) {
            val controller = allControllers[index] ?: continue

            // https://developer.apple.com/documentation/gamecontroller/gcmicrogamepad
            // https://developer.apple.com/documentation/gamecontroller/gcgamepad
            // https://developer.apple.com/documentation/gamecontroller/gcextendedgamepad
            val microGamepad = controller.microGamepad
            val gamepad = controller.gamepad
            val extendedGamepad = controller.extendedGamepad

            if (microGamepad != null) {
                button(GameButton.LEFT, microGamepad.dpad.left)
                button(GameButton.RIGHT, microGamepad.dpad.right)
                button(GameButton.UP, microGamepad.dpad.up)
                button(GameButton.DOWN, microGamepad.dpad.down)
                button(GameButton.XBOX_A, microGamepad.buttonA)
                button(GameButton.XBOX_X, microGamepad.buttonX)
                button(GameButton.START, microGamepad.buttonMenu)
            }

            if (gamepad != null) {
                button(GameButton.XBOX_B, gamepad.buttonB)
                button(GameButton.XBOX_Y, gamepad.buttonY)
                button(GameButton.L1, gamepad.leftShoulder)
                button(GameButton.R1, gamepad.rightShoulder)
            }

            if (extendedGamepad != null) {
                button(GameButton.SYSTEM, extendedGamepad.buttonHome)
                button(GameButton.SELECT, extendedGamepad.buttonOptions)
                button(GameButton.L3, extendedGamepad.leftThumbstickButton)
                button(GameButton.R3, extendedGamepad.rightThumbstickButton)
                button(GameButton.L2, extendedGamepad.leftTrigger)
                button(GameButton.R2, extendedGamepad.rightTrigger)
                stick(GameButton.LX, GameButton.LY, extendedGamepad.leftThumbstick)
                stick(GameButton.RX, GameButton.RY, extendedGamepad.rightThumbstick)
            }

            info.connected = true
            info.name = controller.productCategory
            info.index = index // 0 - first player, 1 - second player...
            info.batteryLevel = controller.battery?.batteryLevel?.toDouble() ?: 1.0
            info.batteryStatus = when (controller.battery?.batteryState) {
                GCDeviceBatteryStateCharging -> GamepadInfo.BatteryStatus.CHARGING
                GCDeviceBatteryStateDischarging -> GamepadInfo.BatteryStatus.DISCHARGING
                GCDeviceBatteryStateFull -> GamepadInfo.BatteryStatus.FULL
                else -> GamepadInfo.BatteryStatus.UNKNOWN
            }

            gameWindow.dispatchGamepadUpdateAdd(info)
        }
        gameWindow.dispatchGamepadUpdateEnd()

        for (n in allControllers.indices) allControllers[n] = null
    }
}
