package korlibs.event

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.memory.*
import korlibs.io.util.*
import korlibs.math.geom.*
import kotlin.jvm.*
import kotlin.math.*

enum class GameStick(val id: Int) {
    LEFT(0), RIGHT(1);

    companion object {
        val STICKS = values()
    }
}

/**
 * ```
 *             ____________________________              __
 *            / [__L2__]          [__R2__] \               |
 *           / [__ L1 __]        [__ R1 __] \              | Front Triggers
 *        __/________________________________\__         __|
 *       /                                  _   \          |
 *      /      /\           ___            (N)   \         |
 *     /       ||      __  |SYS| __     _       _ \        | Main Pad
 *    |    <===DP===> |SE|      |ST|   (W) -|- (E) |       |
 *     \       ||    ___          ___       _     /        |
 *     /\      \/   /   \        /   \     (S)   /\      __|
 *    /  \________ | LS  | ____ |  RS | ________/  \       |
 *   |         /  \ \___/ /    \ \___/ /  \         |      | Control Sticks
 *   |        /    \_____/      \_____/    \        |    __|
 *   |       /                              \       |
 *    \_____/                                \_____/
 *
 *        |________|______|    |______|___________|
 *          D-Pad    Left       Right   Action Pad
 *         UP/DOWN   Stick      Stick
 *        LEFT/RIGHT LX/LY/L3   RX/RY/R3
 *                 |_____________|
 *                     Menu Pad
 * ```
 */
enum class GameButton {
    /** D-PAD LEFT */ LEFT, /** D-PAD RIGHT */ RIGHT,
    /** D-PAD UP */ UP, /** D-PAD DOWN */ DOWN,
    /** XBox: A, Playstation: Cross */ BUTTON_SOUTH,
    /** XBox: B, Playstation: Circle */ BUTTON_EAST,
    /** XBox: X, Playstation: Square */ BUTTON_WEST,
    /** XBox: Y, Playstation: Triangle */ BUTTON_NORTH,
    /** SELECT OR BACK */ SELECT,
    /** START OR FORWARD */ START,
    /** SYSTEM OR MENU */ SYSTEM,
    /** Left shoulder */ L1, /** Right shoulder */  R1,
    /** Left trigger (pressure 0.0-1.0) */ L2, /** Right trigger (pressure 0.0-1.0) */  R2,
    /** Left thumbstick */ L3, /** Right thumbstick */ R3,
    /** Left stick X: -1=left, +1=right */ LX, /** Left stick Y: -1=down, +1=up */ LY,
    /** Right stick X: -1=left, +1=right */ RX, /** Right stick Y: -1=down, +1=up */ RY,
    /** Generic button 4 */ BUTTON4,
    /** Generic button 5 */ BUTTON5,
    /** Generic button 6 */ BUTTON6,
    /** Generic button 7 */ BUTTON7,
    /** Generic button 8 */ BUTTON8,
    /** Record button */ RECORD,
    /** Internal use DPAD-X */ DPADX,
    /** Internal use DPAD-Y */ DPADY
    ;

    val index: Int get() = ordinal
    val bitMask: Int get() = 1 shl ordinal

    val isXbox: Boolean get() = name.contains("xbox", ignoreCase = true) || name.contains("microsoft", ignoreCase = true) || name.contains("xinput", ignoreCase = true)
    val isPlaystation: Boolean get() = name.contains("dualsense", ignoreCase = true) || name.contains("dualshock", ignoreCase = true) || name.contains("sony", ignoreCase = true)

