package com.soywiz.kds

import com.soywiz.kds.internal.*
import kotlin.jvm.*
import kotlin.math.*

class RingBuffer(bits: Int) : ByteRingBuffer(bits)

open class ByteRingBuffer(val bits: Int) {
    val totalSize = 1 shl bits
    private val mask = totalSize - 1
    private val buffer = ByteArray(totalSize)
    private var readPos = 0
    private var writePos = 0
    var availableWrite = totalSize; private set
    var availableRead = 0; private set

    @JvmOverloads
    fun writeHead(data: ByteArray, offset: Int = 0, size: Int = data.size - offset): Int {
        val toWrite = min(availableWrite, size)
        for (n in 0 until toWrite) {
            readPos = (readPos - 1) and mask
            buffer[readPos] = data[offset + size - n - 1]
        }
        availableRead += toWrite
        availableWrite -= toWrite
        return toWrite
    }

    @JvmOverloads
    fun write(data: ByteArray, offset: Int = 0, size: Int = data.size - offset): Int {
        val toWrite = min(availableWrite, size)
        for (n in 0 until toWrite) {
            buffer[writePos] = data[offset + n]
            writePos = (writePos + 1) and mask
        }
        availableRead += toWrite
        availableWrite -= toWrite
        return toWrite
    }

    @JvmOverloads
    fun read(data: ByteArray, offset: Int = 0, size: Int = data.size - offset): Int {
        val toRead = min(availableRead, size)
        for (n in 0 until toRead) {
            data[offset + n] = buffer[readPos]
            readPos = (readPos + 1) and mask
        }
        availableWrite += toRead
        availableRead -= toRead
        return toRead
    }

    fun readByte(): Int {
        if (availableRead <= 0) return -1
        val out = buffer[readPos].toInt() and 0xFF
        readPos = (readPos + 1) and mask
        availableRead--
        availableWrite++
        return out
    }

    fun writeByte(v: Int): Boolean {
        if (availableWrite <= 0) return false
        buffer[writePos] = v.toByte()
        writePos = (writePos + 1) and mask
        availableWrite--
        availableRead++
        return true
    }

    fun clear() {
        readPos = 0
        writePos = 0
        availableRead = 0
        availableWrite = totalSize
    }

    fun peek(offset: Int = 0) = buffer[(readPos + offset) and mask]
    override fun equals(other: Any?): Boolean = (other is ByteRingBuffer) && this.availableRead == other.availableRead && equaler(availableRead) { this.peek(it) == other.peek(it) }
    override fun hashCode(): Int = contentHashCode()
    fun contentHashCode(): Int = hashCoder(availableRead) { peek(it).toInt() }
}

class ShortRingBuffer(val bits: Int) {
    val totalSize = 1 shl bits
    private val mask = totalSize - 1
    private val buffer = ShortArray(totalSize)
    private var readPos = 0
    private var writePos = 0
    var availableWrite = totalSize; private set
    var availableRead = 0; private set

    @JvmOverloads
    fun writeHead(data: ShortArray, offset: Int = 0, size: Int = data.size - offset): Int {
        val toWrite = min(availableWrite, size)
        for (n in 0 until toWrite) {
            readPos = (readPos - 1) and mask
            buffer[readPos] = data[offset + size - n - 1]
        }
        availableRead += toWrite
        availableWrite -= toWrite
        return toWrite
    }

    @JvmOverloads
    fun write(data: ShortArray, offset: Int = 0, size: Int = data.size - offset): Int {
        val toWrite = min(availableWrite, size)
        for (n in 0 until toWrite) {
            buffer[writePos] = data[offset + n]
            writePos = (writePos + 1) and mask
        }
        availableRead += toWrite
        availableWrite -= toWrite
        return toWrite
    }

    @JvmOverloads
    fun read(data: ShortArray, offset: Int = 0, size: Int = data.size - offset): Int {
        val toRead = min(availableRead, size)
        for (n in 0 until toRead) {
            data[offset + n] = buffer[readPos]
            readPos = (readPos + 1) and mask
        }
        availableWrite += toRead
        availableRead -= toRead
        return toRead
    }

    private val temp = ShortArray(1)
    fun readOne(): Short {
        read(temp, 0, 1)
        return temp[0]
    }
    fun writeOne(value: Short) {
        temp[0] = value
        write(temp, 0, 1)
    }

    fun clear() {
        readPos = 0
        writePos = 0
        availableRead = 0
        availableWrite = totalSize
    }

