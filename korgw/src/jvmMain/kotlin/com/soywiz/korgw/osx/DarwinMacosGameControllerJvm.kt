package com.soywiz.korgw.osx

import com.soywiz.kds.iterators.*
import com.soywiz.kmem.dyn.osx.*
import com.soywiz.korev.*
import com.soywiz.korgw.*
import com.soywiz.korio.annotations.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.sun.jna.*
import kotlin.reflect.*

//fun main() {
//}

interface FrameworkInt : Library {

}

@PublishedApi
internal inline fun <T> getValueOrNull(obj: ObjcRef, property: KProperty<*>, gen: (Long) -> T): T? =
    obj.id.msgSend(property.name).takeIf { it != 0L }?.let { gen(it) }

@PublishedApi
internal inline fun <T> getValue(obj: ObjcRef, property: KProperty<*>, gen: (Long) -> T): T =
    obj.id.msgSend(property.name).let { gen(it) }

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

        inline operator fun getValue(obj: ObjcRef, property: KProperty<*>): GCControllerButtonInput =
            getValue(obj, property) { GCControllerButtonInput(it) }
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

    private val _point: MPoint = MPoint()
    val point: MPoint get() = _point.setTo(x, y)

    companion object {
        inline operator fun getValue(obj: ObjcRef, property: KProperty<*>): GCControllerDirectionPad =
            getValue(obj, property) { GCControllerDirectionPad(it) }
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
            getValueOrNull(obj, property) { GCMicroGamepad(it) }
    }
}

open class GCGamepad(id: Long) : GCMicroGamepad(id) {
    @Keep val leftShoulder by GCControllerButtonInput
    @Keep val rightShoulder by GCControllerButtonInput
    @Keep val buttonB by GCControllerButtonInput
    @Keep val buttonY by GCControllerButtonInput

    companion object {
        inline operator fun getValue(obj: ObjcRef, property: KProperty<*>): GCGamepad? =
            getValueOrNull(obj, property) { GCGamepad(it) }
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
            getValueOrNull(obj, property) { GCExtendedGamepad(it) }
    }

    override fun toString(): String = "GCExtendedGamepad(dpad=$dpad, LR=[${leftThumbstick.point.niceStr(2)}, ${rightThumbstick.point.niceStr(2)}] [A=${buttonA.nice}, B=${buttonB.nice}, X=${buttonX.nice}, Y=${buttonY.nice}], L=[${leftShoulder.nice}, ${leftTrigger.nice}, ${leftThumbstickButton.nice}], R=[${rightShoulder.nice}, ${rightTrigger.nice}, ${rightThumbstickButton.nice}], SYS=[${buttonMenu.nice}, ${buttonOptions.nice}, ${buttonHome.nice}])"
}

//class GCDevice(id: Long) : ObjcRef(id) {
//    val vendorName: String by lazy { NSString(id.msgSend("vendorName")).toString() }
//    val productCategory: String by lazy { NSString(id.msgSend("productCategory")).toString() }
//    companion object {
//        inline operator fun getValue(obj: ObjcRef, property: KProperty<*>): GCDevice =
//            getValue(obj, property) { GCDevice(it) }
//    }
//}

//class GCPhysicalInputProfile(id: Long) : ObjcRef(id) {
//    val device: GCDevice by GCDevice
//   companion object {
//       inline operator fun getValue(obj: ObjcRef, property: KProperty<*>): GCPhysicalInputProfile =
//           getValue(obj, property) { GCPhysicalInputProfile(it) }
//   }
//}

class GCDeviceBattery(id: Long) : ObjcRef(id) {
    val batteryLevel: Float get() = id.msgSendFloat("batteryLevel")
    val batteryState: Int get() = id.msgSendInt("batteryState")

    companion object {
        inline operator fun getValue(obj: ObjcRef, property: KProperty<*>): GCDeviceBattery? =
            getValueOrNull(obj, property) { GCDeviceBattery(it) }
    }
}

/**
 * https://developer.apple.com/documentation/gamecontroller/gccontroller?language=objc
 */
class GCController(id: Long) : ObjcRef(id) {
    val isAttachedToDevice: Boolean get() = id.msgSendInt("isAttachedToDevice") != 0
    val playerIndex: Int get() = id.msgSendInt("playerIndex")
    //val physicalInputProfile: GCPhysicalInputProfile by GCPhysicalInputProfile
    val extendedGamepad: GCExtendedGamepad? by GCExtendedGamepad
    val gamepad: GCGamepad? by GCGamepad
    val microGamepad: GCMicroGamepad? by GCMicroGamepad

    val battery: GCDeviceBattery? by GCDeviceBattery

