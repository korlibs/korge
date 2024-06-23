package korlibs.korge.ipc

import java.io.*
import java.nio.*
import java.nio.channels.*
import java.nio.file.*
import kotlin.reflect.*

data class IPCOldEvent(
    var timestamp: Long = System.currentTimeMillis(),
    var type: Int = 0,
    var p0: Int = 0,
    var p1: Int = 0,
    var p2: Int = 0,
    var p3: Int = 0,
) {
    fun setNow(): IPCOldEvent {
        timestamp = System.currentTimeMillis()
        return this
    }

    companion object {
        val RESIZE = 1
        val BRING_BACK = 2
        val BRING_FRONT = 3

        val MOUSE_MOVE = 10
        val MOUSE_DOWN = 11
        val MOUSE_UP = 12
        val MOUSE_CLICK = 13

        val KEY_DOWN = 20
        val KEY_UP = 21
        val KEY_TYPE = 22
    }
}

class KorgeOldEventsBuffer(val path: String) {
    val channel = FileChannel.open(Path.of(path), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
    val HEAD_SIZE = 32
    val EVENT_SIZE = 32
    val MAX_EVENTS = 4096
    var buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0L, (HEAD_SIZE + EVENT_SIZE * MAX_EVENTS).toLong())

    init {
        //File(path).deleteOnExit()
    }

    var readPos: Long by DelegateBufferLong(buffer, 0)
    var writePos: Long by DelegateBufferLong(buffer, 8)


    fun eventOffset(index: Long): Int = 32 + ((index % MAX_EVENTS).toInt() * 32)

    fun readEvent(index: Long, e: IPCOldEvent = IPCOldEvent()): IPCOldEvent {
        val pos = eventOffset(index)
        e.timestamp = buffer.getLong(pos + 0)
        e.type = buffer.getInt(pos + 8)
        e.p0 = buffer.getInt(pos + 12)
        e.p1 = buffer.getInt(pos + 16)
        e.p2 = buffer.getInt(pos + 20)
        e.p3 = buffer.getInt(pos + 24)
        return e
    }

    fun writeEvent(index: Long, e: IPCOldEvent) {
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

    fun writeEvent(e: IPCOldEvent) {
        //println("EVENT: $e")
        writeEvent(writePos++, e)
    }

    fun readEvent(e: IPCOldEvent = IPCOldEvent()): IPCOldEvent? {
        if (readPos >= writePos) return null
        return readEvent(readPos++, e)
    }

    fun close() {
        channel.close()
    }

    fun delete() {
        close()
        File(path).delete()
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
