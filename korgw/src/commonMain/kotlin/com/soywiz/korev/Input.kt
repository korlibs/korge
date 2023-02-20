package com.soywiz.korev

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korma.geom.*
import kotlin.math.*

enum class MouseButton(val id: Int, val bits: Int = 1 shl id) {
	LEFT(0), MIDDLE(1), RIGHT(2), BUTTON3(3),
    BUTTON4(4), BUTTON5(5), BUTTON6(6), BUTTON7(7),
    BUTTON_WHEEL(8),
    BUTTON_UNKNOWN(10),
    NONE(11, bits = 0);

    val isLeft get() = this == LEFT
    val isMiddle get() = this == MIDDLE
    val isRight get() = this == RIGHT

    fun pressedFromFlags(flags: Int): Boolean = (flags and this.bits) != 0

    companion object {
        val MAX = NONE.ordinal + 1
		val BUTTONS = values()
		operator fun get(id: Int) = BUTTONS.getOrElse(id) { BUTTON_UNKNOWN }
	}
}

enum class Key {
	SPACE, APOSTROPHE, COMMA, MINUS, PLUS, PERIOD, SLASH,
	N0, N1, N2, N3, N4, N5, N6, N7, N8, N9, N11, N12,
    N3D_MODE,
	SEMICOLON, EQUAL,
    AT,
	A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z,
	LEFT_BRACKET, BACKSLASH, RIGHT_BRACKET, GRAVE_ACCENT,
	WORLD_1, WORLD_2,
	ESCAPE,
	META,
	ENTER, TAB, BACKSPACE, INSERT, DELETE,
	RIGHT, LEFT, DOWN, UP,
	PAGE_UP, PAGE_DOWN, FUNCTION, HELP, MUTE, VOLUME_DOWN, VOLUME_UP, VOLUME_MUTE,
	HOME, END,
	CAPS_LOCK, SCROLL_LOCK, NUM_LOCK,
	PRINT_SCREEN, PAUSE,
	F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12,
	F13, F14, F15, F16, F17, F18, F19, F20, F21, F22, F23, F24, F25,
	KP_0, KP_1, KP_2, KP_3, KP_4, KP_5, KP_6, KP_7, KP_8, KP_9,
	KP_DECIMAL, KP_DIVIDE, KP_MULTIPLY,
	KP_SUBTRACT, KP_ADD, KP_COMMA, KP_DOT, KP_ENTER, KP_EQUAL, KP_SEPARATOR,
    KP_LEFT_PAREN, KP_RIGHT_PAREN,

    SHIFT, CONTROL, ALT, SUPER,

	MENU, BACK,

	BACKQUOTE, QUOTE,

	KP_UP, KP_DOWN, KP_LEFT, KP_RIGHT,

	UNDERLINE, SELECT_KEY,

	CANCEL, CLEAR,

	OPEN_BRACKET, CLOSE_BRACKET,

    PLAY, NONAME, FINAL,
    OEM102, OEM1, OEM2, OEM3, OEM4, OEM5, OEM6, OEM7, OEM8,
    LEFT_MENU, RIGHT_MENU,
    SLEEP, SNAPSHOT, INFO,

    XBUTTON1, XBUTTON2, XBUTTON3, XBUTTON4, XBUTTON5, XBUTTON6, XBUTTON7, XBUTTON8, XBUTTON9,
    XBUTTON10, XBUTTON11, XBUTTON12, XBUTTON13, XBUTTON14, XBUTTON15, XBUTTON16,
    XBUTTON_A, XBUTTON_B, XBUTTON_C, XBUTTON_L1, XBUTTON_L2, XBUTTON_MODE,
    XBUTTON_R1, XBUTTON_R2, XBUTTON_SELECT, XBUTTON_START,
    XBUTTON_THUMBL, XBUTTON_THUMBR, XBUTTON_X, XBUTTON_Y, XBUTTON_Z,

    DPAD_CENTER, DPAD_DOWN, DPAD_DOWN_LEFT, DPAD_DOWN_RIGHT, DPAD_LEFT, DPAD_RIGHT, DPAD_UP, DPAD_UP_LEFT, DPAD_UP_RIGHT,

    DVR, EISU, ENDCALL, ENVELOPE, EXPLORER,
    FOCUS, FORWARD, GRAVE, GUIDE, HEADSETHOOK,

    ABNT_C1, ABNT_C2,
    ATTN, CRSEL, EREOF, EXECUTE, EXSEL,
    ICO_CLEAR, ICO_HELP,
    HENKAN, PAIRING,

    APP_SWITCH, ASSIST, AVR_INPUT, AVR_POWER, BOOKMARK, BREAK, CAPTIONS, CAMERA, CALL, CALENDAR,
    BRIGHTNESS_DOWN, BRIGHTNESS_UP,
    CHANNEL_DOWN, CHANNEL_UP,
    CALCULATOR,
    CONTACTS,
    NOTIFICATION,
    COPY, CUT, PASTE,
    SEARCH, SETTINGS,
    SOFT_LEFT, SOFT_RIGHT, SOFT_SLEEP,
    STAR, STB_INPUT, STB_POWER,
    STEM_1, STEM_2, STEM_3, STEM_PRIMARY, SWITCH_CHARSET,
    SYM, SYSRQ, NUM,
    TV, TV_ANTENNA_CABLE, TV_AUDIO_DESCRIPTION, TV_AUDIO_DESCRIPTION_MIX_DOWN, TV_AUDIO_DESCRIPTION_MIX_UP, TV_CONTENTS_MENU,
    TV_DATA_SERVICE, TV_INPUT, TV_INPUT_COMPONENT_1, TV_INPUT_COMPONENT_2,
    TV_INPUT_COMPOSITE_1, TV_INPUT_COMPOSITE_2,
    TV_INPUT_HDMI_1, TV_INPUT_HDMI_2, TV_INPUT_HDMI_3, TV_INPUT_HDMI_4,
    TV_INPUT_VGA_1, TV_MEDIA_CONTEXT_MENU, TV_NETWORK, TV_NUMBER_ENTRY,
    TV_POWER, TV_RADIO_SERVICE, TV_SATELLITE, TV_SATELLITE_BS, TV_SATELLITE_CS,
    TV_SATELLITE_SERVICE, TV_TELETEXT, TV_TERRESTRIAL_ANALOG, TV_TERRESTRIAL_DIGITAL,
    TV_TIMER_PROGRAMMING, TV_ZOOM_MODE,

