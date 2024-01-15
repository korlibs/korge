package korlibs.datastructure

import korlibs.datastructure.internal.memory.Memory.arraycopy
import korlibs.datastructure.lock.NonRecursiveLock
import kotlin.math.min

class ChunkedByteDeque {
    private val lock = NonRecursiveLock()
    private val chunks = Deque<ByteArray>()
    private var chunkPos: Int = 0
    var availableRead: Int = 0; private set

    private fun writeFullNoCopy(data: ByteArray) {
        lock {
            chunks.add(data)
            availableRead += data.size
        }
    }

    fun write(data: ByteArray, offset: Int = 0, size: Int = data.size - offset) {
        writeFullNoCopy(data.copyOfRange(offset, offset + size))
    }

    // @TODO: Optimize this by using some kind of Deque or ByteBuffer instead of a normal ByteArray
    fun write(byte: Int) {
        writeFullNoCopy(byteArrayOf(byte.toByte()))
    }

    private fun ByteArray.consumeChunkSize(size: Int) {
        val chunk = this
        lock {
            chunkPos += size
            availableRead -= size
            if (chunkPos >= chunk.size) { chunks.removeFirst(); chunkPos = 0 }
        }
    }

    fun read(data: ByteArray, offset: Int = 0, size: Int = data.size - offset): Int {
        if (offset < 0 || offset + size > data.size) throw IndexOutOfBoundsException()
        var coffset = offset
        var pending = size
        var readTotal = 0
        while (pending > 0) {
            val chunk = lock { if (chunks.isNotEmpty()) chunks.first else null } ?: break
            val availableInChunk = chunk.size - chunkPos
            val toCopy = min(availableInChunk, pending)
            if (toCopy <= 0) break
            arraycopy(chunk, chunkPos, data, coffset, toCopy)
            coffset += toCopy
            pending -= toCopy
            readTotal += toCopy
            chunk.consumeChunkSize(toCopy)
        }
        return readTotal
    }

    fun read(): Int {
        val chunk = lock { if (chunks.isNotEmpty()) chunks.first else null } ?: return -1
        return chunk[chunkPos].also { chunk.consumeChunkSize(1)}.toInt() and 0xFF
    }

    fun read(count: Int): ByteArray = ByteArray(count).let { it.copyOf(read(it)) }
}