    companion object {
        val BUTTONS = values()
        const val MAX = 32

        val LEFT_SHOULDER get() = L1
        val RIGHT_SHOULDER get() = R1

        val LEFT_TRIGGER get() = L2
        val RIGHT_TRIGGER get() = R2

        val LEFT_THUMB get() = L3
        val RIGHT_THUMB get() = R3

        val BACK get() = SELECT
        val FORWARD get() = START

        val XBOX_A get() = BUTTON_SOUTH
        val XBOX_B get() = BUTTON_EAST
        val XBOX_X get() = BUTTON_WEST
        val XBOX_Y get() = BUTTON_NORTH

        val PS_CROSS get() = BUTTON_SOUTH
        val PS_CIRCLE get() = BUTTON_EAST
        val PS_SQUARE get() = BUTTON_WEST
        val PS_TRIANGLE get() = BUTTON_NORTH
    }
}

class GamepadInfoEmitter(val dispatcher: EventListener) {
    private val gamepadPrevConnected = BooleanArray(GamepadInfo.MAX_CONTROLLERS)
    private val gamePadUpdateEvent = GamePadUpdateEvent()
    private val gamePadConnectionEvent = GamePadConnectionEvent()
    private val tempInts = IntArrayList()

    fun dispatchGamepadUpdateStart() {
        gamePadUpdateEvent.gamepads.fastForEach { it.connected = false }
        gamePadUpdateEvent.gamepadsLength = 0
    }
    fun dispatchGamepadUpdateAdd(info: GamepadInfo) {
        val index = gamePadUpdateEvent.gamepadsLength++
        val pad = gamePadUpdateEvent.gamepads[index]
        pad.copyFrom(info)
        pad.connected = true
        pad.index = index
    }
    /**
     * Triggers an update envent and potential CONNECTED/DISCONNECTED events.
     *
     * Returns a list of disconnected gamepads.
     */
    fun dispatchGamepadUpdateEnd(out: IntArrayList = tempInts): IntArrayList {
        out.clear()
        gamePadUpdateEvent.gamepads.fastForEach {
            if (gamepadPrevConnected[it.index] != it.connected) {
                gamepadPrevConnected[it.index] = it.connected
                out.add(it.index)
                // Clear stuff
                if (!it.connected) {
                    it.name = null
                }
                dispatcher.dispatch(gamePadConnectionEvent.apply {
                    this.type = GamePadConnectionEvent.Type.fromConnected(it.connected)
                    this.gamepad = it.index
                })
            }
        }
        dispatcher.dispatch(gamePadUpdateEvent)
        return out
    }
}

