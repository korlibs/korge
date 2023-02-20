package com.soywiz.korgw

import com.soywiz.kmem.convertRangeClamped
import com.soywiz.korev.*
import com.soywiz.korev.gamepad.*
import kotlinx.cinterop.*
import platform.windows.GetProcAddress
import platform.windows.LoadLibraryA

internal val XINPUT_DLL by lazy { LoadLibraryA("xinput9_1_0.dll") }

internal val XInputGetState by lazy {
    GetProcAddress(XINPUT_DLL, "XInputGetState")?.reinterpret<CFunction<(dwUserIndex: Int, pState: CPointer<ByteVar>?) -> Int>>()
}

internal const val ERROR_SUCCESS = 0
internal const val ERROR_DEVICE_NOT_CONNECTED = 0x48F

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
                val data = allocArray<ByteVar>(XInputMapping.SIZE)
                (XInputGetState?.invoke(n, data) == 0).also {
                    state.write(data)
                }
            }
            val gamepad = gamePadUpdateEvent.gamepads[n]
            gamepad.connected = connected
            if (connected) {
                XInputMapping.setController(
                    gamepad,
                    state.wButtons, state.bLeftTrigger, state.bRightTrigger, state.sThumbLX, state.sThumbLY, state.sThumbRX, state.sThumbRY
                )
                connectedCount++
            }
            if (prevConnected != connected) {
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
}

/*
fun test() {
    memScoped {
        val state = XInputState()
        val data = allocArray<ByteVar>(16)
        if (XInputGetState?.invoke(0, data) == 0) {
            state.write(data)
        }
    }
}
*/
