package com.soywiz.korgw

import com.soywiz.korev.*
import com.soywiz.korma.geom.*
import platform.GameController.*

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

                when {
                    extendedGamepad != null -> {
                        button(GameButton.SYSTEM, extendedGamepad.buttonHome)
                        button(GameButton.START, extendedGamepad.buttonMenu)
                        button(GameButton.SELECT, extendedGamepad.buttonOptions)
                        button(GameButton.UP, extendedGamepad.dpad.up)
                        button(GameButton.DOWN, extendedGamepad.dpad.down)
                        button(GameButton.LEFT, extendedGamepad.dpad.left)
                        button(GameButton.RIGHT, extendedGamepad.dpad.right)
                        button(GameButton.GENERIC_BUTTON_UP, extendedGamepad.buttonY)
                        button(GameButton.GENERIC_BUTTON_RIGHT, extendedGamepad.buttonB)
                        button(GameButton.GENERIC_BUTTON_DOWN, extendedGamepad.buttonA)
                        button(GameButton.GENERIC_BUTTON_LEFT, extendedGamepad.buttonX)
                        button(GameButton.L1, extendedGamepad.leftShoulder)
                        button(GameButton.L2, extendedGamepad.leftTrigger)
                        button(GameButton.R1, extendedGamepad.rightShoulder)
                        button(GameButton.R2, extendedGamepad.rightTrigger)
                        button(GameButton.L3, extendedGamepad.leftThumbstickButton)
                        button(GameButton.R3, extendedGamepad.rightThumbstickButton)
                        stick(leftStick, extendedGamepad.leftThumbstick)
                        stick(rightStick, extendedGamepad.rightThumbstick)
                    }
                    gamepad != null -> {
                        button(GameButton.UP, gamepad.dpad.up)
                        button(GameButton.DOWN, gamepad.dpad.down)
                        button(GameButton.LEFT, gamepad.dpad.left)
                        button(GameButton.RIGHT, gamepad.dpad.right)
                        button(GameButton.GENERIC_BUTTON_UP, gamepad.buttonY)
                        button(GameButton.GENERIC_BUTTON_RIGHT, gamepad.buttonB)
                        button(GameButton.GENERIC_BUTTON_DOWN, gamepad.buttonA)
                        button(GameButton.GENERIC_BUTTON_LEFT, gamepad.buttonX)
                        button(GameButton.L1, gamepad.leftShoulder)
                        button(GameButton.R1, gamepad.rightShoulder)
                    }
                    microGamepad != null -> {
                        button(GameButton.START, microGamepad.buttonMenu)
                        button(GameButton.UP, microGamepad.dpad.up)
                        button(GameButton.DOWN, microGamepad.dpad.down)
                        button(GameButton.LEFT, microGamepad.dpad.left)
                        button(GameButton.RIGHT, microGamepad.dpad.right)
                        button(GameButton.GENERIC_BUTTON_DOWN, microGamepad.buttonA)
                        button(GameButton.GENERIC_BUTTON_LEFT, microGamepad.buttonX)
                        stick(leftStick, microGamepad.dpad)
                    }
                }

                gameWindow.dispatchGamepadUpdateAdd(
                    leftStick, rightStick,
                    buttonMask,
                    mapping,
                    controller.vendorName,
                    controller.battery?.batteryLevel?.toDouble() ?: 1.0
                )
            }
            gameWindow.dispatchGamepadUpdateEnd()
        }
    }
}
