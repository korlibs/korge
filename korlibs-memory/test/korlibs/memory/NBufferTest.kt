package korlibs.memory

import korlibs.platform.*
import kotlin.byteArrayOf
import kotlin.test.*

class NBufferTest : NBufferTestBase() {
    override val direct: Boolean get() = false

    @Test fun tets() = Unit
}
class NBufferDirectTest : NBufferTestBase() {
    override val direct: Boolean get() = true

    @Test fun tets() = Unit
}

open class NBufferTestBase {
    open val direct: Boolean = true

    fun Int8Buffer.str(): String = "[$size,${buffer.byteOffset},${buffer.sizeInBytes}]: " + (0 until size).map { this[it] }.joinToString(",")
    fun Int16Buffer.str(): String = "[$size,${buffer.byteOffset},${buffer.sizeInBytes}]: " + (0 until size).map { this[it] }.joinToString(",")
    fun Int32Buffer.str(): String = "[$size,${buffer.byteOffset},${buffer.sizeInBytes}]: " + (0 until size).map { this[it] }.joinToString(",")
    fun Int64Buffer.str(): String = "[$size,${buffer.byteOffset},${buffer.sizeInBytes}]: " + (0 until size).map { this[it] }.joinToString(",")
    fun Float32Buffer.str(): String = "[$size,${buffer.byteOffset},${buffer.sizeInBytes}]: " + (0 until size).map { this[it].toInt().toString() }.joinToString(",")
    fun Float64Buffer.str(): String = "[$size,${buffer.byteOffset},${buffer.sizeInBytes}]: " + (0 until size).map { this[it].toInt().toString() }.joinToString(",")

    @Test
    fun testInt8() {
        val data = Int8Buffer(8, direct)
        val data2 = data.slice(1, 3)
        val data3 = data.sliceWithSize(1, 7).sliceWithSize(1, 6).sliceWithSize(1, 5).sliceWithSize(1, 4).sliceWithSize(1, 3)

        for (n in 0 until 8) data[n] = (n * 10 + n).toByte()

        assertEquals(
            """
                [8,0,8]: 0,11,22,33,44,55,66,77
                [2,1,2]: 11,22
                [3,5,3]: 55,66,77
            """.trimIndent(),
            """
                ${data.str()}
                ${data2.str()}
                ${data3.str()}
            """.trimIndent()
        )

        data2.setArray(0, byteArrayOf(-11, -22))
        data3.setArray(1, byteArrayOf(-66))
        assertEquals("[8,0,8]: 0,-11,-22,33,44,55,-66,77", data.str())
    }

    @Test
    fun testInt16() {
        val data = Int16Buffer(8, direct)
        val data2 = data.slice(1, 3)
        val data3 = data.sliceWithSize(1, 7).sliceWithSize(1, 6).sliceWithSize(1, 5).sliceWithSize(1, 4).sliceWithSize(1, 3)

        for (n in 0 until 8) data[n] = (n * 10 + n).toShort()

        assertEquals(
            """
                [8,0,16]: 0,11,22,33,44,55,66,77
                [2,2,4]: 11,22
                [3,10,6]: 55,66,77
            """.trimIndent(),
            """
                ${data.str()}
                ${data2.str()}
                ${data3.str()}
            """.trimIndent()
        )

        data2.setArray(0, shortArrayOf(-11, -22))
        data3.setArray(1, shortArrayOf(-66))
        assertEquals("[8,0,16]: 0,-11,-22,33,44,55,-66,77", data.str())
    }

    @Test
    fun testInt32() {
        val data = Int32Buffer(8, direct)
        val data2 = data.slice(1, 3)
        val data3 = data.sliceWithSize(1, 7).sliceWithSize(1, 6).sliceWithSize(1, 5).sliceWithSize(1, 4).sliceWithSize(1, 3)

        for (n in 0 until 8) data[n] = n * 10 + n

        assertEquals(
            """
                [8,0,32]: 0,11,22,33,44,55,66,77
                [2,4,8]: 11,22
                [3,20,12]: 55,66,77
            """.trimIndent(),
            """
                ${data.str()}
                ${data2.str()}
                ${data3.str()}
            """.trimIndent()
        )

        data2.setArray(0, intArrayOf(-11, -22))
        data3.setArray(1, intArrayOf(-66))
        assertEquals("[8,0,32]: 0,-11,-22,33,44,55,-66,77", data.str())
    }

