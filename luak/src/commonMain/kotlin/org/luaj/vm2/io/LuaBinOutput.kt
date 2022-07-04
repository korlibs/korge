package org.luaj.vm2.io

import org.luaj.vm2.internal.*
import org.luaj.vm2.internal.arraycopy
import kotlin.math.*

abstract class LuaBinOutput {
    abstract fun write(value: Int)
    open fun write(b: ByteArray, i: Int, size: Int) { for (n in 0 until size) write(b[i + n].toInt() and 0xFF) }
    fun write(b: ByteArray) = write(b, 0, b.size)

    open fun writeByte(v: Int): Unit = write(v)
    open fun writeInt(x: Int): Unit {
        write((x ushr 24) and 0xFF)
        write((x ushr 16) and 0xFF)
        write((x ushr 8) and 0xFF)
        write((x ushr 0) and 0xFF)
    }
    open fun writeLong(v: Long): Unit {
        writeInt((v ushr 32).toInt())
        writeInt((v ushr 0).toInt())
    }

    open fun flush() = Unit
    open fun close() = Unit
}

open class ByteArrayLuaBinOutput(val initialCapacity: Int = 64) : LuaBinOutput() {
    var pos = 0
    var buf = ByteArray(initialCapacity)

    private fun ensure(size: Int) {
        if (pos + size >= buf.size) {
            buf = buf.copyOf(max(buf.size * 2 + 1, pos + size + 7))
        }
    }

    override fun write(value: Int) {
        ensure(1)
        buf[pos++] = value.toByte()
    }

    override fun write(b: ByteArray, i: Int, size: Int) {
        ensure(size)
        arraycopy(b, i, buf, pos, size)
        pos += size
    }

    fun size(): Int = pos
    fun toByteArray() = buf.copyOf(pos)
    override fun toString() = toByteArray().contentToString()
}