// http://blog.teamtreehouse.com/wp-content/uploads/2014/03/standardgamepad.png
class GamepadInfo(
    var index: Int = 0,
    var connected: Boolean = false,
    var name: String? = null,
    var rawButtons: FloatArray = FloatArray(GameButton.MAX),
    var batteryLevel: Double = 1.0,
    var name2: String = DEFAULT_NAME2,
    var batteryStatus: BatteryStatus = BatteryStatus.UNKNOWN,
) {
    enum class BatteryStatus { CHARGING, DISCHARGING, FULL, UNKNOWN }

    companion object {
        val DEFAULT_NAME2 = "Wireless Controller"
        const val MAX_CONTROLLERS = 4
        //const val MAX_CONTROLLERS = 8

        const val DEAD_RANGE = 0.06f

        fun withoutDeadRange(value: Float, margin: Float = DEAD_RANGE, apply: Boolean = true): Float {
            if (apply && value.absoluteValue < margin) return 0f
            return value
        }
        fun withoutDeadRange(value: Double, margin: Double = DEAD_RANGE.toDouble(), apply: Boolean = true): Double {
            if (apply && value.absoluteValue < margin) return 0.0
            return value
        }
    }

    val fullName: String get() = "${name ?: "unknown"} - $name2"

    fun copyFrom(that: GamepadInfo) {
        this.index = that.index
        this.name = that.name
        this.name2 = that.name2
        this.connected = that.connected
        this.batteryLevel = that.batteryLevel
        this.batteryStatus = that.batteryStatus
        arraycopy(that.rawButtons, 0, this.rawButtons, 0, min(this.rawButtons.size, that.rawButtons.size))
    }

    val up: Boolean get() = this[GameButton.UP] != 0.0
    val down: Boolean get() = this[GameButton.DOWN] != 0.0
    val left: Boolean get() = this[GameButton.LEFT] != 0.0
    val right: Boolean get() = this[GameButton.RIGHT] != 0.0
    val start: Boolean get() = this[GameButton.START] != 0.0
    val select: Boolean get() = this[GameButton.SELECT] != 0.0
    val system: Boolean get() = this[GameButton.SYSTEM] != 0.0

    val north: Boolean get() = this[GameButton.BUTTON_NORTH] != 0.0
    val west: Boolean get() = this[GameButton.BUTTON_WEST] != 0.0
    val east: Boolean get() = this[GameButton.BUTTON_EAST] != 0.0
    val south: Boolean get() = this[GameButton.BUTTON_SOUTH] != 0.0

    val lx: Double get() = this[GameButton.LX]
    val ly: Double get() = this[GameButton.LY]
    val rx: Double get() = this[GameButton.RX]
    val ry: Double get() = this[GameButton.RY]

    val l1: Boolean get() = this[GameButton.L1] != 0.0
    val l2: Double get() = this[GameButton.L2]
    val l3: Boolean get() = this[GameButton.L3] != 0.0

    val r1: Boolean get() = this[GameButton.R1] != 0.0
    val r2: Double get() = this[GameButton.R2]
    val r3: Boolean get() = this[GameButton.R3] != 0.0

    private val stick = Array(2) { MPoint() }

    operator fun get(button: GameButton): Double = rawButtons[button.index].toDouble()
    operator fun get(stick: GameStick): MPoint = this.stick[stick.id].setTo(getX(stick), getY(stick))
    fun getX(stick: GameStick) = when (stick) {
        GameStick.LEFT -> get(GameButton.LX)
        GameStick.RIGHT -> get(GameButton.RX)
    }
    fun getY(stick: GameStick) = when (stick) {
        GameStick.LEFT -> get(GameButton.LY)
        GameStick.RIGHT -> get(GameButton.RY)
    }
    fun toStringEx(includeButtons: Boolean = true): String = buildString {
        append("Gamepad[$index][$fullName]")
        if (includeButtons) {
            append("[")
            var count = 0
            for (button in GameButton.values()) {
                val value = this@GamepadInfo[button]
                if (value != 0.0) {
                    if (count > 0) append(",")
                    append("$button=${value.niceStr}")
                    count++
                }
            }
            append("]")
        }
    }
    override fun toString(): String = toStringEx(includeButtons = false)
}

data class GamePadConnectionEvent(
    override var type: Type = Type.CONNECTED,
    var gamepad: Int = 0
) : Event(), TEvent<GamePadConnectionEvent> {

    enum class Type : EventType<GamePadConnectionEvent> {
        CONNECTED, DISCONNECTED;
        companion object {
            val ALL = values()
            fun fromConnected(connected: Boolean): Type = if (connected) CONNECTED else DISCONNECTED
        }
    }

    fun copyFrom(other: GamePadConnectionEvent) {
        this.type = other.type
        this.gamepad = other.gamepad
    }
}

@Suppress("ArrayInDataClass")
data class GamePadUpdateEvent @JvmOverloads constructor(
    var gamepadsLength: Int = 0,
    val gamepads: Array<GamepadInfo> = Array(GamepadInfo.MAX_CONTROLLERS) { GamepadInfo(it) },
) : Event(), TEvent<GamePadUpdateEvent> {
    override val type: EventType<GamePadUpdateEvent> get() = GamePadUpdateEvent
    companion object : EventType<GamePadUpdateEvent>

    fun copyFrom(that: GamePadUpdateEvent) {
        this.gamepadsLength = that.gamepadsLength
        for (n in 0 until gamepads.size) {
            this.gamepads[n].copyFrom(that.gamepads[n])
        }
    }

    override fun toString(): String = "GamePadUpdateEvent(${gamepads.filter { it.connected }.map { it.toStringEx() }})"
}
