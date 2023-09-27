package korlibs.render.x11

import korlibs.event.*
import korlibs.io.async.*
import korlibs.io.file.sync.*
import korlibs.io.lang.*
import korlibs.memory.*
import korlibs.platform.*
import kotlin.test.*

class LinuxJoyEventAdapterTest {
    @Test
    fun test() = suspendTest({ !Platform.isJs && !Platform.isWasm }) {
        val sync = MemorySyncIO()
        sync.writelink("/dev/input/by-id/usb-Xbox_Controller-joystick", "../js1")
        sync.write(
            "/dev/input/js1", byteArrayOf(
                *packet(
                    time = 0,
                    type = LinuxJoyEventAdapter.JS_EVENT_BUTTON,
                    value = Short.MAX_VALUE.toInt(),
                    number = 0
                ), // A
            )
        )
        val logs = arrayListOf<String>()
        LinuxJoyEventAdapter(sync).use { adapter ->
            val dispatcher = BaseEventListener()
            dispatcher.onEvents(*GamePadConnectionEvent.Type.ALL) { logs += "$it" }
            dispatcher.onEvent(GamePadUpdateEvent) { logs += "$it" }
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
        packet.set32LE(0, time) // time
        packet.set16LE(4, value) // value
        packet.set8(6, type) // type
        packet.set8(7, number) // number
        return packet
    }
}
