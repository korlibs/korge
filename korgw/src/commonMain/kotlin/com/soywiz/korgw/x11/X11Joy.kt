package com.soywiz.korgw.x11

import com.soywiz.kds.*
import com.soywiz.kds.diff.*
import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korev.*
import com.soywiz.korio.concurrent.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.sync.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.math.*

internal class LinuxJoyEventAdapter : Closeable {
    private val gamePadUpdateEvent = GamePadUpdateEvent()
    private val gamePadConnectionEvent = GamePadConnectionEvent()

    data class DeviceInfo(val namedDevice: String, val finalDevice: String) : Extra by Extra.Mixin() {
        val baseName = PathInfo(namedDevice).baseName.removePrefix("usb-").removeSuffix("-joystick")
        val id: Int = Regex("\\d+$").find(finalDevice)?.value?.toInt() ?: -1
        override fun toString(): String = "DeviceInfo[$id]($baseName)"
    }

    fun listJoysticks(): List<DeviceInfo> {
        val base = "/dev/input/by-id"
        return SyncIO.list(base)
            .map { "$base/$it" }
            .filter { it.endsWith("-joystick") }
            .map { DeviceInfo(it, (SyncIO.readlink(it) ?: "")) }
            .filter { it.finalDevice.contains("/js") }
    }

    class RunEvery(val time: TimeSpan) {
        var lastRun = DateTime.EPOCH
        operator fun invoke(block: () -> Unit) {
            val now = DateTime.now()
            if (now - lastRun >= time) {
                lastRun = now
                return block()
            }
        }
    }

    private val checkGamepadsRun = RunEvery(1.seconds)

    var oldJoysticks = emptySet<DeviceInfo>()
    var joysticks = emptySet<DeviceInfo>()

    val readers = LinkedHashMap<Int, X11JoystickReader>()

    fun updateGamepads(dispatcher: EventDispatcher) {
        //Dispatchers.Unconfined.launchUnscoped { listJoysticks() }
        checkGamepadsRun {
            joysticks = listJoysticks().toSet()
        }
        val diff = Diff.compare(oldJoysticks, joysticks)
        oldJoysticks = joysticks

        for (device in diff.removed) {
            dispatcher.dispatch(gamePadConnectionEvent.also {
                it.gamepad = device.id
                it.type = GamePadConnectionEvent.Type.DISCONNECTED
            })
            readers.remove(device.id)?.close()
        }
        for (device in diff.added) {
            readers[device.id] = X11JoystickReader(device)
            dispatcher.dispatch(gamePadConnectionEvent.also {
                it.gamepad = device.id
                it.type = GamePadConnectionEvent.Type.CONNECTED
            })
        }
        val connected = diff.added + diff.kept
        for ((index, device) in connected.sortedBy { it.id }.withIndex()) {
            val reader = readers[device.id] ?: continue
            reader.read(gamePadUpdateEvent.gamepads[index], index)
        //println("${device.baseName} : $")
        }

        gamePadUpdateEvent.gamepadsLength = connected.size
        if (connected.size > 0) {
            dispatcher.dispatch(gamePadUpdateEvent)
        }
    }

    override fun close() {
        readers.forEach { it.value.close() }
        readers.clear()
    }

    internal class X11JoystickReader(val info: DeviceInfo) : Closeable {
        val index: Int = info.id

        companion object {
            const val JS_EVENT_BUTTON = 0x01    /* button pressed/released */
            const val JS_EVENT_AXIS = 0x02    /* joystick moved */
            const val JS_EVENT_INIT = 0x80    /* initial state of device */
        }

        private var prevConnected = false
        var connected = false; private set
        private var buttons = 0
        private val axes = DoubleArray(16)

        fun setButton(index: Int, value: Boolean) {
            buttons = buttons.setBits(1 shl index, value)
        }
        fun setButton(button: GameButton, value: Boolean) {
            setButton(button.index, value)
        }

        fun getConnectedChange(): Boolean? {
            if (prevConnected != connected) {
                prevConnected = connected
                return connected
            }
            return null
        }

        fun read(gamepad: GamepadInfo, index: Int) {
            gamepad.playerIndex = info.id
            gamepad.rawButtonsPressed = buttons
            gamepad.name = info.baseName
            arraycopy(axes, 0, gamepad.rawAxes, 0, 16)
        }

        val dispatcher = Dispatchers.createSingleThreadedDispatcher("index").also {
            it.dispatch(EmptyCoroutineContext, Runnable { threadMain() })
        }

        fun threadMain() {
            while (true) {
                try {
                    connected = false
                    val raf = platformSyncIO.open("/dev/input/js$index", "r")
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

                            //println("JS_EVENT: time=$time, value=$value, type=$type, number=$number")

                            if (type hasFlags JS_EVENT_AXIS) {
                                val axeButton = when (number) {
                                    0 -> GameButton.LY
                                    1 -> GameButton.LX
                                    2 -> GameButton.L2
                                    3 -> GameButton.RY
                                    4 -> GameButton.RX
                                    5 -> GameButton.R2
                                    6 -> GameButton.DPADX
                                    7 -> GameButton.DPADY
                                    else -> GameButton.BUTTON8
                                }
                                val dvalue = (value.toDouble() / 32767).clamp(-1.0, +1.0)
                                val axeIndex = StandardGamepadMapping.getAxeIndex(axeButton)
                                axes[axeIndex] = dvalue
                                maxAxes = max(maxAxes, axeIndex)
                                when (axeButton) {
                                    GameButton.DPADX, GameButton.DPADY -> {
                                        val L = if (axeButton == GameButton.DPADX) GameButton.LEFT else GameButton.UP
                                        val R = if (axeButton == GameButton.DPADX) GameButton.RIGHT else GameButton.DOWN
                                        when {
                                            dvalue < 0.0 -> setButton(L, true)
                                            dvalue > 0.0 -> setButton(R, true)
                                            else -> {
                                                setButton(L, false)
                                                setButton(R, false)
                                            }
                                        }
                                    }
                                    GameButton.L2 -> setButton(GameButton.L2, dvalue >= -0.9)
                                    GameButton.R2 -> setButton(GameButton.R2, dvalue >= -0.9)
                                    else -> Unit
                                }
                            }
                            if (type hasFlags JS_EVENT_BUTTON) {
                                val button = when (number) {
                                    0 -> GameButton.XBOX_A
                                    1 -> GameButton.XBOX_B
                                    2 -> GameButton.XBOX_X
                                    3 -> GameButton.XBOX_Y
                                    4 -> GameButton.L1
                                    5 -> GameButton.R1
                                    6 -> GameButton.SELECT
                                    7 -> GameButton.START
                                    8 -> GameButton.SYSTEM
                                    9 -> GameButton.L3
                                    10 -> GameButton.R3
                                    else -> GameButton.BUTTON8
                                }
                                setButton(button, value != 0)
                                maxButtons = max(maxButtons, button.index)
                            }
                            //println("$time, $type, $number, $value: ${buttons.toStringUnsigned(2)}, ${axes.slice(0 until maxAxes).toList()}")
                        }
                    }
                } catch (e: Throwable) {
                    //e.printStackTrace()
                    Thread_sleep(1000L)
                } finally {
                    connected = false
                }
            }
        }

        override fun close() {
            dispatcher.close()
        }
    }
}