    fun peek(offset: Int = 0) = buffer[(readPos + offset) and mask]
    override fun equals(other: Any?): Boolean = (other is ShortRingBuffer) && this.availableRead == other.availableRead && equaler(availableRead) { this.peek(it) == other.peek(it) }
    override fun hashCode(): Int = contentHashCode()
    fun contentHashCode(): Int = hashCoder(availableRead) { peek(it).toInt() }
}

class IntRingBuffer(val bits: Int) {
    val totalSize = 1 shl bits
    private val mask = totalSize - 1
    private val buffer = IntArray(totalSize)
    private var readPos = 0
    private var writePos = 0
    var availableWrite = totalSize; private set
    var availableRead = 0; private set

    @JvmOverloads
    fun writeHead(data: IntArray, offset: Int = 0, size: Int = data.size - offset): Int {
        val toWrite = min(availableWrite, size)
        for (n in 0 until toWrite) {
            readPos = (readPos - 1) and mask
            buffer[readPos] = data[offset + size - n - 1]
        }
        availableRead += toWrite
        availableWrite -= toWrite
        return toWrite
    }

    @JvmOverloads
    fun write(data: IntArray, offset: Int = 0, size: Int = data.size - offset): Int {
        val toWrite = min(availableWrite, size)
        for (n in 0 until toWrite) {
            buffer[writePos] = data[offset + n]
            writePos = (writePos + 1) and mask
        }
        availableRead += toWrite
        availableWrite -= toWrite
        return toWrite
    }

    @JvmOverloads
    fun read(data: IntArray, offset: Int = 0, size: Int = data.size - offset): Int {
        val toRead = min(availableRead, size)
        for (n in 0 until toRead) {
            data[offset + n] = buffer[readPos]
            readPos = (readPos + 1) and mask
        }
        availableWrite += toRead
        availableRead -= toRead
        return toRead
    }

    fun clear() {
        readPos = 0
        writePos = 0
        availableRead = 0
        availableWrite = totalSize
    }

    fun peek(offset: Int = 0) = buffer[(readPos + offset) and mask]
    override fun equals(other: Any?): Boolean = (other is IntRingBuffer) && this.availableRead == other.availableRead && equaler(availableRead) { this.peek(it) == other.peek(it) }
    override fun hashCode(): Int = contentHashCode()
    fun contentHashCode(): Int = hashCoder(availableRead) { peek(it).toInt() }
}

class FloatRingBuffer(val bits: Int) {
    val totalSize = 1 shl bits
    private val mask = totalSize - 1
    private val buffer = FloatArray(totalSize)
    private var readPos = 0
    private var writePos = 0
    var availableWrite = totalSize; private set
    var availableRead = 0; private set

    @JvmOverloads
    fun writeHead(data: FloatArray, offset: Int = 0, size: Int = data.size - offset): Int {
        val toWrite = min(availableWrite, size)
        for (n in 0 until toWrite) {
            readPos = (readPos - 1) and mask
            buffer[readPos] = data[offset + size - n - 1]
        }
        availableRead += toWrite
        availableWrite -= toWrite
        return toWrite
    }

    @JvmOverloads
    fun write(data: FloatArray, offset: Int = 0, size: Int = data.size - offset): Int {
        val toWrite = min(availableWrite, size)
        for (n in 0 until toWrite) {
            buffer[writePos] = data[offset + n]
            writePos = (writePos + 1) and mask
        }
        availableRead += toWrite
        availableWrite -= toWrite
        return toWrite
    }

    @JvmOverloads
    fun read(data: FloatArray, offset: Int = 0, size: Int = data.size - offset): Int {
        val toRead = min(availableRead, size)
        for (n in 0 until toRead) {
            data[offset + n] = buffer[readPos]
            readPos = (readPos + 1) and mask
        }
        availableWrite += toRead
        availableRead -= toRead
        return toRead
    }

    fun clear() {
        readPos = 0
        writePos = 0
        availableRead = 0
        availableWrite = totalSize
    }

    fun peek(offset: Int = 0) = buffer[(readPos + offset) and mask]
    override fun equals(other: Any?): Boolean = (other is FloatRingBuffer) && this.availableRead == other.availableRead && equaler(availableRead) { this.peek(it) == other.peek(it) }
    override fun hashCode(): Int = contentHashCode()
    fun contentHashCode(): Int = hashCoder(availableRead) { peek(it).toBits() }
}
