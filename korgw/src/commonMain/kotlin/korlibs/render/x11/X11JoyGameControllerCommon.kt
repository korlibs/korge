package korlibs.render.x11

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.time.*
import korlibs.memory.*
import korlibs.event.*
import korlibs.io.async.*
import korlibs.io.concurrent.*
import korlibs.io.concurrent.atomic.*
import korlibs.io.concurrent.atomic.KorAtomicInt
import korlibs.io.file.*
import korlibs.io.file.sync.*
import korlibs.io.lang.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

/**
 * <https://www.kernel.org/doc/Documentation/input/gamepad.txt>
 */
internal class LinuxJoyEventAdapter(val syncIO: SyncIO = SyncIO) : Closeable {
    companion object {
        const val JS_EVENT_BUTTON = 0x01    /* button pressed/released */
        const val JS_EVENT_AXIS = 0x02    /* joystick moved */
        const val JS_EVENT_INIT = 0x80    /* initial state of device */
    }

    data class DeviceInfo(val namedDevice: String, val finalDevice: String) : Extra by Extra.Mixin() {
        val baseName = PathInfo(namedDevice).baseName.removePrefix("usb-").removeSuffix("-joystick").replace("_", " ")
        val id: Int = Regex("\\d+$").find(finalDevice)?.value?.toInt() ?: -1
        override fun toString(): String = "DeviceInfo[$id]($baseName)"
    }

    fun listJoysticks(): List<DeviceInfo> {
        val base = "/dev/input/by-id"
        return syncIO.list(base)
            .map { "$base/$it" }
            .filter { it.endsWith("-joystick") }
            .map { DeviceInfo(it, (syncIO.readlink(it) ?: "")) }
            .filter { it.finalDevice.contains("/js") }
            .sortedBy { it.id }
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

    private val checkGamepadsRun = RunEvery(0.1.seconds)

    var joysticks = listOf<DeviceInfo>()

    val readers = LinkedHashMap<Int, X11JoystickReader>()
    private val gamepad = GamepadInfo()

    fun ensureJoysticks() {
        joysticks = listJoysticks()
    }

    private fun getReader(device: DeviceInfo): X11JoystickReader =
        readers.getOrPut(device.id) { X11JoystickReader(device, syncIO) }

    fun updateGamepads(emitter: GamepadInfoEmitter) {
        checkGamepadsRun {
            //Dispatchers.Unconfined.launchUnscoped { ensureJoysticks() }
            ensureJoysticks()
        }
        emitter.dispatchGamepadUpdateStart()
        joysticks.fastForEach { device ->
            val reader = getReader(device)
            reader.read(gamepad)
            emitter.dispatchGamepadUpdateAdd(gamepad)
            reader.dispatcher // Ensure dispatcher
        }
        emitter.dispatchGamepadUpdateEnd()

        val joystickDeviceIds = joysticks.map { it.id }.toSet()
        val deletedDeviceIds = readers.keys.filter { it !in joystickDeviceIds }
        deletedDeviceIds.fastForEach { readers.remove(it)?.close() }
    }

    override fun close() {
        readers.forEach { it.value.close() }
        readers.clear()
    }

    internal class X11JoystickReader(val info: DeviceInfo, val platformSyncIO: SyncIO) : Closeable {
        val index: Int = info.id

        private val buttonsPressure = FloatArray(GameButton.MAX)

        fun setButton(button: GameButton, value: Boolean) {
            buttonsPressure[button.index] = if (value) 1f else 0f
        }

        fun read(gamepad: GamepadInfo) {
            gamepad.name = info.baseName
            arraycopy(buttonsPressure, 0, gamepad.rawButtons, 0, buttonsPressure.size)
        }

        private var running = true
        val readCount = KorAtomicInt(0)
        val once = CompletableDeferred<Unit>()
        val dispatcher by lazy { Dispatchers.createSingleThreadedDispatcher("index").also {
            it.dispatch(EmptyCoroutineContext, Runnable { threadMain() })
        } }

        fun threadMain() {
            val packet = ByteArray(8)
            val isLittleEndian = Endian.NATIVE == Endian.LITTLE_ENDIAN
            while (running) {
                try {
                    val raf = platformSyncIO.open("/dev/input/js$index", "r")
                    while (running) {
                        if (raf.read(packet) == 8) {
                            val time = packet.readS32(0, isLittleEndian)
                            val value = packet.readS16(4, isLittleEndian)
                            val type = packet.readU8(6)
                            val number = packet.readU8(7)

                            //println("JS_EVENT: time=$time, value=$value, type=$type, number=$number")

                            if (type hasFlags JS_EVENT_AXIS) {
                                val button = when (number) {
                                    0 -> GameButton.LX
                                    1 -> GameButton.LY
                                    2 -> GameButton.L2
                                    3 -> GameButton.RX
                                    4 -> GameButton.RY
                                    5 -> GameButton.R2
                                    6 -> GameButton.DPADX
                                    7 -> GameButton.DPADY
                                    else -> GameButton.BUTTON8
                                }
                                val fvalue = (value.toFloat() / 32767).clamp(-1f, +1f)
                                when (button) {
                                    GameButton.DPADX -> {
                                        buttonsPressure[GameButton.LEFT.index] = (fvalue < 0f).toInt().toFloat()
                                        buttonsPressure[GameButton.RIGHT.index] = (fvalue > 0f).toInt().toFloat()
                                    }
                                    GameButton.DPADY -> {
                                        buttonsPressure[GameButton.UP.index] = (fvalue < 0f).toInt().toFloat()
                                        buttonsPressure[GameButton.DOWN.index] = (fvalue > 0f).toInt().toFloat()
                                    }
                                    else -> {
                                        buttonsPressure[button.index] = when (button) {
                                            GameButton.LX, GameButton.RX -> GamepadInfo.withoutDeadRange(+fvalue)
                                            GameButton.LY, GameButton.RY -> GamepadInfo.withoutDeadRange(-fvalue)
                                            GameButton.L2, GameButton.R2 -> fvalue.convertRange(-1f, +1f, 0f, 1f)
                                            else -> fvalue
                                        }
                                    }
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
                            }
                            //println("$time, $type, $number, $value: ${buttons.toStringUnsigned(2)}, ${axes.slice(0 until maxAxes).toList()}")
                        } else {
                            Thread_sleep(10L)
                        }
                        readCount.incrementAndGet()
                        once.complete(Unit)
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    Thread_sleep(100L)
                }
            }
        }

        override fun close() {
            running = false
            dispatcher.close()
        }
    }
}
