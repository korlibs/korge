package com.soywiz.kmem

import kotlin.math.max

/**
 * Analogous to [StringBuilder] but for [ByteArray]. Allows to [append] values to end calling [toByteArray].
 * Provides some methods like [s16LE] or [f32BE] to append specific bit representations easily.
 */
class ByteArrayBuilder(var data: ByteArray, size: Int = data.size, val allowGrow: Boolean = true) {
    constructor(initialCapacity: Int = 4096) : this(ByteArray(initialCapacity), 0)

    private var _size: Int = size
    var size: Int get() = _size
        set(value) {
            val oldPosition = _size
            val newPosition = value
            ensure(newPosition)
            _size = newPosition
            if (newPosition > oldPosition) {
                arrayfill(data, 0, oldPosition, newPosition)
            }
        }

    private fun ensure(expected: Int) {
        if (data.size < expected) {
            if (!allowGrow) throw RuntimeException("ByteArrayBuffer configured to not grow!")
            data = data.copyOf(max(expected, (data.size + 7) * 5))
        }
    }

    private inline fun <T> prepare(count: Int, callback: () -> T): T {
        ensure(_size + count)
        return callback().also { _size += count }
    }

    fun append(array: ByteArray, offset: Int = 0, len: Int = array.size - offset) {
        prepare(len) {
            arraycopy(array, offset, this.data, _size, len)
        }
    }

    fun append(v: Byte) = this.apply { prepare(1) { data[_size] = v } }

    fun append(vararg v: Byte) = append(v)
    fun append(vararg v: Int) = this.apply {
        prepare(v.size) {
            for (n in 0 until v.size) this.data[this._size + n] = v[n].toByte()
        }
    }

    fun appendByte(v: Int) = this.apply { prepare(1) { data[_size] = v.toByte() } }

    fun s8(v: Int) = appendByte(v)

    fun s16(v: Int, little: Boolean) = this.apply { prepare(2) { data.write16(_size, v, little) } }
    fun s16LE(v: Int) = this.apply { prepare(2) { data.write16LE(_size, v) } }
    fun s16BE(v: Int) = this.apply { prepare(2) { data.write16BE(_size, v) } }

    fun s24(v: Int, little: Boolean) = this.apply { prepare(3) { data.write24(_size, v, little) } }
    fun s24LE(v: Int) = this.apply { prepare(3) { data.write24LE(_size, v) } }
    fun s24BE(v: Int) = this.apply { prepare(3) { data.write24BE(_size, v) } }

    fun s32(v: Int, little: Boolean) = this.apply { prepare(4) { data.write32(_size, v, little) } }
    fun s32LE(v: Int) = this.apply { prepare(4) { data.write32LE(_size, v) } }
    fun s32BE(v: Int) = this.apply { prepare(4) { data.write32BE(_size, v) } }

    fun f16(v: Float16, little: Boolean) = this.apply { prepare(2) { data.writeF16(_size, v, little) } }
    fun f16LE(v: Float16) = this.apply { prepare(2) { data.writeF16LE(_size, v) } }
    fun f16BE(v: Float16) = this.apply { prepare(2) { data.writeF16BE(_size, v) } }

    fun f32(v: Float, little: Boolean) = this.apply { prepare(4) { data.writeF32(_size, v, little) } }
    fun f32LE(v: Float) = this.apply { prepare(4) { data.writeF32LE(_size, v) } }
    fun f32BE(v: Float) = this.apply { prepare(4) { data.writeF32BE(_size, v) } }

    fun f64(v: Double, little: Boolean) = this.apply { prepare(8) { data.writeF64(_size, v, little) } }
    fun f64LE(v: Double) = this.apply { prepare(8) { data.writeF64LE(_size, v) } }
    fun f64BE(v: Double) = this.apply { prepare(8) { data.writeF64BE(_size, v) } }

    fun clear() {
        _size = 0
    }

