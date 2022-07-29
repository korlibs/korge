package com.soywiz.korvi.mpeg.util

import com.soywiz.kds.FastArrayList
import com.soywiz.kmem.Uint8Buffer
import com.soywiz.kmem.arraycopy
import com.soywiz.korvi.mpeg.byteLength
import com.soywiz.korvi.mpeg.length
import com.soywiz.korvi.mpeg.set

class BitBuffer(var bytes: Uint8Buffer, val mode: MODE = MODE.EXPAND) {
    var byteLength: Int = bytes.size
    var index: Int = 0
    var buffer: Uint8Buffer? = null

    constructor(size: Int, mode: MODE = MODE.EXPAND) : this(Uint8Buffer(size), mode) {
        byteLength = 0
    }

    fun resize(size: Int) {
        val newBytes = Uint8Buffer(size)
        if (this.byteLength != 0) {
            this.byteLength = kotlin.math.min(this.byteLength, size)
            arraycopy(this.bytes.b, 0, newBytes.b, 0, this.byteLength)
        }
        this.bytes = newBytes
        this.index = kotlin.math.min(this.index, this.byteLength shl 3)
    }

    fun evict(sizeNeeded: Int) {
        val bytePos = this.index shr 3
        val available = this.bytes.size - this.byteLength

        // If the current index is the write position, we can simply reset both
        // to 0. Also reset (and throw away yet unread data) if we won't be able
        // to fit the new data in even after a normal eviction.
        if (
            this.index == this.byteLength shl 3 ||
            sizeNeeded > available + bytePos // emergency evac
        ) {
            this.byteLength = 0
            this.index = 0
            return
        } else if (bytePos == 0) {
            // Nothing read yet - we can't evict anything
            return
        }

        // Some browsers don't support copyWithin() yet - we may have to do
        // it manually using set and a subarray
        arraycopy(this.bytes.b, bytePos, this.bytes.b, 0, this.byteLength - bytePos)
        //when {
        //    this.bytes.copyWithin -> this.bytes.copyWithin(0, bytePos, this.byteLength)
        //    else -> this.bytes.set(this.bytes.subarray(bytePos, this.byteLength))
        //}

        this.byteLength = this.byteLength - bytePos
        this.index -= bytePos shl 3
        return
    }

    fun write(buffers: FastArrayList<Uint8Buffer>): Int {
        var totalLength = 0
        val available = this.bytes.size - this.byteLength

        // Calculate total byte length
        for (i in 0 until buffers.length) {
            totalLength += buffers[i].byteLength
        }

        // Do we need to resize or evict?
        if (totalLength > available) {
            if (this.mode === MODE.EXPAND) {
                val newSize = kotlin.math.max(
                    this.bytes.size * 2,
                    totalLength - available
                )
                this.resize(newSize)
            } else {
                this.evict(totalLength)
            }
        }

        for (i in 0 until buffers.length) {
            this.appendSingleBuffer(buffers[i])
        }

        return totalLength
    }

    fun appendSingleBuffer(buffer: Uint8Buffer) {
        this.buffer = buffer

        this.bytes.set(buffer, this.byteLength)
        this.byteLength += buffer.size
    }

    fun findNextStartCode(): Int {
        for (i in ((this.index + 7) ushr 3) until this.byteLength) {
            if (i < 128) {
                //println("CHECK: $i, this.bytes[i + 0..3]=${this.bytes[i + 0]},${this.bytes[i + 1]},${this.bytes[i + 2]},${this.bytes[i + 3]}, byteLength=$byteLength")
            }
            if (
                this.bytes[i + 0] == 0x00 &&
                this.bytes[i + 1] == 0x00 &&
                this.bytes[i + 2] == 0x01
            ) {
                //println("SYNC")
                this.index = (i + 4) shl 3
                val code = this.bytes[i + 3]
                //println("findNextStartCode $i $code")
                return code
            }
        }
        this.index = (this.byteLength shl 3)
        return -1
    }

    fun findStartCode(code: Int): Int {
        var current = 0
        while (true) {
            current = this.findNextStartCode()
            if (current == code || current == -1) {
                return current
            }
        }
    }

    fun nextBytesAreStartCode(): Boolean {
        val i = (this.index + 7 shr 3)
        return (
            i >= this.byteLength || (
                this.bytes[i] == 0x00 &&
                    this.bytes[i + 1] == 0x00 &&
                    this.bytes[i + 2] == 0x01
                )
            )
    }

    fun peek(count: Int): Int {
        var count = count
        var offset = this.index
        var value = 0
        while (count != 0) {
            val currentByte = this.bytes[offset ushr 3]
            val remaining = 8 - (offset and 7) // remaining bits in byte
            val read = if (remaining < count) remaining else count // bits in this run
            val shift = remaining - read
            val mask = (0xff shr (8 - read))

            value = (value shl read) or ((currentByte and (mask shl shift)) shr shift)

            offset += read
            count -= read
        }

        return value
    }

    fun readBool(): Boolean = read(1) != 0

    fun read(count: Int): Int {
        val value = this.peek(count)
        this.index += count
        return value
    }

    fun skip(count: Int): Int {
        this.index += count
        return this.index
    }

    fun rewind(count: Int) {
        this.index = kotlin.math.max(this.index - count, 0)
    }

    fun has(count: Int): Boolean {
        return ((this.byteLength shl 3) - this.index) >= count
    }

    enum class MODE(val id: Int) {
        EVICT(1), EXPAND(2)
    }
}
