@file:Suppress("PackageDirectoryMismatch")

package korlibs.memory.arrays

import korlibs.encoding.*
import kotlin.test.*

class TypedArraysTest {
    @Test
    fun test() {
        val data = Int8Array(10)
        val data2 = data.buffer.int8Array(1)
        for (n in 0 until 10) data[n] = (1 + n).toByte()
        for (n in 0 until 10) assertEquals((1 + n).toByte(), data[n])
        for (n in 0 until 9) assertEquals((2 + n).toByte(), data2[n])
        assertEquals(0, data.byteOffset)
        assertEquals(10, data.byteLength)
        assertEquals(10, data.length)
        assertEquals(1, data2.byteOffset)
        assertEquals(9, data2.byteLength)
        assertEquals(9, data2.length)
    }

    @Test
    fun test2() {
        val data = Int8Array(16)
        val data2 = Int8Array(data.buffer, 4 + 3).subarray(2)
        val data3 = data.buffer.int16Array(6 + 2, 3)
        val dataView = DataView(data.buffer, 4, 6)
        data3[0] = 10
        data3[1] = 12
        data2[2] = 7
        assertEquals("00000000000000000a000c0700000000", data.bytes.hex)
        assertEquals(1804, data3[1])
        assertEquals("0/16", data.info)
        assertEquals("9/7", data2.info)
        assertEquals("8/6", data3.info)
        assertEquals(0, dataView.getS32LE(0))
        assertEquals(167772160, dataView.getS32LE(1))
        assertEquals(655360, dataView.getS32LE(2))
    }

    @Test
    fun test3() {
        val data = Int8Array(80)
        val view = data.buffer.dataView(1)
        var offset = 0
        fun offset(size: Int) = offset.also { offset += size }
        view.setU8(offset(1), -1)
        view.setS8(offset(1), -2)
        view.setU16LE(offset(2), -3)
        view.setS16LE(offset(2), -4)
        view.setU32LE(offset(4), (-5).toUInt())
        view.setS32LE(offset(4), -6)
        view.setS64LE(offset(8), -7)
        view.setF32LE(offset(4), -8f)
        view.setF64LE(offset(8), -9.0)
        view.setU16BE(offset(2), -10)
        view.setS16BE(offset(2), -11)
        view.setU32BE(offset(4), (-12).toUInt())
        view.setS32BE(offset(4), -13)
        view.setS64BE(offset(8), -14)
        view.setF32BE(offset(4), -15f)
        view.setF64BE(offset(8), -16.0)

        assertEquals("00fffefdfffcfffbfffffffafffffff9ffffffffffffff000000c100000000000022c0fff6fff5fffffff4fffffff3fffffffffffffff2c1700000c03000000000000000000000000000000000000000", data.bytes.hex)
        offset = 0
        assertEquals(255, view.getU8(offset(1)))
        assertEquals(-2, view.getS8(offset(1)))
        assertEquals(65533, view.getU16LE(offset(2)))
        assertEquals(-4, view.getS16LE(offset(2)))
        assertEquals(4294967291L.toInt(), view.getU32LE(offset(4)).toLong().toInt())
        assertEquals(-6, view.getS32LE(offset(4)))
        assertEquals(-7, view.getS64LE(offset(8)))
        assertEquals(-8f, view.getF32LE(offset(4)))
        assertEquals(-9.0, view.getF64LE(offset(8)))
        assertEquals(65526, view.getU16BE(offset(2)))
        assertEquals(-11, view.getS16BE(offset(2)))
        assertEquals(-12, view.getU32BE(offset(4)).toLong().toInt())
        assertEquals(-13, view.getS32BE(offset(4)))
        assertEquals(-14, view.getS64BE(offset(8)))
        assertEquals(-15f, view.getF32BE(offset(4)))
        assertEquals(-16.0, view.getF64BE(offset(8)))
    }

    @Test
    fun testLong() {
        val data = Int8Array(16)
        val view = data.buffer.dataView(0)
        view.setS64LE(0, 1L)
        view.setS64BE(8, 1L)
        assertEquals(1L, view.getS64LE(0))
        assertEquals(1L, view.getS64BE(8))
        assertEquals("01000000000000000000000000000001", data.bytes.hex)
    }

    @Test
    fun testUIntClamped() {
        val data = Uint8ClampedArray(7)
        data[0] = 1000
        data[1] = 255
        data[2] = 254
        data[3] = 10
        data[4] = 0
        data[5] = -100
        data[6] = -1000
        assertEquals("fffffe0a000000", Int8Array(data.buffer).bytes.hex)
    }