    @Test
    fun testInt64() {
        val data = Int64Buffer(8, direct)
        val data2 = data.slice(1, 3)
        val data3 = data.sliceWithSize(1, 7).sliceWithSize(1, 6).sliceWithSize(1, 5).sliceWithSize(1, 4).sliceWithSize(1, 3)

        for (n in 0 until 8) data[n] = (n * 10 + n).toLong()

        assertEquals(
            """
                [8,0,64]: 0,11,22,33,44,55,66,77
                [2,8,16]: 11,22
                [3,40,24]: 55,66,77
            """.trimIndent(),
            """
                ${data.str()}
                ${data2.str()}
                ${data3.str()}
            """.trimIndent()
        )

        data2.setArray(0, longArrayOf(-11, -22))
        data3.setArray(1, longArrayOf(-66))
        assertEquals("[8,0,64]: 0,-11,-22,33,44,55,-66,77", data.str())
    }

    @Test
    fun testFloat32() {
        val data = Float32Buffer(8, direct)
        val data2 = data.slice(1, 3)
        val data3 = data.sliceWithSize(1, 7).sliceWithSize(1, 6).sliceWithSize(1, 5).sliceWithSize(1, 4).sliceWithSize(1, 3)

        for (n in 0 until 8) data[n] = (n * 10 + n).toFloat()

        assertEquals(
            """
                [8,0,32]: 0,11,22,33,44,55,66,77
                [2,4,8]: 11,22
                [3,20,12]: 55,66,77
            """.trimIndent(),
            """
                ${data.str()}
                ${data2.str()}
                ${data3.str()}
            """.trimIndent()
        )

        data2.setArray(0, floatArrayOf(-11f, -22f))
        data3.setArray(1, floatArrayOf(-66f))
        assertEquals("[8,0,32]: 0,-11,-22,33,44,55,-66,77", data.str())
    }

    @Test
    fun testFloat64() {
        val data = Float64Buffer(8, direct)
        val data2 = data.slice(1, 3)
        val data3 = data.sliceWithSize(1, 7).sliceWithSize(1, 6).sliceWithSize(1, 5).sliceWithSize(1, 4).sliceWithSize(1, 3)

        for (n in 0 until 8) data[n] = (n * 10 + n).toDouble()

        assertEquals(
            """
                [8,0,64]: 0,11,22,33,44,55,66,77
                [2,8,16]: 11,22
                [3,40,24]: 55,66,77
            """.trimIndent(),
            """
                ${data.str()}
                ${data2.str()}
                ${data3.str()}
            """.trimIndent()
        )

        data2.setArray(0, doubleArrayOf(-11.0, -22.0))
        data3.setArray(1, doubleArrayOf(-66.0))
        assertEquals("[8,0,64]: 0,-11,-22,33,44,55,-66,77", data.str())
    }

    @Test
    fun testEndianness() {
        val data64 = Int64Buffer(1, direct)
        val data32 = data64.asInt32()
        val data16 = data64.asInt16()
        val data8 = data64.asInt8()
        for (n in 0 until 8) data8[n] = n.toByte()
        assertEquals(0x00, data8[0].asLittle().toInt() and 0xFF)
        assertEquals(0x0100, data16[0].asLittle().toInt() and 0xFFFF)
        assertEquals(0x03020100, data32[0].asLittle())
        assertEquals(0x0706050403020100L, data64[0].asLittle())
    }

