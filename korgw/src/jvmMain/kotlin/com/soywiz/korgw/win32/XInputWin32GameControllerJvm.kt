package com.soywiz.korgw.win32

import com.soywiz.korev.*
import com.soywiz.korev.gamepad.*
import com.soywiz.korgw.*
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Structure

// Used this as reference:
// https://github.com/fantarama/JXInput/blob/86356e7a4037bbb1f3478c7333555e00b3601bde/XInputJNA/src/main/java/com/microsoft/xinput/XInput.java
internal interface XInput : Library {
    companion object {
        operator fun invoke(): XInput? = try {
            Native.load(XINPUT_DLL_9_1_0, XInput::class.java).also {
                it.XInputGetState(0, null)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }

        const val XINPUT_DLL_9_1_0 = "xinput9_1_0.dll"
        const val SUCCESS = 0
        const val ERROR_DEVICE_NOT_CONNECTED = 0x48F
        private fun load(): XInput = Native.load(XINPUT_DLL_9_1_0, XInput::class.java)
    }

    fun XInputGetState(dwUserIndex: Int, pState: XInputState?): Int
}

internal class XInputState() : Structure() {
    @JvmField var dwPacketNumber: Int = 0 // offset: 0
    @JvmField var wButtons: Short = 0 // offset: 4
    @JvmField var bLeftTrigger: Byte = 0 // offset: 6
    @JvmField var bRightTrigger: Byte = 0 // offset: 7
    @JvmField var sThumbLX: Short = 0 // offset: 8
    @JvmField var sThumbLY: Short = 0 // offset: 10
    @JvmField var sThumbRX: Short = 0 // offset: 12
    @JvmField var sThumbRY: Short = 0 // offset: 14

    override fun getFieldOrder(): List<String> {
        return listOf(
            XInputState::dwPacketNumber.name,
            XInputState::wButtons.name,
            XInputState::bLeftTrigger.name,
            XInputState::bRightTrigger.name,
            XInputState::sThumbLX.name,
            XInputState::sThumbLY.name,
            XInputState::sThumbRX.name,
            XInputState::sThumbRY.name,
        )
    }

    override fun toString(): String {
        return "XInputState(dwPacketNumber=$dwPacketNumber, wButtons=$wButtons, bLeftTrigger=$bLeftTrigger, bRightTrigger=$bRightTrigger, sThumbLX=$sThumbLX, sThumbLY=$sThumbLY, sThumbRX=$sThumbRX, sThumbRY=$sThumbRY)"
    }
}

internal class XInputEventAdapter {
    private val xinputState = XInputState()
    private val gamePadUpdateEvent = GamePadUpdateEvent()
    private val xinput: XInput? by lazy { XInput() }
    private val joy by lazy { Win32Joy() }
    private val joyCapsW = JoyCapsW()

    fun updateGamepadsWin32(gameWindow: GameWindow) {
        if (xinput == null) return

        val state = xinputState
        gameWindow.dispatchGamepadUpdateStart()
        for (n in 0 until GamepadInfo.MAX_CONTROLLERS) {
            val connected = xinput?.XInputGetState(n, state) == XInput.SUCCESS
            val gamepad = gamePadUpdateEvent.gamepads[n]
            if (connected) {
                XInputMapping.setController(
                    gamepad,
                    state.wButtons, state.bLeftTrigger, state.bRightTrigger, state.sThumbLX, state.sThumbLY, state.sThumbRX, state.sThumbRY
                )
                if (gamepad.name == null && joy?.joyGetDevCapsW(n, joyCapsW, JoyCapsW.SIZE) == 0) {
                    gamepad.name = joyCapsW.name
                }
                gameWindow.dispatchGamepadUpdateAdd(gamepad)
            }
        }
        gameWindow.dispatchGamepadUpdateEnd()
    }
}

/*
object XInputSample {
    @JvmStatic
    fun main(args: Array<String>) {
        val state = XInputState()
        val xinput = XInput()
        while (true) {
            val result = xinput?.XInputGetState(0, state)
            println("XINPUT[0]: $result, $state")
            Thread.sleep(300L)
        }
    }
}
*/