    fun toByteArray(): ByteArray = data.copyOf(_size)
}

inline class ByteArrayBuilderLE(val bab: ByteArrayBuilder)

val ByteArrayBuilderLE.size get() = bab.size
fun ByteArrayBuilderLE.append(array: ByteArray, offset: Int = 0, len: Int = array.size - offset) = bab.append(array, offset, len)
fun ByteArrayBuilderLE.append(v: Byte) = bab.append(v)
fun ByteArrayBuilderLE.appendByte(v: Int) = bab.appendByte(v)
fun ByteArrayBuilderLE.append(vararg v: Byte) = bab.append(*v)
fun ByteArrayBuilderLE.append(vararg v: Int) = bab.append(*v)
fun ByteArrayBuilderLE.s8(v: Int) = bab.s8(v)
fun ByteArrayBuilderLE.s16(v: Int) = bab.s16LE(v)
fun ByteArrayBuilderLE.s24(v: Int) = bab.s24LE(v)
fun ByteArrayBuilderLE.s32(v: Int) = bab.s32LE(v)
fun ByteArrayBuilderLE.f16(v: Float16) = bab.f16LE(v)
fun ByteArrayBuilderLE.f32(v: Float) = bab.f32LE(v)
fun ByteArrayBuilderLE.f64(v: Double) = bab.f64LE(v)
fun ByteArrayBuilderLE.clear() = bab.clear()
fun ByteArrayBuilderLE.toByteArray(): ByteArray = bab.toByteArray()

inline class ByteArrayBuilderBE(val bab: ByteArrayBuilder)

val ByteArrayBuilderBE.size get() = bab.size
fun ByteArrayBuilderBE.append(array: ByteArray, offset: Int = 0, len: Int = array.size - offset) = bab.append(array, offset, len)
fun ByteArrayBuilderBE.append(v: Byte) = bab.append(v)
fun ByteArrayBuilderBE.appendByte(v: Int) = bab.appendByte(v)
fun ByteArrayBuilderBE.append(vararg v: Byte) = bab.append(*v)
fun ByteArrayBuilderBE.append(vararg v: Int) = bab.append(*v)
fun ByteArrayBuilderBE.s8(v: Int) = bab.s8(v)
fun ByteArrayBuilderBE.s16(v: Int) = bab.s16BE(v)
fun ByteArrayBuilderBE.s24(v: Int) = bab.s24BE(v)
fun ByteArrayBuilderBE.s32(v: Int) = bab.s32BE(v)
fun ByteArrayBuilderBE.f16(v: Float16) = bab.f16BE(v)
fun ByteArrayBuilderBE.f32(v: Float) = bab.f32BE(v)
fun ByteArrayBuilderBE.f64(v: Double) = bab.f64BE(v)
fun ByteArrayBuilderBE.clear() = bab.clear()
fun ByteArrayBuilderBE.toByteArray(): ByteArray = bab.toByteArray()

/** Analogous to [buildString] but for [ByteArray] */
inline fun buildByteArray(capacity: Int = 4096, callback: ByteArrayBuilder.() -> Unit): ByteArray =
    ByteArrayBuilder(capacity).apply(callback).toByteArray()

/** Analogous to [buildString] but for [ByteArray] (Provides shortcuts for writing Little Endian bit values) */
inline fun buildByteArrayLE(capacity: Int = 4096, callback: ByteArrayBuilderLE.() -> Unit): ByteArray =
    ByteArrayBuilderLE(ByteArrayBuilder(capacity)).apply(callback).toByteArray()

/** Analogous to [buildString] but for [ByteArray] (Provides shortcuts for writing Big Endian bit values) */
inline fun buildByteArrayBE(capacity: Int = 4096, callback: ByteArrayBuilderBE.() -> Unit): ByteArray =
    ByteArrayBuilderBE(ByteArrayBuilder(capacity)).apply(callback).toByteArray()