    @Test
    fun testWrap() {
        val data = Buffer(byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7), 2, 6).i16
        val farray = data.getArray(1, size = 2).map { it.asLittle() }.toShortArray()
        assertEquals("1284,1798", farray.joinToString(","))
        assertEquals("3,4", data.asInt8().getArray(1, size = 2).map { it.asLittle() }.toByteArray().joinToString(","))
    }

    @Test
    fun testWrapUnsigned() {
        val data = Buffer(byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7), 2, 6).u16
        assertEquals("1284,1798", data.getArray(1, size = 2).data.map { it.asLittle() }.toShortArray().joinToString(","))
        assertEquals("3,4", data.asUInt8().getArray(1, size = 2).data.map { it.asLittle() }.toByteArray().joinToString(","))
    }

    @Test
    fun testArrayTransfer() {
        val i64 = Int64Buffer(2, direct)
        val buffer = i64.buffer
        val data = i64.asUInt8()
        val i32 = i64.asInt32().sliceWithSize(1, 1)
        val i16 = i64.asInt16().sliceWithSize(2, 1)
        val i8 = i64.asInt8().sliceWithSize(4, 1)
        for (n in 0 until 8) {
            data[n] = n
            data[n + 8] = n * 16 + n
        }
        assertEquals("00010203040506070011223344556677", buffer.hex())
        assertEquals("7766554433221100", i64[1].asLittle().toULong().toString(16))
        assertEquals("7060504", i32[0].asLittle().toULong().toString(16))
        assertEquals("504", i16[0].asLittle().toInt().toULong().toString(16))
        assertEquals("4", i8[0].asLittle().toInt().toULong().toString(16))
    }

    @Test
    fun testArrayTransferEx() {
        val expectedStr = """
            0,1,2,3,4,0,0,-3,-4,-5,0,0,0,0
            0,1,2,3,4,0,0,-3,-4,-5,0,0,0,0
            1,2,3,4,0,0,-3,-4,-5,0,0,0,0
            2,3,4
        """.trimIndent()

        Uint8Buffer(14).also { array ->
            array.setArray(1, ubyteArrayIntOf(1, 2, 3, 4))
            array.setArray(7, ubyteArrayIntOf(-1, -2, -3, -4, -5, -6, -7), 2, 3)
            assertEquals(
                expectedStr,
                """
                ${array.getArray().data.joinToString(",")}
                ${array.getArray(0, size = array.size).data.joinToString(",")}
                ${array.getArray(1).data.joinToString(",")}
                ${array.getArray(2, size = 3).data.joinToString(",")}
            """.trimIndent()
            )
        }

        Uint16Buffer(14).also { array ->
            array.setArray(1, ushortArrayIntOf(1, 2, 3, 4))
            array.setArray(7, ushortArrayIntOf(-1, -2, -3, -4, -5, -6, -7), 2, 3)
            assertEquals(
                expectedStr,
                """
                ${array.getArray().data.joinToString(",")}
                ${array.getArray(0, size = array.size).data.joinToString(",")}
                ${array.getArray(1).data.joinToString(",")}
                ${array.getArray(2, size = 3).data.joinToString(",")}
            """.trimIndent()
            )
        }

        Int8Buffer(14).also { array ->
            array.setArray(1, byteArrayOf(1, 2, 3, 4))
            array.setArray(7, byteArrayOf(-1, -2, -3, -4, -5, -6, -7), 2, 3)
            assertEquals(
                expectedStr,
                """
                ${array.getArray().joinToString(",")}
                ${array.getArray(0, size = array.size).joinToString(",")}
                ${array.getArray(1).joinToString(",")}
                ${array.getArray(2, size = 3).joinToString(",")}
            """.trimIndent()
            )
        }

        Int16Buffer(14).also { array ->
            array.setArray(1, shortArrayOf(1, 2, 3, 4))
            array.setArray(7, shortArrayOf(-1, -2, -3, -4, -5, -6, -7), 2, 3)
            assertEquals(
                expectedStr,
                """
                ${array.getArray().joinToString(",")}
                ${array.getArray(0, size = array.size).joinToString(",")}
                ${array.getArray(1).joinToString(",")}
                ${array.getArray(2, size = 3).joinToString(",")}
            """.trimIndent()
            )
        }

        Int32Buffer(14).also { array ->
            array.setArray(1, intArrayOf(1, 2, 3, 4))
            array.setArray(7, intArrayOf(-1, -2, -3, -4, -5, -6, -7), 2, 3)
            assertEquals(
                expectedStr,
                """
                ${array.getArray().joinToString(",")}
                ${array.getArray(0, size = array.size).joinToString(",")}
                ${array.getArray(1).joinToString(",")}
                ${array.getArray(2, size = 3).joinToString(",")}
            """.trimIndent()
            )
        }

        Int64Buffer(14).also { array ->
            array.setArray(1, longArrayOf(1, 2, 3, 4))
            array.setArray(7, longArrayOf(-1, -2, -3, -4, -5, -6, -7), 2, 3)
            assertEquals(
                expectedStr,
                """
                ${array.getArray().joinToString(",")}
                ${array.getArray(0, size = array.size).joinToString(",")}
                ${array.getArray(1).joinToString(",")}
                ${array.getArray(2, size = 3).joinToString(",")}
            """.trimIndent()
            )
        }

        Float32Buffer(14).also { array ->
            array.setArray(1, floatArrayOf(1f, 2f, 3f, 4f))
            array.setArray(7, floatArrayOf(-1f, -2f, -3f, -4f, -5f, -6f, -7f), 2, 3)
            assertEquals(
                expectedStr,
                """
                ${array.getArray().map { it.toInt() }.joinToString(",")}
                ${array.getArray(0, size = array.size).map { it.toInt() }.joinToString(",")}
                ${array.getArray(1).map { it.toInt() }.joinToString(",")}
                ${array.getArray(2, size = 3).map { it.toInt() }.joinToString(",")}
            """.trimIndent()
            )
        }

        Float64Buffer(14).also { array ->
            array.setArray(1, doubleArrayOf(1.0, 2.0, 3.0, 4.0))
            array.setArray(7, doubleArrayOf(-1.0, -2.0, -3.0, -4.0, -5.0, -6.0, -7.0), 2, 3)
            assertEquals(
                expectedStr,
                """
                ${array.getArray().map { it.toInt() }.joinToString(",")}
                ${array.getArray(0, size = array.size).map { it.toInt() }.joinToString(",")}
                ${array.getArray(1).map { it.toInt() }.joinToString(",")}
                ${array.getArray(2, size = 3).map { it.toInt() }.joinToString(",")}
            """.trimIndent()
            )
        }
    }

    @Test
    fun testUnalignedVariants() {
        val i = Buffer(4, direct)
        val u8 = i.u8
        val u16 = i.u16
        assertEquals(4, u8.size)
        assertEquals(2, u16.size)
        for (n in 0 until 4) u8[n] = n * 16 + n
        assertEquals(0x1100, u16[0].asLittle())
        assertEquals(0x3322, u16[1].asLittle())
        assertEquals(0x33, u8[3].asLittle())
        u16[0] = -1
        u16[1] = -2
        assertEquals(0xFFFF, u16[0])
        assertEquals(0xFFFE, u16[1])
    }

    @Test
    fun testAsTyped() {
        val i = Buffer(8, direct)
        assertEquals(i, i.u8.buffer)
        assertEquals(i, i.u16.buffer)
        assertEquals(i, i.i8.buffer)
        assertEquals(i, i.i16.buffer)
        assertEquals(i, i.i32.buffer)
        assertEquals(i, i.i64.buffer)
        assertEquals(i, i.f32.buffer)
        assertEquals(i, i.f64.buffer)
    }

    @Test
    fun testTypedAsTyped() {
        val i = Buffer(8, direct).u8
        assertEquals(i.buffer, i.asUInt8().buffer)
        assertEquals(i.buffer, i.asUInt16().buffer)
        assertEquals(i.buffer, i.asInt8().buffer)
        assertEquals(i.buffer, i.asInt16().buffer)
        assertEquals(i.buffer, i.asInt32().buffer)
        assertEquals(i.buffer, i.asInt64().buffer)
        assertEquals(i.buffer, i.asFloat32().buffer)
        assertEquals(i.buffer, i.asFloat64().buffer)
    }

    @Test
    fun testHex() {
        assertEquals(
            "0123456789abcdef",
            Buffer(ubyteArrayIntOf(0x01, 0x23, 0x45, 0x67, 0x89, 0xab, 0xcd, 0xef).asByteArray()).hex()
        )
    }

    @Test
    fun testInvalidConstructionArguments() {
        Buffer(0)
        assertFailsWith<IllegalArgumentException> { Buffer(-1) }
        assertFailsWith<IllegalArgumentException> { Buffer(byteArrayOf(1), 0, -1) }
        assertFailsWith<IllegalArgumentException> { Buffer(byteArrayOf(1), 0, 2) }
        assertFailsWith<IllegalArgumentException> { Buffer(byteArrayOf(1), 1, 1) }
        assertFailsWith<IllegalArgumentException> { Buffer(byteArrayOf(1), 2, 0) }
        assertFailsWith<IllegalArgumentException> { Buffer(byteArrayOf(1), -1, 0) }
    }

    @Test
    fun testInvalidSlice() {
        val buffer = Buffer(8)
        buffer.sliceWithSize(0, 0)
        buffer.sliceWithSize(8, 0)
        buffer.sliceWithSize(0, 8)
        buffer.sliceWithSize(1, 7)
        assertFailsWith<IllegalArgumentException> { buffer.sliceWithSize(9, 0) }
        assertFailsWith<IllegalArgumentException> { buffer.sliceWithSize(1, -1) }
        assertFailsWith<IllegalArgumentException> { buffer.sliceWithSize(0, 9) }
        assertFailsWith<IllegalArgumentException> { buffer.sliceWithSize(1, 8) }
    }

    @Test
    fun testCopy() {
        val bufferBase = Buffer(ByteArray(16) { it.toByte() })
        val buffer = bufferBase.sliceBuffer(1)
        val buffer2 = Buffer(14, direct)
        Buffer.copy(buffer, 2, buffer2, 5, 7)
        assertEquals("0000000000030405060708090000", buffer2.hex())
    }

    @Test
    fun testCopy2() {
        for (direct1 in listOf(false, true)) {
            for (direct2 in listOf(false, true)) {
                val bufferBase = Buffer(16, direct1)
                for (n in 0 until 16) bufferBase.setUnalignedUInt8(n, n)
                val buffer = bufferBase.sliceBuffer(1)
                val buffer2 = Buffer(14, direct2)
                Buffer.copy(buffer, 2, buffer2, 5, 7)
                assertEquals("0000000000030405060708090000", buffer2.hex())
            }
        }
    }

    @Test
    fun testCopy3() {
        for (direct1 in listOf(false, true)) {
            for (direct2 in listOf(false, true)) {
                val bufferBase = Buffer(16, direct1)
                for (n in 0 until 16) bufferBase.setUnalignedUInt8(n, n)
                val buffer = bufferBase.sliceBuffer(1)
                val buffer2 = buffer.sliceBuffer(2)
                Buffer.copy(buffer.sliceBuffer(1), 2, buffer2, 5, 7)
                assertEquals("03040506070405060708090a0f", buffer2.hex())
            }
        }
    }

    fun Byte.asLittle(): Byte = this
    fun Short.asLittle(): Short = if (Platform.isLittleEndian) this else this.reverseBytes()
    fun Int.asLittle(): Int = if (Platform.isLittleEndian) this else this.reverseBytes()
    fun Long.asLittle(): Long = if (Platform.isLittleEndian) this else this.reverseBytes()

    fun Byte.asBig(): Byte = this
    fun Short.asBig(): Short = if (Platform.isLittleEndian) this.reverseBytes() else this
    fun Int.asBig(): Int = if (Platform.isLittleEndian) this.reverseBytes() else this
    fun Long.asBig(): Long = if (Platform.isLittleEndian) this.reverseBytes() else this
}