    val vendorName: String by lazy { NSString(id.msgSend("vendorName")).toString() }

    /** https://developer.apple.com/documentation/gamecontroller/gcdevice/product_category_constants?language=objc */
    val productCategory: String by lazy { NSString(id.msgSend("productCategory")).toString() }

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
            val events = GameWindow()
            events.onEvent(GamePadUpdateEvent) { print("$it\r") }
            events.onEvents(*GamePadConnectionEvent.Type.ALL) { println(it) }
            while (true) {
                gamepad.updateGamepads(events)
                Thread.sleep(10L)
            }
        }
    }
}

internal class MacosGamepadEventAdapter {
    val lib by lazy { Native.load("/System/Library/Frameworks/GameController.framework/Versions/A/GameController", FrameworkInt::class.java) }

    private fun GamepadInfo.set(button: GameButton, value: Double, deadRange: Boolean = false) { rawButtons[button.index] = GamepadInfo.withoutDeadRange(value.toFloat(), apply = deadRange) }
    private fun GamepadInfo.set(button: GameButton, cbutton: GCControllerButtonInput, deadRange: Boolean = false) {
        set(button, cbutton.value, deadRange)
    }

    private val allControllers = Array<GCController?>(GamepadInfo.MAX_CONTROLLERS) { null }
    private val gamepad = GamepadInfo()

    fun updateGamepads(gameWindow: GameWindow) {
        try {
            lib
            for (n in allControllers.indices) allControllers[n] = null
            GCController.controllers()
                //.sortedBy { it }
                .map { GCController(it) }
                .fastForEachWithIndex { index, it ->
                    //println("index=$index")
                    if (index in allControllers.indices) allControllers[index] = it
                }

            gameWindow.dispatchGamepadUpdateStart()
            for (index in allControllers.indices) {
                val ctrl = allControllers[index] ?: continue
                val ex: GCExtendedGamepad? = ctrl.extendedGamepad
                val base: GCGamepad? = ctrl.gamepad ?: ctrl.extendedGamepad
                val micro: GCMicroGamepad? = ctrl.microGamepad ?: ctrl.gamepad ?: ctrl.extendedGamepad
                if (micro != null) {
                    gamepad.set(GameButton.LEFT, micro.dpad.left)
                    gamepad.set(GameButton.RIGHT, micro.dpad.right)
                    gamepad.set(GameButton.UP, micro.dpad.up)
                    gamepad.set(GameButton.DOWN, micro.dpad.down)
                    gamepad.set(GameButton.XBOX_A, micro.buttonA)
                    gamepad.set(GameButton.XBOX_X, micro.buttonX)
                    gamepad.set(GameButton.START, micro.buttonMenu)
                }
                if (base != null) {
                    gamepad.set(GameButton.XBOX_B, base.buttonB)
                    gamepad.set(GameButton.XBOX_Y, base.buttonY)
                    gamepad.set(GameButton.L1, base.leftShoulder)
                    gamepad.set(GameButton.R1, base.rightShoulder)
                }
                if (ex != null) {
                    gamepad.set(GameButton.SYSTEM, ex.buttonHome)
                    gamepad.set(GameButton.SELECT, ex.buttonOptions)
                    gamepad.set(GameButton.L3, ex.leftThumbstickButton)
                    gamepad.set(GameButton.R3, ex.rightThumbstickButton)
                    gamepad.set(GameButton.L2, ex.leftTrigger)
                    gamepad.set(GameButton.R2, ex.rightTrigger)
                    gamepad.set(GameButton.LX, ex.leftThumbstick.x, deadRange = true)
                    gamepad.set(GameButton.LY, ex.leftThumbstick.y, deadRange = true)
                    gamepad.set(GameButton.RX, ex.rightThumbstick.x, deadRange = true)
                    gamepad.set(GameButton.RY, ex.rightThumbstick.y, deadRange = true)
                }
                gamepad.name = ctrl.vendorName
                gamepad.batteryLevel = ctrl.battery?.batteryLevel?.toDouble() ?: 1.0
                //println("ctrl.battery?.batteryState=${ctrl.battery?.batteryState}")
                gamepad.batteryStatus = when (ctrl.battery?.batteryState) {
                    0 -> GamepadInfo.BatteryStatus.DISCHARGING
                    1 -> GamepadInfo.BatteryStatus.CHARGING
                    2 -> GamepadInfo.BatteryStatus.FULL
                    else -> GamepadInfo.BatteryStatus.UNKNOWN
                }
                gameWindow.dispatchGamepadUpdateAdd(gamepad)
            }
            gameWindow.dispatchGamepadUpdateEnd()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
