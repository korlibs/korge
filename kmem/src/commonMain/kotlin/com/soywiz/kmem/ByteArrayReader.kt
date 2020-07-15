package com.soywiz.kmem

class ByteArrayReader(val data: ByteArray, val start: Int, val size: Int = 0) {
    private var offset = start
    val remaining get() = size - offset
    val hasMore get() = remaining > 0

    private fun <T> move(count: Int, callback: ByteArray.(Int) -> T): T {
        val res = callback(data, this.offset)
        this.offset += count
        return res
    }

    fun u8() = move(1) { readU8(it) }
    fun s8() = move(1) { readS8(it) }

    fun u16(little: Boolean) = move(2) { readU16(it, little) }
    fun s16(little: Boolean) = move(2) { readS16(it, little) }
    fun u16LE() = move(2) { readU16LE(it) }
    fun s16LE() = move(2) { readS16LE(it) }
    fun u16BE() = move(2) { readU16BE(it) }
    fun s16BE() = move(2) { readS16BE(it) }

    fun u24(little: Boolean) = move(3) { readU24(it, little) }
    fun s24(little: Boolean) = move(3) { readS24(it, little) }
    fun u24LE() = move(3) { readU24LE(it) }
    fun s24LE() = move(3) { readS24LE(it) }
    fun u24BE() = move(3) { readU24BE(it) }
    fun s24BE() = move(3) { readS24BE(it) }

    fun u32(little: Boolean) = move(4) { readU32(it, little) }
    fun s32(little: Boolean) = move(4) { readS32(it, little) }
    fun u32LE() = move(4) { readU32LE(it) }
    fun s32LE() = move(4) { readS32LE(it) }
    fun u32BE() = move(4) { readU32BE(it) }
    fun s32BE() = move(4) { readS32BE(it) }

    fun f16(little: Boolean) = move(2) { readF16(it, little) }
    fun f16LE() = move(2) { readF16LE(it) }
    fun f16BE() = move(2) { readF16BE(it) }
    fun f32(little: Boolean) = move(4) { readF32(it, little) }
    fun f32LE() = move(4) { readF32LE(it) }
    fun f32BE() = move(4) { readF32BE(it) }
    fun f64(little: Boolean) = move(8) { readF64(it, little) }
    fun f64LE() = move(8) { readF64LE(it) }
    fun f64BE() = move(8) { readF64BE(it) }
}

inline class ByteArrayReaderLE(val bar: ByteArrayReader)

val ByteArrayReaderLE.size get() = bar.size
val ByteArrayReaderLE.remaining get() = bar.remaining
val ByteArrayReaderLE.hasMore get() = bar.hasMore
fun ByteArrayReaderLE.u8() = bar.u8()
fun ByteArrayReaderLE.s8() = bar.s8()
fun ByteArrayReaderLE.u16() = bar.u16LE()
fun ByteArrayReaderLE.s16() = bar.s16LE()
fun ByteArrayReaderLE.u24() = bar.u24LE()
fun ByteArrayReaderLE.s24() = bar.s24LE()
fun ByteArrayReaderLE.u32() = bar.u32LE()
fun ByteArrayReaderLE.s32() = bar.s32LE()
fun ByteArrayReaderLE.f16() = bar.f16LE()
fun ByteArrayReaderLE.f32() = bar.f32LE()
fun ByteArrayReaderLE.f64() = bar.f64LE()

inline class ByteArrayReaderBE(val bar: ByteArrayReader)

val ByteArrayReaderBE.size get() = bar.size
val ByteArrayReaderBE.remaining get() = bar.remaining
val ByteArrayReaderBE.hasMore get() = bar.hasMore
fun ByteArrayReaderBE.u8() = bar.u8()
fun ByteArrayReaderBE.s8() = bar.s8()
fun ByteArrayReaderBE.u16() = bar.u16BE()
fun ByteArrayReaderBE.s16() = bar.s16BE()
fun ByteArrayReaderBE.u24() = bar.u24BE()
fun ByteArrayReaderBE.s24() = bar.s24BE()
fun ByteArrayReaderBE.u32() = bar.u32BE()
fun ByteArrayReaderBE.s32() = bar.s32BE()
fun ByteArrayReaderBE.f16() = bar.f16BE()
fun ByteArrayReaderBE.f32() = bar.f32BE()
fun ByteArrayReaderBE.f64() = bar.f64BE()

fun ByteArray.reader(offset: Int = 0, size: Int = this.size) = ByteArrayReader(this, offset, size)
fun ByteArray.readerLE(offset: Int = 0, size: Int = this.size) = ByteArrayReaderLE(reader(offset, size))
fun ByteArray.readerBE(offset: Int = 0, size: Int = this.size) = ByteArrayReaderBE(reader(offset, size))

fun <T> ByteArray.read(offset: Int = 0, size: Int = this.size, callback: ByteArrayReader.() -> T): T =
    callback(reader(offset, size))

fun <T> ByteArray.readLE(offset: Int = 0, size: Int = this.size, callback: ByteArrayReaderLE.() -> T): T =
    callback(readerLE(offset, size))

fun <T> ByteArray.readBE(offset: Int = 0, size: Int = this.size, callback: ByteArrayReaderBE.() -> T): T =
    callback(readerBE(offset, size))