    @Test
    fun testUInt8() {
        val data = Uint8Array(7)
        data[0] = 1000
        data[1] = 255
        data[2] = 254
        data[3] = 10
        data[4] = 0
        data[5] = -100
        data[6] = -1000
        assertEquals("e8fffe0a009c18", Int8Array(data.buffer).bytes.hex)
    }

    @Test
    fun testInt64Array() {
        val values = Int64Array(2)
        val data = DataView(values.buffer)
        val v0 = -1L
        val v1 = 1100277060986L
        values[0] = v0
        assertEquals(v0, values[0])
        assertEquals(v0, data.getS64LE(0))
        values[1] = v1
        assertEquals(v1, values[1])
        assertEquals(v1, data.getS64LE(8))
    }

    @Test
    fun testAs() {
        val values = Int64Array(2)
        val ints = values.asInt8Array().subarray(4).asInt32Array()
        ints[1] = -2
        ints[2] = -1
        assertEquals(0L, values[0])
        assertEquals(-2L, values[1])
    }

    @Test
    fun testSet() {
        val values = Int64Array(2)
        val ints = values.asInt8Array().subarray(4).asInt32Array()
        ints[1] = -2
        ints[2] = -1
        assertEquals(0L, values[0])
        assertEquals(-2L, values[1])
    }

    @Test
    fun testArrays() {
        val buffer = ArrayBuffer(24)
        val s8 = Int8Array(buffer, 1)
        val s16 = Int16Array(buffer, 2)
        val s32 = Int32Array(buffer, 4)
        val s64 = Int64Array(buffer, 8)
        val f32 = Float32Array(buffer, 4)
        val f64 = Float64Array(buffer, 0)
        f64[0] = 1.0
        s64[1] = 2L
        f32[2] = 3f
        s32[3] = 4
        s16[7] = 5
        s8[9] = 6
        assertEquals("000000000000f03f00000600000040400500000000000000", buffer.int8Array().toByteArray().hex)
    }

    @Test
    fun testExtract() {
        assertEquals(ubyteArrayOf(1u, 2u, 3u, 4u).toList(), ubyteArrayOf(1u, 2u, 3u, 4u).toUint8ClampedArray().toUByteArray().toList())
        assertEquals(ubyteArrayOf(1u, 2u, 3u, 4u).toList(), ubyteArrayOf(1u, 2u, 3u, 4u).toUint8Array().toUByteArray().toList())
        assertEquals(ushortArrayOf(1u, 2u, 3u, 4u).toList(), ushortArrayOf(1u, 2u, 3u, 4u).toUint16Array().toUShortArray().toList())
        assertEquals(byteArrayOf(1, 2, 3, 4).toList(), byteArrayOf(1, 2, 3, 4).toInt8Array().toByteArray().toList())
        assertEquals(shortArrayOf(1, 2, 3, 4).toList(), shortArrayOf(1, 2, 3, 4).toInt16Array().toShortArray().toList())
        assertEquals(intArrayOf(1, 2, 3, 4).toList(), intArrayOf(1, 2, 3, 4).toInt32Array().toIntArray().toList())
        assertEquals(longArrayOf(1, 2, 3, 4).toList(), longArrayOf(1, 2, 3, 4).toInt64Array().toLongArray().toList())
        assertEquals(floatArrayOf(1f, 2f, 3f, 4f).toList(), floatArrayOf(1f, 2f, 3f, 4f).toFloat32Array().toFloatArray().toList())
        assertEquals(doubleArrayOf(1.0, 2.0, 3.0, 4.0).toList(), doubleArrayOf(1.0, 2.0, 3.0, 4.0).toFloat64Array().toDoubleArray().toList())
    }

    @Test
    fun testCopy() {
        val src = Int8Array(4, direct = true) { (10 + it).toByte() }
        val dst = ArrayBufferDirect(16).int16Array()
        dst.set(src.subarray(1), 3)
        assertEquals("0000000000000b0c0d00000000000000", dst.asInt8Array().toByteArray().hex)
    }

    @Test
    fun testClone() {
        run {
            val dataA = Int8Array(4) { (10 + it).toByte() }.subarray(1)
            val dataB = dataA.clone()
            dataA[1] = -22
            dataB[1] = -11
            assertEquals("0bea0d", dataA.bytes.hex)
            assertEquals("0bf50d", dataB.bytes.hex)
        }
        run {
            val dataA = Int16Array(4) { (10 + it).toShort() }.subarray(1)
            val dataB = dataA.clone()
            dataA[1] = -22
            dataB[1] = -11
            assertEquals("0b00eaff0d00", dataA.asInt8Array().bytes.hex)
            assertEquals("0b00f5ff0d00", dataB.asInt8Array().bytes.hex)
        }
    }

    val Int8Array.bytes: ByteArray get() = toByteArray()
    val ArrayBufferView.info: String get() = "$byteOffset/$byteLength"
}
