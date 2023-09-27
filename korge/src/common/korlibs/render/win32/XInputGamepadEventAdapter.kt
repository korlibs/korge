package korlibs.render.win32

import korlibs.event.*
import korlibs.ffi.*
import korlibs.io.lang.*
import korlibs.math.*
import korlibs.memory.*

internal class XInputGamepadEventAdapter {
    private val controllers = Array(GamepadInfo.MAX_CONTROLLERS) { GamepadInfo(it) }

    fun updateGamepads(emitter: GamepadInfoEmitter) {
        if (!XInput.loaded) return

        emitter.dispatchGamepadUpdateStart()
        ffiScoped {
            val state = XInputState(allocBytes(XInputState().size))
            for (n in 0 until GamepadInfo.MAX_CONTROLLERS) {
                val connected = XInput.XInputGetState(n, state.ptr) == XInput.SUCCESS
                val gamepad = controllers[n]
                if (connected) {
                    val buttons = state.wButtons.toInt() and 0xFFFF

                    gamepad.setDigital(GameButton.UP, buttons, XInput.GAMEPAD_DPAD_UP)
                    gamepad.setDigital(GameButton.DOWN, buttons, XInput.GAMEPAD_DPAD_DOWN)
                    gamepad.setDigital(GameButton.LEFT, buttons, XInput.GAMEPAD_DPAD_LEFT)
                    gamepad.setDigital(GameButton.RIGHT, buttons, XInput.GAMEPAD_DPAD_RIGHT)
                    gamepad.setDigital(GameButton.BACK, buttons, XInput.GAMEPAD_BACK)
                    gamepad.setDigital(GameButton.START, buttons, XInput.GAMEPAD_START)
                    gamepad.setDigital(GameButton.LEFT_THUMB, buttons, XInput.GAMEPAD_LEFT_THUMB)
                    gamepad.setDigital(GameButton.RIGHT_THUMB, buttons, XInput.GAMEPAD_RIGHT_THUMB)
                    gamepad.setDigital(GameButton.LEFT_SHOULDER, buttons, XInput.GAMEPAD_LEFT_SHOULDER)
                    gamepad.setDigital(GameButton.RIGHT_SHOULDER, buttons, XInput.GAMEPAD_RIGHT_SHOULDER)
                    gamepad.setDigital(GameButton.XBOX_A, buttons, XInput.GAMEPAD_A)
                    gamepad.setDigital(GameButton.XBOX_B, buttons, XInput.GAMEPAD_B)
                    gamepad.setDigital(GameButton.XBOX_X, buttons, XInput.GAMEPAD_X)
                    gamepad.setDigital(GameButton.XBOX_Y, buttons, XInput.GAMEPAD_Y)
                    gamepad.rawButtons[GameButton.LEFT_TRIGGER.index] = convertUByteRangeToDouble(state.bLeftTrigger)
                    gamepad.rawButtons[GameButton.RIGHT_TRIGGER.index] = convertUByteRangeToDouble(state.bRightTrigger)
                    gamepad.rawButtons[GameButton.LX.index] = GamepadInfo.withoutDeadRange(convertShortRangeToDouble(state.sThumbLX))
                    gamepad.rawButtons[GameButton.LY.index] = GamepadInfo.withoutDeadRange(convertShortRangeToDouble(state.sThumbLY))
                    gamepad.rawButtons[GameButton.RX.index] = GamepadInfo.withoutDeadRange(convertShortRangeToDouble(state.sThumbRX))
                    gamepad.rawButtons[GameButton.RY.index] = GamepadInfo.withoutDeadRange(convertShortRangeToDouble(state.sThumbRY))

                    if (gamepad.name == null) {
                        ffiScoped {
                            val joyCapsW = JoyCapsW(allocBytes(JoyCapsW.SIZE))
                            if (Win32Joy.joyGetDevCapsW(n, joyCapsW.ptr, JoyCapsW.SIZE) == 0) {
                                gamepad.name = joyCapsW.name
                            }
                        }
                    }
                    emitter.dispatchGamepadUpdateAdd(gamepad)
                }
            }
            emitter.dispatchGamepadUpdateEnd()
        }
    }

