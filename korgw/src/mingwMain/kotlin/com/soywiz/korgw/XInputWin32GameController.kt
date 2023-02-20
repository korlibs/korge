package com.soywiz.korgw

import com.soywiz.korev.*
import com.soywiz.korev.gamepad.*
import kotlinx.cinterop.*
import platform.windows.*

private val XINPUT_DLL by lazy { LoadLibraryA("xinput9_1_0.dll") }
private val XInputGetState by lazy {
    GetProcAddress(XINPUT_DLL, "XInputGetState")?.reinterpret<CFunction<(dwUserIndex: Int, pState: CPointer<ByteVar>?) -> Int>>()
}

internal class XInputEventAdapter {
    private val xinputState = XInputState()
    private val controllers = Array(GamepadInfo.MAX_CONTROLLERS) { GamepadInfo(it) }

    fun updateGamepadsWin32(gameWindow: GameWindow) {
        if (XInputGetState == null) return

        val state = xinputState
        gameWindow.dispatchGamepadUpdateStart()
        for (n in 0 until GamepadInfo.MAX_CONTROLLERS) {
            val connected = memScoped {
                val data = allocArray<ByteVar>(XInputMapping.SIZE)
                (XInputGetState?.invoke(n, data) == SUCCESS).also {
                    state.write(data)
                }
            }
            val gamepad = controllers[n]
            if (connected) {
                XInputMapping.setController(
                    gamepad,
                    state.wButtons, state.bLeftTrigger, state.bRightTrigger, state.sThumbLX, state.sThumbLY, state.sThumbRX, state.sThumbRY
                )
                gameWindow.dispatchGamepadUpdateAdd(gamepad)
            }
        }
        gameWindow.dispatchGamepadUpdateEnd()
    }

    companion object {
        private const val SUCCESS = 0
        private const val ERROR_DEVICE_NOT_CONNECTED = 0x48F
    }


    // https://learn.microsoft.com/en-us/windows/win32/api/xinput/ns-xinput-xinput_gamepad
    internal class XInputState {
        var dwPacketNumber: Int = 0 // offset: 0
        var wButtons: Short = 0 // offset: 4, short = 2
        var bLeftTrigger: Byte = 0 // offset: 6
        var bRightTrigger: Byte = 0 // offset: 7
        var sThumbLX: Short = 0 // offset: 8
        var sThumbLY: Short = 0 // offset: 10
        var sThumbRX: Short = 0 // offset: 12
        var sThumbRY: Short = 0 // offset: 14

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

        override fun toString(): String =
            "XInputState(dwPacketNumber=$dwPacketNumber, wButtons=$wButtons, bLeftTrigger=$bLeftTrigger, bRightTrigger=$bRightTrigger, sThumbLX=$sThumbLX, sThumbLY=$sThumbLY, sThumbRX=$sThumbRX, sThumbRY=$sThumbRY)"
    }
}
