package com.soywiz.korgw.x11

import com.soywiz.kmem.*
import com.soywiz.korev.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.sync.*
import com.soywiz.korio.lang.*
import kotlin.test.*

class LinuxJoyEventAdapterTest {
    @Test
    fun test() = suspendTest({ !Platform.isJs }) {
        val sync = MemorySyncIO()
        sync.writelink("/dev/input/by-id/usb-Xbox_Controller-joystick", "../js1")
        sync.write("/dev/input/js1", byteArrayOf(
            *packet(time = 0, type = LinuxJoyEventAdapter.JS_EVENT_BUTTON, value = Short.MAX_VALUE.toInt(), number = 0), // A
        ))
        val logs = arrayListOf<String>()
        LinuxJoyEventAdapter(sync).use { adapter ->
            val dispatcher = EventDispatcher()
            dispatcher.addEventListener<GamePadConnectionEvent> { logs += "$it" }
            dispatcher.addEventListener<GamePadUpdateEvent> { logs += "$it" }
            val emitter = GamepadInfoEmitter(dispatcher)
            adapter.ensureJoysticks()
            adapter.updateGamepads(emitter)
            adapter.readers[1]?.once?.await()
            adapter.updateGamepads(emitter)
            sync.delete("/dev/input/by-id/usb-Xbox_Controller-joystick")
            adapter.ensureJoysticks()
            adapter.updateGamepads(emitter)
            assertEquals(
                """
                    GamePadConnectionEvent(type=CONNECTED, gamepad=0)
                    GamePadUpdateEvent([Gamepad[0][Xbox Controller - Wireless Controller][]])
                    GamePadUpdateEvent([Gamepad[0][Xbox Controller - Wireless Controller][BUTTON_SOUTH=1]])
                    GamePadConnectionEvent(type=DISCONNECTED, gamepad=0)
                    GamePadUpdateEvent([])
                """.trimIndent(),
                logs.joinToString("\n")
            )
        }
    }

    private fun packet(time: Int, value: Int, type: Int, number: Int): ByteArray {
        val packet = ByteArray(8)
        packet.write32LE(0, time) // time
        packet.write16LE(4, value) // value
        packet.write8(6, type) // type
        packet.write8(7, number) // number
        return packet
    }
}
