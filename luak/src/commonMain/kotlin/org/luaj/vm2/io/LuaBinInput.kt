package org.luaj.vm2.io

import org.luaj.vm2.internal.*

abstract class LuaBinInput() {
    abstract fun read(): Int

    open fun read(out: ByteArray, off: Int, size: Int): Int {
        for (n in 0 until size) {
            val c = read()
            if (c >= 0) {
                out[off + n] = c.toByte()
            } else {
                return if (n == 0) -1 else n
            }
        }
        return size
    }
    fun read(out: ByteArray): Int = read(out, 0, out.size)

    open fun skip(n: Long): Long {
        for (v in 0L until n) if (read() < 0) return if (v == 0L) -1 else v
        return n
    }
    open fun available() = 0
    open fun markSupported(): Boolean = false
    open fun mark(value: Int): Unit = Unit
    open fun reset(): Unit = Unit
    open fun close() = Unit

    fun readByte(): Byte = read().let { c -> if (c >= 0) c.toByte() else throw EOFException() }
    fun readUnsignedByte(): Int = readByte().toInt() and 0xFF

    fun readFully(b: ByteArray, off: Int, len: Int) {
        var count: Int
        var n = 0
        while (n < len) {
            count = read(b, off + n, len - n)
            if (count < 0) throw EOFException()
            n += count
        }
    }
}

fun LuaBinInput(buf: ByteArray, start: Int = 0, size: Int = buf.size - start) = BytesLuaBinInput(buf, start, size)

open class BytesLuaBinInput(val buf: ByteArray, val start: Int = 0, val size: Int = buf.size - start) : LuaBinInput() {
    var offset = 0

    override fun read(): Int = if (offset < size) buf[start + offset++].toInt() and 0xFF else -1

    override fun skip(n: Long): Long {
        val rn = n.toInt()
        offset += rn
        return rn.toLong()
    }

    override fun available(): Int = size - offset

    private var mark = 0
    //override fun markSupported(): Boolean = true
    override fun markSupported(): Boolean = false
    override fun mark(value: Int) { mark = offset }
    override fun reset() { offset = mark }
}

class BufferedLuaBinInput(val base: LuaBinInput) : LuaBinInput() {
    init {
        TODO()
    }

    override fun skip(n: Long): Long = TODO()

    override fun markSupported(): Boolean = TODO()
    override fun available(): Int {
        TODO()
    }

    override fun mark(value: Int) {
        TODO()
    }

    override fun reset() {
        TODO()
    }

    override fun read(): Int = base.read()
}

fun LuaBinInput.buffered(): LuaBinInput = if (this is BytesLuaBinInput) this else BufferedLuaBinInput(this)
fun ByteArray.toLuaBinInput(start: Int = 0, size: Int = this.size - start) = BytesLuaBinInput(this)
