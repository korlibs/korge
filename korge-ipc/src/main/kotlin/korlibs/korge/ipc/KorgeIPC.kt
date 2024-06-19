package korlibs.korge.ipc

import java.io.*
import java.nio.*
import java.nio.channels.*
import java.nio.file.*
import kotlin.reflect.*

class KorgeIPC(val path: String = System.getenv("KORGE_IPC") ?: DEFAULT_PATH) {
    init {
        println("KorgeIPC:$path")
    }

    companion object {
        val DEFAULT_PATH = "/tmp/KORGE_IPC"
    }
    val frame = KorgeFrameBuffer("$path.frame")
    val events = KorgeEventsBuffer("$path.events")

    val availableEvents get() = events.availableRead
    fun writeEvent(e: IPCEvent) = events.writeEvent(e)
    fun readEvent(e: IPCEvent = IPCEvent()): IPCEvent? = events.readEvent(e)
    fun setFrame(f: IPCFrame) = frame.setFrame(f)
    fun getFrame(): IPCFrame = frame.getFrame()
    fun getFrameId(): Int = frame.getFrameId()
}

data class IPCEvent(
    var timestamp: Long = System.currentTimeMillis(),
    var type: Int = 0,
    var p0: Int = 0,
    var p1: Int = 0,
    var p2: Int = 0,
    var p3: Int = 0,
) {
    fun setNow(): IPCEvent {
        timestamp = System.currentTimeMillis()
        return this
    }

    companion object {
        val RESIZE = 1

        val MOUSE_MOVE = 10
        val MOUSE_DOWN = 11
        val MOUSE_UP = 12
        val MOUSE_CLICK = 13

        val KEY_DOWN = 20
        val KEY_UP = 21
        val KEY_TYPE = 22
    }
}

class KorgeEventsBuffer(val path: String) {
    val channel = FileChannel.open(Path.of(path), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
    val HEAD_SIZE = 32
    val EVENT_SIZE = 32
    val MAX_EVENTS = 4096
    var buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0L, (HEAD_SIZE + EVENT_SIZE * MAX_EVENTS).toLong())

    init {
        File(path).deleteOnExit()
    }

    var readPos: Long by DelegateBufferLong(buffer, 0)
    var writePos: Long by DelegateBufferLong(buffer, 8)


    fun eventOffset(index: Long): Int = 32 + ((index % MAX_EVENTS).toInt() * 32)

    fun readEvent(index: Long, e: IPCEvent = IPCEvent()): IPCEvent {
        val pos = eventOffset(index)
        e.timestamp = buffer.getLong(pos + 0)
        e.type = buffer.getInt(pos + 8)
        e.p0 = buffer.getInt(pos + 12)
        e.p1 = buffer.getInt(pos + 16)
        e.p2 = buffer.getInt(pos + 20)
        e.p3 = buffer.getInt(pos + 24)
        return e
    }

    fun writeEvent(index: Long, e: IPCEvent) {
        val pos = eventOffset(index)
        buffer.putLong(pos + 0, e.timestamp)
        buffer.putInt(pos + 8, e.type)
        buffer.putInt(pos + 12, e.p0)
        buffer.putInt(pos + 16, e.p1)
        buffer.putInt(pos + 20, e.p2)
        buffer.putInt(pos + 24, e.p3)
    }

    fun reset() {
        readPos = 0L
        writePos = 0L
    }

    val availableRead: Int get() = (writePos - readPos).toInt()
    val availableWriteWithoutOverflow: Int get() = MAX_EVENTS - availableRead

    fun writeEvent(e: IPCEvent) {
        //println("EVENT: $e")
        writeEvent(writePos++, e)
    }

    fun readEvent(e: IPCEvent = IPCEvent()): IPCEvent? {
        if (readPos >= writePos) return null
        return readEvent(readPos++, e)
    }

    fun close() {
        channel.close()
    }

    class DelegateBufferLong(val buffer: ByteBuffer, val index: Int) {
        operator fun getValue(obj: Any, property: KProperty<*>): Long = buffer.getLong(index)
        operator fun setValue(obj: Any, property: KProperty<*>, value: Long) { buffer.putLong(index, value) }
    }

    class DelegateBufferInt(val buffer: ByteBuffer, val index: Int) {
        operator fun getValue(obj: Any, property: KProperty<*>): Int = buffer.getInt(index)
        operator fun setValue(obj: Any, property: KProperty<*>, value: Int) { buffer.putInt(index, value) }
    }
}

class IPCFrame(val id: Int, val width: Int, val height: Int, val pixels: IntArray = IntArray(width * height))

class KorgeFrameBuffer(val path: String) {
    val channel = FileChannel.open(Path.of(path), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
    var width: Int = 0
    var height: Int = 0
    var buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0L, 16 + 0)
    var ibuffer = buffer.asIntBuffer()

    init {
        File(path).deleteOnExit()
    }

    fun ensureSize(width: Int, height: Int) {
        if (this.width < width || this.height < height) {
            this.width = width
            this.height = height
            buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0L, (16 + (width * height * 4)).toLong())
            ibuffer = buffer.asIntBuffer()
        }
    }

    fun setFrame(frame: IPCFrame) {
        ensureSize(frame.width, frame.height)
        ibuffer.clear()
        ibuffer.put(frame.id)
        ibuffer.put(frame.width)
        ibuffer.put(frame.height)
        ibuffer.put(frame.pixels)
    }

    fun getFrameId(): Int {
        ibuffer.clear()
        return ibuffer.get()
    }

    fun getFrame(): IPCFrame {
        ibuffer.clear()
        val id = ibuffer.get()
        val width = ibuffer.get()
        val height = ibuffer.get()
        ensureSize(width, height)
        val pixels = IntArray(width * height)
        ibuffer.get(pixels)
        return IPCFrame(id, width, height, pixels)
    }

    fun close() {
        channel.close()
    }
}
