package korlibs.korge.gradle.util

import java.io.*

class ByteArraySlice(val ba: ByteArray, val pos: Int = 0, val size: Int = ba.size - pos) {
    fun sliceRange(range: IntRange): ByteArraySlice {
        return ByteArraySlice(ba, pos + range.first, range.last - range.first - 1)
    }
    val length: Int get() = size
    operator fun get(index: Int): Byte = ba[pos + index]
    fun sliceArray(range: IntRange): ByteArray {
        return ba.sliceArray((pos + range.first) .. (pos + range.last))
    }

    override fun toString(): String= "ByteArraySlice[$size]"
}

class ByteArraySimpleInputStream(
    val data: ByteArraySlice,
    var pos: Int = 0
) {
    val available: Int get() = length - pos
    val length: Int get() = data.size

    fun read(): Int {
        if (pos >= length) return -1
        return data[pos++].toInt() and 0xFF
    }

    fun readU8(): Int {
        val v = this.read()
        if (v < 0) error("Can't read byte at $pos in $data")
        return v
    }

    fun readU16LE(): Int {
        val v0 = readU8()
        val v1 = readU8()
        return (v0 shl 0) or (v1 shl 8)
    }

    fun readS16LE(): Int = (readU16LE() shl 16) shr 16

    fun skip(count: Int): Int {
        val oldPos = pos
        pos += count
        return oldPos
    }

    fun readS32LE(): Int {
        val v0 = readU8()
        val v1 = readU8()
        val v2 = readU8()
        val v3 = readU8()
        return (v0 shl 0) or (v1 shl 8) or (v2 shl 16) or (v3 shl 24)
    }

    fun readU32LE(): Long {
        return readS32LE().toLong() and 0xFFFFFFFFL
    }

    fun readStream(count: Int): ByteArraySimpleInputStream {
        val pos = skip(count)
        return ByteArraySimpleInputStream(data.sliceRange(pos until (pos + count)), 0)
    }

    fun readBytes(count: Int): ByteArray {
        val start = skip(count)
        return data.sliceArray(start until (start + count))
    }
}

fun InputStream.readU8(): Int {
    val v = this.read()
    if (v < 0) error("Can't read byte")
    return v
}

fun InputStream.readU16LE(): Int {
    val v0 = readU8()
    val v1 = readU8()
    return (v0 shl 0) or (v1 shl 8)
}

fun InputStream.readS32LE(): Int {
    val v0 = readU8()
    val v1 = readU8()
    val v2 = readU8()
    val v3 = readU8()
    return (v0 shl 0) or (v1 shl 8) or (v2 shl 16) or (v3 shl 24)
}

fun InputStream.readU32LE(): Long {
    return readS32LE().toLong() and 0xFFFFFFFFL
}