    VOICE_ASSIST,
    WAKEUP, WINDOW, YEN, ZENKAKU_HANKAKU,
    ZOOM_IN, ZOOM_OUT,

    SYSTEM_NAVIGATION_DOWN, SYSTEM_NAVIGATION_LEFT, SYSTEM_NAVIGATION_RIGHT, SYSTEM_NAVIGATION_UP,
    PICTSYMBOLS, POUND, POWER, PROG_BLUE, PROG_GREEN, PROG_RED, PROG_YELLOW,
    RO,
    OEM_ATTN, OEM_AUTO, OEM_AX, OEM_BACKTAB, OEM_CLEAR, OEM_COMMA, OEM_COPY, OEM_CUSEL, OEM_ENLW, OEM_FINISH,
    OEM_FJ_LOYA, OEM_FJ_MASSHOU, OEM_FJ_ROYA, OEM_FJ_TOUROKU, OEM_JUMP, OEM_MINUS,
    OEM_PA1, OEM_PA2, OEM_PA3,
    OEM_PERIOD, OEM_PLUS, OEM_RESET, OEM_WSCTRL,
    PA1, PACKET, PROCESSKEY, ZOOM, NONE, ACCEPT, APPS,
    BROWSER_BACK, BROWSER_FAVORITES, BROWSER_FORWARD, BROWSER_HOME, BROWSER_REFRESH, BROWSER_SEARCH, BROWSER_STOP,
    CAPITAL, CONVERT, ICO_00, JUNJA, KANA, KANJI, KATAKANA_HIRAGANA, LANGUAGE_SWITCH, MUHENKAN,
    LAUNCH_APP1, LAUNCH_APP2, LAUNCH_MAIL, LAUNCH_MEDIA_SELECT,
    LEFT_BUTTON, MIDDLE_BUTTON,
    MUSIC,
    MEDIA_NEXT_TRACK, MEDIA_PLAY_PAUSE, MEDIA_PREV_TRACK, MEDIA_STOP, MEDIA_PLAY, MEDIA_PAUSE,
    MEDIA_AUDIO_TRACK, MEDIA_CLOSE, MEDIA_EJECT, MEDIA_FAST_FORWARD, MEDIA_RECORD, MEDIA_REWIND,
    MEDIA_SKIP_BACKWARD, MEDIA_SKIP_FORWARD, MEDIA_STEP_BACKWARD, MEDIA_STEP_FORWARD, MEDIA_TOP_MENU,
    MODECHANGE, NEXT, NONCONVERT,

    OEM_FJ_JISHO, PRIOR, RIGHT_BUTTON,
    LAST_CHANNEL, MANNER_MODE,

    NAVIGATE_IN, NAVIGATE_NEXT, NAVIGATE_OUT, NAVIGATE_PREVIOUS,
    HYPHEN,

	UNDEFINED,
	UNKNOWN,

    ;

    val isFunctionKey get() = when (this) {
        F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12,
        F13, F14, F15, F16, F17, F18, F19, F20, F21, F22, F23, F24, F25 -> true
        else -> false
    }

    companion object {
	    val MAX = UNKNOWN.ordinal + 1

        val RETURN = ENTER

		val NUMPAD0 = N0
		val NUMPAD1 = N1
		val NUMPAD2 = N2
		val NUMPAD3 = N3
		val NUMPAD4 = N4
		val NUMPAD5 = N5
		val NUMPAD6 = N6
		val NUMPAD7 = N7
		val NUMPAD8 = N8
		val NUMPAD9 = N9

        @Deprecated("", ReplaceWith("CONTROL", "com.soywiz.korev.Key.CONTROL")) val LEFT_CONTROL get() = CONTROL
        @Deprecated("", ReplaceWith("CONTROL", "com.soywiz.korev.Key.CONTROL")) val RIGHT_CONTROL get() = CONTROL
        @Deprecated("", ReplaceWith("SHIFT", "com.soywiz.korev.Key.SHIFT")) val LEFT_SHIFT get() = SHIFT
        @Deprecated("", ReplaceWith("SHIFT", "com.soywiz.korev.Key.SHIFT")) val RIGHT_SHIFT get() = SHIFT
        @Deprecated("", ReplaceWith("ALT", "com.soywiz.korev.Key.ALT")) val LEFT_ALT get() = ALT
        @Deprecated("", ReplaceWith("ALT", "com.soywiz.korev.Key.ALT")) val RIGHT_ALT get() = ALT
        @Deprecated("", ReplaceWith("SUPER", "com.soywiz.korev.Key.SUPER")) val LEFT_SUPER get() = SUPER
        @Deprecated("", ReplaceWith("SUPER", "com.soywiz.korev.Key.SUPER")) val RIGHT_SUPER get() = SUPER
    }
}

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

class GamepadInfoEmitter(val dispatcher: EventDispatcher) {
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
	override fun toString(): String = "Gamepad[$index][$fullName]"
}
