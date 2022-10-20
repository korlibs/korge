package com.soywiz.korgw

import com.soywiz.korev.*
import com.soywiz.korma.geom.*
import platform.CoreHaptics.*
import platform.GameController.*
import kotlin.native.internal.*

class DarwinGamePad {
    val knownControllers = mutableSetOf<GCController>()

    @Suppress("RemoveRedundantCallsOfConversionMethods")
    fun updateGamepads(gameWindow: GameWindow) {
        val controllers = GCController.controllers().filterIsInstance<GCController>().sortedBy { it.playerIndex.toInt() }
        if (controllers.isNotEmpty() || knownControllers.isNotEmpty()) {
            val addedControllers = controllers - knownControllers
            val removedControllers = knownControllers - controllers
            knownControllers.clear()
            knownControllers.addAll(controllers)
            for (controller in addedControllers) {
                gameWindow.dispatchGamepadConnectionEvent(GamePadConnectionEvent.Type.CONNECTED, controller.playerIndex.toInt())
                //println("supportedLocalities: " + controller.haptics?.supportedLocalities)
                //val engine: CHHapticEngine? = controller.haptics?.createEngineWithLocality("Default") as? CHHapticEngine?
                //val pattern = CHHapticPattern(
                //    listOf(CHHapticEvent(CHHapticEventTypeHapticTransient))
                //)
                //pattern.
                //engine!!.createAdvancedPlayerWithPattern(pattern)
            }
            for (controller in removedControllers) {
                gameWindow.dispatchGamepadConnectionEvent(
                    GamePadConnectionEvent.Type.DISCONNECTED,
                    controller.playerIndex.toInt()
                )
            }
            gameWindow.dispatchGamepadUpdateStart()
            val mapping = StandardGamepadMapping
            for ((index, controller) in controllers.withIndex()) {
                var buttonMask = 0
                val leftStick = Point()
                val rightStick = Point()
                fun button(button: GameButton, pressed: Boolean) {
                    if (pressed) buttonMask = buttonMask or (1 shl button.ordinal)
                }

                fun button(button: GameButton, gcbutton: GCControllerButtonInput?) {
                    if (gcbutton != null) {
                        button(button, gcbutton.pressed)
                    }
                }
                fun stick(stick: Point, pad: GCControllerDirectionPad) {
                    stick.setTo(pad.xAxis.value, pad.yAxis.value)
                }

                // https://developer.apple.com/documentation/gamecontroller/gcmicrogamepad
                // https://developer.apple.com/documentation/gamecontroller/gcgamepad
                // https://developer.apple.com/documentation/gamecontroller/gcextendedgamepad
                val microGamepad = controller.microGamepad
                val gamepad = controller.gamepad
                val extendedGamepad = controller.extendedGamepad

                if (microGamepad != null) {
                    button(GameButton.START, microGamepad.buttonMenu)
                    button(GameButton.UP, microGamepad.dpad.up)
                    button(GameButton.DOWN, microGamepad.dpad.down)
                    button(GameButton.LEFT, microGamepad.dpad.left)
                    button(GameButton.RIGHT, microGamepad.dpad.right)
                    button(GameButton.XBOX_A, microGamepad.buttonA)
                    button(GameButton.XBOX_X, microGamepad.buttonX)
                    stick(leftStick, microGamepad.dpad)
                }

                if (gamepad != null) {
                    button(GameButton.XBOX_Y, gamepad.buttonY)
                    button(GameButton.XBOX_B, gamepad.buttonB)
                    button(GameButton.L1, gamepad.leftShoulder)
                    button(GameButton.R1, gamepad.rightShoulder)
                }

                if (extendedGamepad != null) {
                    button(GameButton.SYSTEM, extendedGamepad.buttonHome)
                    button(GameButton.SELECT, extendedGamepad.buttonOptions)
                    button(GameButton.R1, extendedGamepad.rightShoulder)
                    button(GameButton.R2, extendedGamepad.rightTrigger)
                    button(GameButton.L3, extendedGamepad.leftThumbstickButton)
                    button(GameButton.R3, extendedGamepad.rightThumbstickButton)
                    stick(leftStick, extendedGamepad.leftThumbstick)
                    stick(rightStick, extendedGamepad.rightThumbstick)
                }

                gameWindow.dispatchGamepadUpdateAdd(
                    leftStick, rightStick,
                    buttonMask,
                    mapping,
                    controller.productCategory,
                    controller.battery?.batteryLevel?.toDouble() ?: 1.0,
                    controller.vendorName,
                    controller.playerIndex.toInt(), // -1 - unknown, 0 - first player, 1 - second player...
                    when (controller.battery?.batteryState) {
                        GCDeviceBatteryStateCharging -> GamepadInfo.BatteryStatus.CHARGING
                        GCDeviceBatteryStateDischarging -> GamepadInfo.BatteryStatus.DISCHARGING
                        GCDeviceBatteryStateFull -> GamepadInfo.BatteryStatus.FULL
                        else -> GamepadInfo.BatteryStatus.UNKNOWN
                    }
                )
            }
            gameWindow.dispatchGamepadUpdateEnd()
        }
    }
}
