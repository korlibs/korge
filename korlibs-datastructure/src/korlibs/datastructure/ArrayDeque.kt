package korlibs.datastructure

import korlibs.datastructure.internal.KdsInternalApi
import korlibs.math.ilog2
import kotlin.jvm.JvmOverloads

class ByteArrayDeque(val initialBits: Int = 10, val allowGrow: Boolean = true) {
    private var ring = RingBuffer(initialBits)
    private val tempBuffer = ByteArray(1024)

    var written: Long = 0; private set
    var read: Long = 0; private set
    val availableWriteWithoutAllocating get() = ring.availableWrite
    val availableRead get() = ring.availableRead
    val bufferSize get() = ring.totalSize

    @JvmOverloads
    fun writeHead(buffer: ByteArray, offset: Int = 0, size: Int = buffer.size - offset): Int {
        ensureWrite(size)
        val out = ring.writeHead(buffer, offset, size)
        if (out > 0) written += out
        return out
    }

    @JvmOverloads
    fun write(buffer: ByteArray, offset: Int = 0, size: Int = buffer.size - offset): Int {
        ensureWrite(size)
        val out = ring.write(buffer, offset, size)
        if (out > 0) written += out
        return out
    }

    @JvmOverloads
    fun read(buffer: ByteArray, offset: Int = 0, size: Int = buffer.size - offset): Int {
        val out = ring.read(buffer, offset, size)
        if (out > 0) read += out
        return out
    }

    fun skip(count: Int): Int {
        return ring.skip(count)
    }

    fun peek(buffer: ByteArray, offset: Int = 0, size: Int = buffer.size - offset): Int {
        return ring.peek(buffer, offset, size)
    }

    fun readByte(): Int = ring.readByte()
    fun writeByte(v: Int): Boolean {
        ensureWrite(1)
        return ring.writeByte(v)
    }

    @OptIn(KdsInternalApi::class)
    private fun ensureWrite(count: Int) {
        if (count <= ring.availableWrite) return
        if (!allowGrow) {
            val message = "Can't grow ByteArrayDeque. Need to write $count, but only ${ring.availableWrite} is available"
            println("ERROR: $message")
            error(message)
        }
        val minNewSize = ring.availableRead + count
        this.ring = RingBuffer(ilog2(minNewSize) + 2).also { it.write(ring) }
    }

    fun clear() {
        ring.clear()
    }

    val hasMoreToWrite get() = ring.availableWrite > 0
    val hasMoreToRead get() = ring.availableRead > 0
    fun readOne(): Byte {
        read(tempBuffer, 0, 1)
        return tempBuffer[0]
    }
    fun writeOne(value: Byte) {
        tempBuffer[0] = value
        write(tempBuffer, 0, 1)
    }

    override fun hashCode(): Int = ring.contentHashCode()
    override fun equals(other: Any?): Boolean = (other is ByteArrayDeque) && this.ring == other.ring
}

class ShortArrayDeque(val initialBits: Int = 10) {
    private var ring = ShortRingBuffer(initialBits)
    private val tempBuffer = ShortArray(1024)

    var written: Long = 0; private set
    var read: Long = 0; private set
    val availableWriteWithoutAllocating get() = ring.availableWrite
    val availableRead get() = ring.availableRead

    fun clone(): ShortArrayDeque {
        return ShortArrayDeque(initialBits).also { out ->
            out.ring = ring.clone()
            out.written = written
            out.read = read
        }
    }

    @JvmOverloads
    fun writeHead(buffer: ShortArray, offset: Int = 0, size: Int = buffer.size - offset): Int {
        val out = ensureWrite(size).ring.writeHead(buffer, offset, size)
        if (out > 0) written += out
        return out
    }

    @JvmOverloads
    fun write(buffer: ShortArray, offset: Int = 0, size: Int = buffer.size - offset): Int {
        val out = ensureWrite(size).ring.write(buffer, offset, size)
        if (out > 0) written += out
        return out
    }

    @JvmOverloads
    fun read(buffer: ShortArray, offset: Int = 0, size: Int = buffer.size - offset): Int {
        val out = ring.read(buffer, offset, size)
        if (out > 0) read += out
        return out
    }

    private fun ensureWrite(count: Int): ShortArrayDeque {
        if (count > ring.availableWrite) {
            val minNewSize = ring.availableRead + count
            val newBits = ilog2(minNewSize) + 2
            val newRing = ShortRingBuffer(newBits)
            while (ring.availableRead > 0) {
                val read = ring.read(tempBuffer, 0, tempBuffer.size)
                newRing.write(tempBuffer, 0, read)
            }
            this.ring = newRing
        }
        return this
    }

    fun clear() {
        ring.clear()
    }

