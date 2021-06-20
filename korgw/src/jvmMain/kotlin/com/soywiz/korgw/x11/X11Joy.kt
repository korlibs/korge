package com.soywiz.korgw.x11

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kgl.internal.*
import com.soywiz.kmem.*
import com.soywiz.korev.*
import java.io.RandomAccessFile
import kotlin.math.*

/*
fun main() {
    Thread { X11JoystickReader(0).threadMain() }
        .also { it.isDaemon = true }
        .also { it.start() }

    Thread.sleep(1000000000L)
}
*/

internal class LinuxJoyEventAdapter {
    companion object {
        //val MAX_COUNT = 4
        val MAX_COUNT = 1
    }
    val readers by lazy { Array(MAX_COUNT) { X11JoystickReader(it).also { it.startThread() } } }
    private val gamePadUpdateEvent = GamePadUpdateEvent()
    private val gamePadConnectionEvent = GamePadConnectionEvent()

    fun updateGamepads(dispatcher: EventDispatcher) {
        //println("LINUX")
        var connectedCount = 0
        readers.fastForEach { reader ->
            val connected = reader.getConnectedChange()
            if (connected != null) {
                //println("TRIGGER CONNECTION EVENT")
                dispatcher.dispatch(gamePadConnectionEvent.also {
                    it.gamepad = reader.index
                    it.type = if (connected) GamePadConnectionEvent.Type.CONNECTED else GamePadConnectionEvent.Type.DISCONNECTED
                })
            }
            if (reader.connected) {
                connectedCount++
                reader.read(gamePadUpdateEvent.gamepads[reader.index])
            }
        }
        gamePadUpdateEvent.gamepadsLength = connectedCount
        if (connectedCount > 0) {
            dispatcher.dispatch(gamePadUpdateEvent)
        }
    }
}

internal class X11JoystickReader(val index: Int) {
    companion object {
        const val JS_EVENT_BUTTON = 0x01    /* button pressed/released */
        const val JS_EVENT_AXIS = 0x02    /* joystick moved */
        const val JS_EVENT_INIT = 0x80    /* initial state of device */
    }

    private var prevConnected = false
    var connected = false; private set
    private var buttons = 0
    private val axes = DoubleArray(16)

    fun getConnectedChange(): Boolean? {
        if (prevConnected != connected) {
            prevConnected = connected
            return connected
        }
        return null
    }

    fun read(gamepad: GamepadInfo) {
        gamepad.rawButtonsPressed = buttons
        arraycopy(axes, 0, gamepad.rawAxes, 0, 16)
    }

    fun startThread(): Thread {
        return Thread { threadMain() }
            .also { it.isDaemon = true }
            .also { it.start() }
    }

    fun threadMain() {
        while (true) {
            try {
                connected = false
                val raf = RandomAccessFile("/dev/input/js$index", "r")
                connected = true
                val packet = ByteArray(8)
                val isLittleEndian = Endian.NATIVE == Endian.LITTLE_ENDIAN
                var maxButtons = 0
                var maxAxes = 0
                while (true) {
                    if (raf.read(packet) == 8) {
                        val time = packet.readS32(0, isLittleEndian)
                        val value = packet.readS16(4, isLittleEndian)
                        val type = packet.readU8(6)
                        val number = packet.readU8(7)

                        if (type hasFlags JS_EVENT_AXIS) {
                            axes[number] = (value.toDouble() / 32767).clamp(-1.0, +1.0)
                            maxAxes = max(maxAxes, number)
                        }
                        if (type hasFlags JS_EVENT_BUTTON) {
                            buttons = buttons.setBits(1 shl number, value != 0)
                            maxButtons = max(maxButtons, number)
                        }
                        //println("$time, $type, $number, $value: ${buttons.toStringUnsigned(2)}, ${axes.slice(0 until maxAxes).toList()}")
                    }
                }
            } catch (e: Throwable) {
                //e.printStackTrace()
                Thread.sleep(1000L)
            } finally {
                connected = false
            }
        }
    }
}
