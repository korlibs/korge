package korlibs.korge.ipc

import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.channels.*
import java.nio.file.*

class KorgePacketRingBuffer(
    val meta: IntBuffer,
    val buffer: ByteBuffer
) {
    var readPos: Int
        set(value) { meta.put(0, value) }
        get() = meta[0]

    var writePos: Int
        set(value) { meta.put(1, value) }
        get() = meta[1]

    val availableRead get() = if (writePos < readPos) buffer.limit() - readPos + writePos else writePos - readPos
    val availableWrite get() = if (writePos < readPos) readPos - writePos else buffer.limit() - writePos + readPos

    fun read(): IPCPacket? = synchronized(this) {
        if (availableRead <= 0) return null
        buffer.position(readPos)
        if (buffer.remaining() < 4) buffer.position(0)
        val packetSize = buffer.getInt()
        if (buffer.remaining() < packetSize + 4) buffer.position(0)
        val packetType = buffer.getInt()
        val data = ByteArray(packetSize)
        buffer.get(data)
        IPCPacket(packetType, data).also { readPos = buffer.position() }
    }

    fun write(packet: IPCPacket) = synchronized(this) {
        var count = 0
        while (availableWrite < packet.data.size + 4) {
            read()
            count++
            if (count >= 10 * 1024) error("Can't allocate space for writing packet")
        }
        buffer.position(writePos)
        if (buffer.remaining() < 4) buffer.position(0)
        buffer.putInt(packet.data.size)
        if (buffer.remaining() < packet.data.size + 4) buffer.position(0)
        buffer.putInt(packet.type)
        buffer.put(packet.data)
        writePos = buffer.position()
    }
}

class KorgeIPCQueue(val path: String) : AutoCloseable {
    val channel = FileChannel.open(Path.of(path), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
    var width: Int = 0
    var height: Int = 0
    var buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0L, 1024 + 4 * 1024 * 1024)
    val ring = KorgePacketRingBuffer(buffer.slice(0, 1024).asIntBuffer(), buffer.slice(1024, 4 * 1024 * 1024))

    val availableRead get() = ring.availableRead
    val availableWrite get() = ring.availableWrite
    fun read(): IPCPacket? = ring.read()
    fun write(packet: IPCPacket) = ring.write(packet)

    override fun close() {
        channel.close()
    }
}
