package com.soywiz.korgw

import com.soywiz.kmem.convertRangeClamped
import com.soywiz.korev.*
import kotlinx.cinterop.*
import platform.windows.GetProcAddress
import platform.windows.LoadLibraryA
import kotlin.jvm.JvmField

internal val XINPUT_DLL by lazy { LoadLibraryA("xinput9_1_0.dll") }

internal val XInputGetState by lazy {
    GetProcAddress(XINPUT_DLL, "XInputGetState")?.reinterpret<CFunction<(dwUserIndex: Int, pState: CPointer<ByteVar>?) -> Int>>()
}

internal const val ERROR_SUCCESS = 0
internal const val ERROR_DEVICE_NOT_CONNECTED = 0x48F

internal class XInputState {
    companion object {
        const val XINPUT_GAMEPAD_DPAD_UP = 0
        const val XINPUT_GAMEPAD_DPAD_DOWN = 1
        const val XINPUT_GAMEPAD_DPAD_LEFT = 2
        const val XINPUT_GAMEPAD_DPAD_RIGHT = 3
        const val XINPUT_GAMEPAD_START = 4
        const val XINPUT_GAMEPAD_BACK = 5
        const val XINPUT_GAMEPAD_LEFT_THUMB = 6
        const val XINPUT_GAMEPAD_RIGHT_THUMB = 7
        const val XINPUT_GAMEPAD_LEFT_SHOULDER = 8
        const val XINPUT_GAMEPAD_RIGHT_SHOULDER = 9
        const val XINPUT_GAMEPAD_UNKNOWN_10 = 10
        const val XINPUT_GAMEPAD_UNKNOWN_11 = 11
        const val XINPUT_GAMEPAD_A = 12
        const val XINPUT_GAMEPAD_B = 13
        const val XINPUT_GAMEPAD_X = 14
        const val XINPUT_GAMEPAD_Y = 15

        const val SIZE = 16
    }

    @JvmField var dwPacketNumber: Int = 0 // offset: 0
    @JvmField var wButtons: Short = 0 // offset: 4, short = 2
    @JvmField var bLeftTrigger: Byte = 0 // offset: 6
    @JvmField var bRightTrigger: Byte = 0 // offset: 7
    @JvmField var sThumbLX: Short = 0 // offset: 8
    @JvmField var sThumbLY: Short = 0 // offset: 10
    @JvmField var sThumbRX: Short = 0 // offset: 12
    @JvmField var sThumbRY: Short = 0 // offset: 14

    fun write(ptr: CPointer<ByteVarOf<Byte>>) {
        val sptr = ptr.reinterpret<ShortVar>()
        dwPacketNumber = ptr.reinterpret<IntVar>()[0]
        wButtons = sptr[2]
        bLeftTrigger = ptr[6]
        bRightTrigger = ptr[7]
        sThumbLX = sptr[4]
        sThumbLY = sptr[5]
        sThumbRX = sptr[6]
        sThumbRY = sptr[7]
    }

    override fun toString(): String {
        return "XInputState(dwPacketNumber=$dwPacketNumber, wButtons=$wButtons, bLeftTrigger=$bLeftTrigger, bRightTrigger=$bRightTrigger, sThumbLX=$sThumbLX, sThumbLY=$sThumbLY, sThumbRX=$sThumbRX, sThumbRY=$sThumbRY)"
    }
}

internal class XInputEventAdapter {
    private val MAX_GAMEPADS = 4
    private val gamepadsConnected = BooleanArray(MAX_GAMEPADS)
    private val xinputState = XInputState()
    private val gamePadUpdateEvent = GamePadUpdateEvent()
    private val gamePadConnectionEvent = GamePadConnectionEvent()

    private fun convertShortRangeToDouble(value: Short): Double = value.toDouble().convertRangeClamped(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble(), -1.0, +1.0)
    private fun convertUByteRangeToDouble(value: Byte): Double {
        return (value.toInt() and 0xFF).toDouble().convertRangeClamped(0.0, 255.0, 0.0, +1.0)
    }