    val hasMoreToWrite get() = ring.availableWrite > 0
    val hasMoreToRead get() = ring.availableRead > 0
    fun readOne(): Short {
        read(tempBuffer, 0, 1)
        return tempBuffer[0]
    }
    fun writeOne(value: Short) {
        tempBuffer[0] = value
        write(tempBuffer, 0, 1)
    }

    override fun hashCode(): Int = ring.contentHashCode()
    override fun equals(other: Any?): Boolean = (other is ShortArrayDeque) && this.ring == other.ring
}


class IntArrayDeque(val initialBits: Int = 10) {
    private var ring = IntRingBuffer(initialBits)
    private val tempBuffer = IntArray(1024)

    var written: Long = 0; private set
    var read: Long = 0; private set
    val availableWriteWithoutAllocating get() = ring.availableWrite
    val availableRead get() = ring.availableRead

    @JvmOverloads
    fun writeHead(buffer: IntArray, offset: Int = 0, size: Int = buffer.size - offset): Int {
        val out = ensureWrite(size).ring.writeHead(buffer, offset, size)
        if (out > 0) written += out
        return out
    }

    @JvmOverloads
    fun write(buffer: IntArray, offset: Int = 0, size: Int = buffer.size - offset): Int {
        val out = ensureWrite(size).ring.write(buffer, offset, size)
        if (out > 0) written += out
        return out
    }

    @JvmOverloads
    fun read(buffer: IntArray, offset: Int = 0, size: Int = buffer.size - offset): Int {
        val out = ring.read(buffer, offset, size)
        if (out > 0) read += out
        return out
    }

    private fun ensureWrite(count: Int): IntArrayDeque {
        if (count > ring.availableWrite) {
            val minNewSize = ring.availableRead + count
            val newBits = ilog2(minNewSize) + 2
            val newRing = IntRingBuffer(newBits)
            while (ring.availableRead > 0) {
                val read = ring.read(tempBuffer, 0, tempBuffer.size)
                newRing.write(tempBuffer, 0, read)
            }
            this.ring = newRing
        }
        return this
    }

    fun clear() {
        ring.clear()
    }

    val hasMoreToWrite get() = ring.availableWrite > 0
    val hasMoreToRead get() = ring.availableRead > 0
    fun readOne(): Int {
        read(tempBuffer, 0, 1)
        return tempBuffer[0]
    }
    fun writeOne(value: Int) {
        tempBuffer[0] = value
        write(tempBuffer, 0, 1)
    }

    override fun hashCode(): Int = ring.contentHashCode()
    override fun equals(other: Any?): Boolean = (other is IntArrayDeque) && this.ring == other.ring
}


class FloatArrayDeque(val initialBits: Int = 10) {
    private var ring = FloatRingBuffer(initialBits)
    private val tempBuffer = FloatArray(1024)

    var written: Long = 0; private set
    var read: Long = 0; private set
    val availableWriteWithoutAllocating get() = ring.availableWrite
    val availableRead get() = ring.availableRead

    @JvmOverloads
    fun writeHead(buffer: FloatArray, offset: Int = 0, size: Int = buffer.size - offset): Int {
        val out = ensureWrite(size).ring.writeHead(buffer, offset, size)
        if (out > 0) written += out
        return out
    }

    @JvmOverloads
    fun write(buffer: FloatArray, offset: Int = 0, size: Int = buffer.size - offset): Int {
        val out = ensureWrite(size).ring.write(buffer, offset, size)
        if (out > 0) written += out
        return out
    }

    @JvmOverloads
    fun read(buffer: FloatArray, offset: Int = 0, size: Int = buffer.size - offset): Int {
        val out = ring.read(buffer, offset, size)
        if (out > 0) read += out
        return out
    }

    private fun ensureWrite(count: Int): FloatArrayDeque {
        if (count > ring.availableWrite) {
            val minNewSize = ring.availableRead + count
            val newBits = ilog2(minNewSize) + 2
            val newRing = FloatRingBuffer(newBits)
            while (ring.availableRead > 0) {
                val read = ring.read(tempBuffer, 0, tempBuffer.size)
                newRing.write(tempBuffer, 0, read)
            }
            this.ring = newRing
        }
        return this
    }

    fun clear() {
        ring.clear()
    }

    val hasMoreToWrite get() = ring.availableWrite > 0
    val hasMoreToRead get() = ring.availableRead > 0
    fun readOne(): Float {
        read(tempBuffer, 0, 1)
        return tempBuffer[0]
    }
    fun writeOne(value: Float) {
        tempBuffer[0] = value
        write(tempBuffer, 0, 1)
    }

    override fun hashCode(): Int = ring.contentHashCode()
    override fun equals(other: Any?): Boolean = (other is FloatArrayDeque) && this.ring == other.ring
}
