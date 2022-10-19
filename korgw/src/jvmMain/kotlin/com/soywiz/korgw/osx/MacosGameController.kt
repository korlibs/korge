package com.soywiz.korgw.osx

import com.soywiz.kmem.*
import com.soywiz.korev.*
import com.soywiz.korio.annotations.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.sun.jna.*
import kotlin.reflect.*

//fun main() {
//}

interface FrameworkInt : Library {

}

inline class GCControllerButtonInput(val id: Long) {
    val analog: Boolean get() = id.msgSendInt(sel_isAnalog) != 0
    val touched: Boolean get() = id.msgSendInt(sel_isTouched) != 0
    val pressed: Boolean get() = id.msgSendInt(sel_isPressed) != 0
    val value: Double get() = id.msgSendFloat(sel_value).toDouble()
    val sfSymbolsName: String get() = NSString(id.msgSend("sfSymbolsName")).toString()
    val localizedName: String get() = NSString(id.msgSend("localizedName")).toString()
    val unmappedLocalizedName: String get() = NSString(id.msgSend("unmappedLocalizedName")).toString()

    override fun toString(): String = "GCControllerButtonInput($localizedName, $touched, $pressed, $value)"
    val nice: String get() = value.niceStr(2)

    companion object {
        val sel_isAnalog = ObjcSel("isAnalog")
        val sel_isTouched = ObjcSel("isTouched")
        val sel_isPressed = ObjcSel("isPressed")
        val sel_value = ObjcSel("value")

        inline operator fun getValue(obj: ObjcRef, property: KProperty<*>): GCControllerButtonInput = GCControllerButtonInput(obj.id.msgSend(property.name))
    }
}

class GCControllerAxisInput(id: Long) : ObjcRef(id) {
    val value: Double get() = id.msgSendFloat("value").toDouble()
    companion object {
        inline operator fun getValue(obj: ObjcRef, property: KProperty<*>): GCControllerAxisInput = GCControllerAxisInput(obj.id.msgSend(property.name))
    }

    override fun toString(): String = value.niceStr(2)
}

class GCControllerDirectionPad(id: Long) : ObjcRef(id) {
    @Keep val right by GCControllerButtonInput
    @Keep val left by GCControllerButtonInput
    @Keep val up by GCControllerButtonInput
    @Keep val down by GCControllerButtonInput

    @Keep val xAxis by GCControllerAxisInput
    @Keep val yAxis by GCControllerAxisInput

    val x: Double get() = xAxis.value
    val y: Double get() = yAxis.value

    private val _point: Point = Point()
    val point: IPoint get() = _point.setTo(x, y)

    companion object {
        inline operator fun getValue(obj: ObjcRef, property: KProperty<*>): GCControllerDirectionPad = GCControllerDirectionPad(obj.id.msgSend(property.name))
    }

    override fun toString(): String = "DPad(${up.nice}, ${right.nice}, ${down.nice}, ${left.nice})"
}

open class GCMicroGamepad(id: Long) : ObjcRef(id) {
    @Keep val buttonA by GCControllerButtonInput
    @Keep val buttonX by GCControllerButtonInput
    @Keep val dpad by GCControllerDirectionPad
    @Keep val buttonMenu by GCControllerButtonInput

    companion object {
        inline operator fun getValue(obj: ObjcRef, property: KProperty<*>): GCMicroGamepad? =
            obj.id.msgSend(property.name).takeIf { it != 0L }?.let { GCMicroGamepad(it) }
    }
}

open class GCGamepad(id: Long) : GCMicroGamepad(id) {
    @Keep val leftShoulder by GCControllerButtonInput
    @Keep val rightShoulder by GCControllerButtonInput
    @Keep val buttonB by GCControllerButtonInput
    @Keep val buttonY by GCControllerButtonInput

    companion object {
        inline operator fun getValue(obj: ObjcRef, property: KProperty<*>): GCGamepad? =
            obj.id.msgSend(property.name).takeIf { it != 0L }?.let { GCGamepad(it) }
    }
}

class GCExtendedGamepad(id: Long) : GCGamepad(id) {
    @Keep val leftTrigger by GCControllerButtonInput
    @Keep val rightTrigger by GCControllerButtonInput
    @Keep val buttonOptions by GCControllerButtonInput
    @Keep val buttonHome by GCControllerButtonInput
    @Keep val leftThumbstick by GCControllerDirectionPad
    @Keep val rightThumbstick by GCControllerDirectionPad
    @Keep val leftThumbstickButton by GCControllerButtonInput
    @Keep val rightThumbstickButton by GCControllerButtonInput


    companion object {
        inline operator fun getValue(obj: ObjcRef, property: KProperty<*>): GCExtendedGamepad? =
            obj.id.msgSend(property.name).takeIf { it != 0L }?.let { GCExtendedGamepad(it) }
    }

    override fun toString(): String = "GCExtendedGamepad(dpad=$dpad, LR=[${leftThumbstick.point.niceStr(2)}, ${rightThumbstick.point.niceStr(2)}] [A=${buttonA.nice}, B=${buttonB.nice}, X=${buttonX.nice}, Y=${buttonY.nice}], L=[${leftShoulder.nice}, ${leftTrigger.nice}, ${leftThumbstickButton.nice}], R=[${rightShoulder.nice}, ${rightTrigger.nice}, ${rightThumbstickButton.nice}], SYS=[${buttonMenu.nice}, ${buttonOptions.nice}, ${buttonHome.nice}])"
}