    fun updateGamepadsWin32(dispatcher: EventDispatcher) {
        if (XInputGetState == null) return

        var connectedCount = 0
        val state = xinputState
        for (n in 0 until MAX_GAMEPADS) {
            val prevConnected = gamepadsConnected[n]
            val connected = memScoped {
                val data = allocArray<ByteVar>(XInputState.SIZE)
                (XInputGetState?.invoke(n, data) == 0).also {
                    state.write(data)
                }
            }
            val gamepad = gamePadUpdateEvent.gamepads[n]
            gamepad.connected = connected
            if (connected) {
                gamepad.mapping = XInputMapping
                gamepad.rawButtonsPressed = (state.wButtons.toInt() and 0xFFFF)
                gamepad.rawButtonsPressure[GameButton.L2.index] = convertUByteRangeToDouble(state.bLeftTrigger)
                gamepad.rawButtonsPressure[GameButton.R2.index] = convertUByteRangeToDouble(state.bRightTrigger)
                gamepad.rawAxes[0] = convertShortRangeToDouble(state.sThumbLX)
                gamepad.rawAxes[1] = convertShortRangeToDouble(state.sThumbLY)
                gamepad.rawAxes[2] = convertShortRangeToDouble(state.sThumbRX)
                gamepad.rawAxes[3] = convertShortRangeToDouble(state.sThumbRY)
                connectedCount++
            }
            if (prevConnected != connected) {
                if (connected) {
                }

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

    }

    object XInputMapping : GamepadMapping() {
        override val id = "XInput"

        override fun get(button: GameButton, info: GamepadInfo): Double = when (button) {
            GameButton.XBOX_A -> info.getRawButton(XInputState.XINPUT_GAMEPAD_A)
            GameButton.XBOX_B -> info.getRawButton(XInputState.XINPUT_GAMEPAD_B)
            GameButton.XBOX_X -> info.getRawButton(XInputState.XINPUT_GAMEPAD_X)
            GameButton.XBOX_Y -> info.getRawButton(XInputState.XINPUT_GAMEPAD_Y)
            GameButton.L1     -> info.getRawButton(XInputState.XINPUT_GAMEPAD_LEFT_SHOULDER)
            GameButton.R1     -> info.getRawButton(XInputState.XINPUT_GAMEPAD_RIGHT_SHOULDER)
            GameButton.L2     -> info.getRawPressureButton(GameButton.L2.index)
            GameButton.R2     -> info.getRawPressureButton(GameButton.R2.index)
            GameButton.LEFT_THUMB -> info.getRawButton(XInputState.XINPUT_GAMEPAD_LEFT_THUMB)
            GameButton.RIGHT_THUMB -> info.getRawButton(XInputState.XINPUT_GAMEPAD_RIGHT_THUMB)
            GameButton.BACK -> info.getRawButton(XInputState.XINPUT_GAMEPAD_BACK)
            GameButton.START -> info.getRawButton(XInputState.XINPUT_GAMEPAD_START)
            GameButton.UP -> info.getRawButton(XInputState.XINPUT_GAMEPAD_DPAD_UP)
            GameButton.DOWN -> info.getRawButton(XInputState.XINPUT_GAMEPAD_DPAD_DOWN)
            GameButton.LEFT -> info.getRawButton(XInputState.XINPUT_GAMEPAD_DPAD_LEFT)
            GameButton.RIGHT -> info.getRawButton(XInputState.XINPUT_GAMEPAD_DPAD_RIGHT)
            GameButton.SYSTEM -> 0.0
            GameButton.LX -> info.getRawAxe(0)
            GameButton.LY -> info.getRawAxe(1)
            GameButton.RX -> info.getRawAxe(2)
            GameButton.RY -> info.getRawAxe(3)
            else -> 0.0
        }
    }
}

fun test() {
    memScoped {
        val state = XInputState()
        val data = allocArray<ByteVar>(16)
        if (XInputGetState?.invoke(0, data) == 0) {
            state.write(data)
        }
    }
}