    private fun GamepadInfo.setDigital(button: GameButton, buttons: Int, bit: Int) {
        this.rawButtons[button.index] = if (buttons.hasBitSet(bit)) 1f else 0f
    }

    private fun convertShortRangeToDouble(value: Short): Float = value.toFloat().convertRangeClamped(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat(), -1f, +1f)
    private fun convertUByteRangeToDouble(value: Byte): Float = (value.toInt() and 0xFF).toFloat().convertRangeClamped(0f, 255f, 0f, +1f)

    object XInput : FFILib("xinput9_1_0.dll") {
        val XInputGetState by func<(dwUserIndex: Int, pState: FFIPointer?) -> Int>()

        const val SUCCESS = 0
        const val ERROR_DEVICE_NOT_CONNECTED = 0x0000048F

        const val GAMEPAD_DPAD_UP = 0
        const val GAMEPAD_DPAD_DOWN = 1
        const val GAMEPAD_DPAD_LEFT = 2
        const val GAMEPAD_DPAD_RIGHT = 3
        const val GAMEPAD_START = 4
        const val GAMEPAD_BACK = 5
        const val GAMEPAD_LEFT_THUMB = 6
        const val GAMEPAD_RIGHT_THUMB = 7
        const val GAMEPAD_LEFT_SHOULDER = 8
        const val GAMEPAD_RIGHT_SHOULDER = 9
        const val GAMEPAD_UNKNOWN_10 = 10
        const val GAMEPAD_UNKNOWN_11 = 11
        const val GAMEPAD_A = 12
        const val GAMEPAD_B = 13
        const val GAMEPAD_X = 14
        const val GAMEPAD_Y = 15

        const val SIZE = 16
    }

    object Win32Joy : FFILib("Winmm.dll") {
        val joyGetDevCapsW by func<(uJoyID: Int, pjc: FFIPointer?, cbjc: Int) -> Int>()
    }

    // Used this as reference:
    // https://github.com/fantarama/JXInput/blob/86356e7a4037bbb1f3478c7333555e00b3601bde/XInputJNA/src/main/java/com/microsoft/xinput/XInput.java
    internal class XInputState(pointer: FFIPointer? = null) : FFIStructure(pointer) {
        var dwPacketNumber by int() // offset: 0
        var wButtons by short() // offset: 4
        var bLeftTrigger by byte() // offset: 6
        var bRightTrigger by byte() // offset: 7
        var sThumbLX by short() // offset: 8
        var sThumbLY by short() // offset: 10
        var sThumbRX by short() // offset: 12
        var sThumbRY by short() // offset: 14
        override fun toString(): String =
            "XInputState(dwPacketNumber=$dwPacketNumber, wButtons=$wButtons, bLeftTrigger=$bLeftTrigger, bRightTrigger=$bRightTrigger, sThumbLX=$sThumbLX, sThumbLY=$sThumbLY, sThumbRX=$sThumbRX, sThumbRY=$sThumbRY)"
    }

    internal class JoyCapsW(pointer: FFIPointer? = null) : FFIStructure(pointer) {
        companion object {
            val SIZE = 728
        }

        var wMid: Short by short()
        var wPid: Short by short()
        var szPname by fixedBytes(32 * 2)
        var name: String
            get() = szPname.toString(Charsets.UTF16_LE).trimEnd('\u0000').also {
                //println("JoyCapsW.name='$it'")
            }
            set(value) {
                szPname = run {
                    ByteArray(szPname.size).also {
                        val new = value.toByteArray(Charsets.UTF16_LE)
                        arraycopy(new, 0, it, 0, new.size)
                    }
                }

            }
        override fun toString(): String =
            "JoyCapsW(name=$name)"
    }

}