class GCController(id: Long) : ObjcRef(id) {
    val extendedGamepad: GCExtendedGamepad? by GCExtendedGamepad
    val gamepad: GCGamepad? by GCGamepad
    val microGamepad: GCMicroGamepad? by GCMicroGamepad

    companion object {
        fun controllers(): NSArray = NSArray(NSClass("GCController").msgSend("controllers"))
    }
}

class NSArray(val id: Long) : AbstractList<Long>() {
    val count: Int get() = id.msgSendInt("count")
    override val size: Int get() = count
    override operator fun get(index: Int): Long = id.msgSend("objectAtIndex:", index)
    override fun toString(): String = "NSArray(${toList()})"
}

class MacosGameController {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val gamepad = MacosGamepadEventAdapter()
            val events = EventDispatcher.Mixin()
            events.addEventListener<GamePadUpdateEvent> { print("$it\r") }
            events.addEventListener<GamePadConnectionEvent> { println(it) }
            while (true) {
                gamepad.updateGamepads(events)
                Thread.sleep(10L)
            }
        }
    }
}

internal class MacosGamepadEventAdapter {
    val lib by lazy { Native.load("/System/Library/Frameworks/GameController.framework/Versions/A/GameController", FrameworkInt::class.java) }

    private val gamepadsConnected = BooleanArray(MAX_GAMEPADS)
    private val gamePadUpdateEvent = GamePadUpdateEvent()
    private val gamePadConnectionEvent = GamePadConnectionEvent()

    fun updateGamepads(dispatcher: EventDispatcher) {
        try {
            lib
            val controllers = GCController.controllers().toList().map { GCController(it) }
            var connectedCount = 0

            for (n in 0 until MAX_GAMEPADS) {
                val ctrl = controllers.getOrNull(n)
                val prevConnected = gamepadsConnected[n]
                val connected = ctrl != null
                val gamepad = gamePadUpdateEvent.gamepads[n]

                gamepad.connected = connected
                if (connected && ctrl != null) {
                    gamepad.mapping = StandardGamepadMapping
                    //println("ctrl=$ctrl")
                    var buttons = 0

                    val ex: GCExtendedGamepad? = ctrl.extendedGamepad
                    val base: GCGamepad? = ctrl.gamepad ?: ctrl.extendedGamepad
                    val micro: GCMicroGamepad? = ctrl.microGamepad ?: ctrl.gamepad ?: ctrl.extendedGamepad
                    if (micro != null) {
                        buttons = buttons
                            .insert(micro.dpad.left.pressed, GameButton.LEFT.index)
                            .insert(micro.dpad.right.pressed, GameButton.RIGHT.index)
                            .insert(micro.dpad.up.pressed, GameButton.UP.index)
                            .insert(micro.dpad.down.pressed, GameButton.DOWN.index)
                            .insert(micro.buttonA.pressed, GameButton.XBOX_A.index)
                            .insert(micro.buttonX.pressed, GameButton.XBOX_X.index)
                            .insert(micro.buttonMenu.pressed, GameButton.START.index)
                    }
                    if (base != null) {
                        buttons = buttons
                            .insert(base.buttonB.pressed, GameButton.XBOX_B.index)
                            .insert(base.buttonY.pressed, GameButton.XBOX_Y.index)
                            .insert(base.leftShoulder.pressed, GameButton.L1.index)
                            .insert(base.rightShoulder.pressed, GameButton.R1.index)
                    }
                    if (ex != null) {
                        buttons = buttons
                            .insert(ex.buttonHome.pressed, GameButton.SYSTEM.index)
                            .insert(ex.buttonOptions.pressed, GameButton.SELECT.index)
                            .insert(ex.leftThumbstickButton.pressed, GameButton.L3.index)
                            .insert(ex.rightThumbstickButton.pressed, GameButton.R3.index)
                        gamepad.rawButtonsPressure[GameButton.L2.index] = ex.leftTrigger.value
                        gamepad.rawButtonsPressure[GameButton.R2.index] = ex.rightTrigger.value
                        gamepad.rawButtonsPressure[GameButton.L3.index] = ex.leftThumbstickButton.value
                        gamepad.rawButtonsPressure[GameButton.R3.index] = ex.rightThumbstickButton.value
                        gamepad.rawAxes[0] = ex.leftThumbstick.x
                        gamepad.rawAxes[1] = ex.leftThumbstick.y
                        gamepad.rawAxes[2] = ex.rightThumbstick.x
                        gamepad.rawAxes[3] = ex.rightThumbstick.y
                    }
                    gamepad.rawButtonsPressed = buttons

                    connectedCount++
                }
                if (prevConnected != connected) {
                    if (connected && ctrl != null) {
                        gamepad.name = "Standard Gamepad"
                    }

                    //println("n=$n, prevConnected=$prevConnected, connected=$connected")

                    gamepadsConnected[n] = connected
                    dispatcher.dispatch(gamePadConnectionEvent.also {
                        it.gamepad = n
                        it.type = if (connected) GamePadConnectionEvent.Type.CONNECTED else GamePadConnectionEvent.Type.DISCONNECTED
                    })
                }
            }

            gamePadUpdateEvent.gamepadsLength = connectedCount

            if (connectedCount > 0) {
                dispatcher.dispatch(gamePadUpdateEvent)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    companion object {
        const val MAX_GAMEPADS = 4
    }
}
