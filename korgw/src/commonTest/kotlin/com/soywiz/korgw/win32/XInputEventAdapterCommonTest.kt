package com.soywiz.korgw.win32

import com.soywiz.kmem.*
import com.soywiz.korev.*
import kotlin.test.*

class XInputEventAdapterCommonTest {
    @Test
    fun test() {
        val xinput = XInput { dwUserIndex, pState ->
            when (dwUserIndex) {
                0 -> {
                    pState.wButtons = buildBits(XInput.GAMEPAD_DPAD_UP).toShort()
                    XInput.SUCCESS
                }
                2 -> {
                    pState.wButtons = buildBits(XInput.GAMEPAD_DPAD_RIGHT, XInput.GAMEPAD_Y).toShort()
                    XInput.SUCCESS
                }
                else -> XInput.ERROR_DEVICE_NOT_CONNECTED
            }
        }
        val joy = Joy32 { uJoyID, pjc, cbjc ->
            pjc.name = "test-$uJoyID"
            0
        }
        val logs = arrayListOf<String>()
        val adapter = Win32XInputEventAdapterCommon(xinput, joy)
        val dispatcher = BaseEventListener()
        dispatcher.onEvent(*GamePadConnectionEvent.Type.ALL) { logs += "$it" }
        dispatcher.onEvent(GamePadUpdateEvent) { logs += "$it" }
        val emitter = GamepadInfoEmitter(dispatcher)
        adapter.updateGamepads(emitter)
        assertEquals(
            """
                GamePadConnectionEvent(type=CONNECTED, gamepad=0)
                GamePadConnectionEvent(type=CONNECTED, gamepad=1)
                GamePadUpdateEvent([Gamepad[0][test-0 - Wireless Controller][UP=1], Gamepad[1][test-2 - Wireless Controller][RIGHT=1,BUTTON_NORTH=1]])
            """.trimIndent(),
            logs.joinToString("\n")
        )
    }

    private fun buildBits(vararg bits: Int): Int {
        var v = 0
        for (bit in bits) v = v.insert(true, bit)
        return v
    }
}
