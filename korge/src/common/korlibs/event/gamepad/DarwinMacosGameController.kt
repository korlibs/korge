package korlibs.event.gamepad

import korlibs.annotations.*
import korlibs.datastructure.iterators.*
import korlibs.event.*
import korlibs.ffi.*
import korlibs.ffi.osx.*
import korlibs.math.geom.*
import korlibs.number.*
import korlibs.render.*
import kotlin.reflect.*

@PublishedApi
internal inline fun <T> getValueOrNull(obj: NSObject, property: KProperty<*>, gen: (Long) -> T): T? =
    obj.msgSend(property.name).takeIf { it != 0L }?.let { gen(it) }

@PublishedApi
internal inline fun <T> getValue(obj: NSObject, property: KProperty<*>, gen: (Long) -> T): T =
    obj.msgSend(property.name).let { gen(it) }

internal inline class GCControllerButtonInput(val id: ObjcRef) {
    val analog: Boolean get() = id.msgSendInt(sel_isAnalog) != 0
    val touched: Boolean get() = id.msgSendInt(sel_isTouched) != 0
    val pressed: Boolean get() = id.msgSendInt(sel_isPressed) != 0
    val value: Float get() = id.msgSendFloat(sel_value).toFloat()
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

        inline operator fun getValue(obj: NSObject, property: KProperty<*>): GCControllerButtonInput =
            getValue(obj, property) { GCControllerButtonInput(ObjcRef(it)) }
    }
}

class GCControllerAxisInput(id: ObjcRef) : NSObject(id) {
    val value: Float get() = ref.msgSendFloat("value")
    companion object {
        inline operator fun getValue(obj: NSObject, property: KProperty<*>): GCControllerAxisInput = GCControllerAxisInput(obj.msgSendRef(property.name))
    }

    override fun toString(): String = value.niceStr(2)
}

@KeepNames
internal class GCControllerDirectionPad(id: ObjcRef) : NSObject(id) {
    val right by GCControllerButtonInput
    val left by GCControllerButtonInput
    val up by GCControllerButtonInput
    val down by GCControllerButtonInput

    val xAxis by GCControllerAxisInput
    val yAxis by GCControllerAxisInput

    val x: Float get() = xAxis.value
    val y: Float get() = yAxis.value

    private var _point: Point = Point.ZERO
    val point: Point get() = _point

    companion object {
        inline operator fun getValue(obj: NSObject, property: KProperty<*>): GCControllerDirectionPad =
            getValue(obj, property) { GCControllerDirectionPad(ObjcRef(it)) }
    }

    override fun toString(): String = "DPad(${up.nice}, ${right.nice}, ${down.nice}, ${left.nice})"
}

@KeepNames
internal open class GCMicroGamepad(id: ObjcRef) : NSObject(id) {
    val buttonA by GCControllerButtonInput
    val buttonX by GCControllerButtonInput
    val dpad by GCControllerDirectionPad
    val buttonMenu by GCControllerButtonInput

    companion object {
        inline operator fun getValue(obj: NSObject, property: KProperty<*>): GCMicroGamepad? =
            getValueOrNull(obj, property) { GCMicroGamepad(ObjcRef(it)) }
    }
}

@KeepNames
internal open class GCGamepad(id: ObjcRef) : GCMicroGamepad(id) {
    val leftShoulder by GCControllerButtonInput
    val rightShoulder by GCControllerButtonInput
    val buttonB by GCControllerButtonInput
    val buttonY by GCControllerButtonInput

    companion object {
        inline operator fun getValue(obj: NSObject, property: KProperty<*>): GCGamepad? =
            getValueOrNull(obj, property) { GCGamepad(ObjcRef(it)) }
    }
}

@KeepNames
internal class GCExtendedGamepad(id: ObjcRef) : GCGamepad(id) {
    val leftTrigger by GCControllerButtonInput
    val rightTrigger by GCControllerButtonInput
    val buttonOptions by GCControllerButtonInput
    val buttonHome by GCControllerButtonInput
    val leftThumbstick by GCControllerDirectionPad
    val rightThumbstick by GCControllerDirectionPad
    val leftThumbstickButton by GCControllerButtonInput
    val rightThumbstickButton by GCControllerButtonInput

    companion object {
        inline operator fun getValue(obj: NSObject, property: KProperty<*>): GCExtendedGamepad? =
            getValueOrNull(obj, property) { GCExtendedGamepad(ObjcRef(it)) }
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

class GCDeviceBattery(val id: ObjcRef) {
    val batteryLevel: Float get() = id.msgSendFloat("batteryLevel")
    val batteryState: Int get() = id.msgSendInt("batteryState")

    companion object {
        inline operator fun getValue(obj: NSObject, property: KProperty<*>): GCDeviceBattery? =
            getValueOrNull(obj, property) { GCDeviceBattery(ObjcRef(it)) }
    }
}

/**
 * https://developer.apple.com/documentation/gamecontroller/gccontroller?language=objc
 */
internal class GCController(id: ObjcRef) : NSObject(id) {
    val isAttachedToDevice: Boolean get() = ref.msgSendInt("isAttachedToDevice") != 0
    val playerIndex: Int get() = ref.msgSendInt("playerIndex")
    //val physicalInputProfile: GCPhysicalInputProfile by GCPhysicalInputProfile
    val extendedGamepad: GCExtendedGamepad? by GCExtendedGamepad
    val gamepad: GCGamepad? by GCGamepad
    val microGamepad: GCMicroGamepad? by GCMicroGamepad

    val battery: GCDeviceBattery? by GCDeviceBattery

    val vendorName: String by lazy { NSString(id.msgSend("vendorName")).toString() }

    /** https://developer.apple.com/documentation/gamecontroller/gcdevice/product_category_constants?language=objc */
    val productCategory: String by lazy { NSString(id.msgSend("productCategory")).toString() }

    companion object {
        fun controllers(): NSArray = NSArray(NSClass("GCController").msgSendRef("controllers"))
    }
}

class NSArray(val id: ObjcRef) : AbstractList<Long>() {
    val count: Int get() = id.msgSendInt("count")
    override val size: Int get() = count
    override operator fun get(index: Int): Long = id.msgSend("objectAtIndex:", index)
    override fun toString(): String = "NSArray(${toList()})"
}

//@JvmStatic
//fun main(args: Array<String>) {
//    val gamepad = MacosGamepadEventAdapter()
//    val events = GameWindow()
//    events.onEvent(GamePadUpdateEvent) { print("$it\r") }
//    events.onEvents(*GamePadConnectionEvent.Type.ALL) { println(it) }
//    while (true) {
//        gamepad.updateGamepads(events)
//        NativeThread.sleep(10.milliseconds)
//    }
//}

internal class MacosGamepadEventAdapter {
    internal object FrameworkInt : FFILib("/System/Library/Frameworks/GameController.framework/Versions/A/GameController")

    val lib by lazy { FrameworkInt }

    private fun GamepadInfo.set(button: GameButton, value: Float, deadRange: Boolean = false) { rawButtons[button.index] = GamepadInfo.withoutDeadRange(value.toFloat(), apply = deadRange) }
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
                .map { GCController(ObjcRef(it)) }
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
                gamepad.batteryLevel = ctrl.battery?.batteryLevel?.toFloat() ?: 1f
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
