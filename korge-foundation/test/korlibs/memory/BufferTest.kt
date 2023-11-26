package korlibs.memory

import kotlin.test.*

class BufferTest {
    @Test
    fun test() {
        val ba = Int8Buffer(3).apply {
            this[0] = -1
            this[1] = -2
            this[2] = -3
        }
        assertEquals(-1, ba[0])
        assertEquals(-2, ba[1])
        assertEquals(-3, ba[2])
    }

    @Test
    fun testSlice() {
        val data = Buffer(16 * 4)
        val sliceAll = data.f32.sliceWithSize(0, 16)
        val slice1 = data.f32.sliceWithSize(0, 8)
        val slice2 = data.f32.sliceWithSize(8, 8)
        for (n in 0 until 16) sliceAll[n] = n.toFloat()
        assertEquals(FloatArray(16) { it.toFloat() }.toList(), (0 until sliceAll.size).map { sliceAll[it] })
        assertEquals(FloatArray(8) { it.toFloat() }.toList(), (0 until slice1.size).map { slice1[it] })
        assertEquals(FloatArray(8) { (8 + it).toFloat() }.toList(), (0 until slice2.size).map { slice2[it] })
    }

    @Test
    fun testCopySmall() {
        val buffer1 = Uint8Buffer(byteArrayOf(1, 2, 3, 4, 5, 6))
        val buffer2 = Uint8Buffer(byteArrayOf(-1, -2, -3, -4, -5, -6))
        arraycopy(buffer1.slice(3, 6), 1, buffer2.slice(1), 2, 2)
        assertEquals(listOf(255, 254, 253, 5, 6, 250), IntArray(buffer2.size) { buffer2[it] }.toList())
    }

    @Test
    fun testCopyBigUnsigned8() {
        val buffer1 = Uint8Buffer(byteArrayOf(1, 2, 3, 4, 5, 6) + ByteArray(4096))
        val buffer2 = Uint8Buffer(byteArrayOf(-1, -2, -3, -4, -5, -6) + ByteArray(4096))
        arraycopy(buffer1.slice(3, 4000), 1, buffer2.slice(1), 2, 3000)
        assertEquals(listOf(255, 254, 253, 5, 6, 0, 0, 0, 0, 0), IntArray(10) { buffer2[it] }.toList())
    }
    
    @Test
    fun testCopyBig8() {
        run {
            val buffer1 = Int8Buffer(byteArrayOf(1, 2, 3, 4, 5, 6) + ByteArray(4096))
            val buffer2 = Int8Buffer(byteArrayOf(-1, -2, -3, -4, -5, -6) + ByteArray(4096))
            arraycopy(buffer1.slice(3, 4000), 1, buffer2.slice(1), 2, 3000)
            assertEquals(listOf(-1, -2, -3, 5, 6, 0, 0, 0, 0, 0), IntArray(10) { buffer2[it].toInt() }.toList())
        }
        run {
            val buffer1 = byteArrayOf(1, 2, 3, 4, 5, 6) + ByteArray(4096)
            val buffer2 = Int8Buffer(byteArrayOf(-1, -2, -3, -4, -5, -6) + ByteArray(4096))
            arraycopy(buffer1.sliceArray(3 until 3 + 4000), 1, buffer2.slice(1), 2, 3000)
            assertEquals(listOf(-1, -2, -3, 5, 6, 0, 0, 0, 0, 0), IntArray(10) { buffer2[it].toInt() }.toList())
        }
        run {
            val buffer1 = Int8Buffer(byteArrayOf(1, 2, 3, 4, 5, 6) + ByteArray(4096))
            val buffer2 = byteArrayOf(-1, -2, -3, -4, -5, -6) + ByteArray(4096)
            arraycopy(buffer1.slice(3, 4000), 1, buffer2, 3, 3000)
            assertEquals(listOf(-1, -2, -3, 5, 6, 0, 0, 0, 0, 0), IntArray(10) { buffer2[it].toInt() }.toList())
        }
    }

    @Test
    fun testCopyBig16() {
        run {
            val buffer1 = Int16Buffer(shortArrayOf(1, 2, 3, 4, 5, 6) + ShortArray(4096))
            val buffer2 = Int16Buffer(shortArrayOf(-1, -2, -3, -4, -5, -6) + ShortArray(4096))
            arraycopy(buffer1.slice(3, 4000), 1, buffer2.slice(1), 2, 3000)
            assertEquals(listOf(-1, -2, -3, 5, 6, 0, 0, 0, 0, 0), IntArray(10) { buffer2[it].toInt() }.toList())
        }
        run {
            val buffer1 = shortArrayOf(1, 2, 3, 4, 5, 6) + ShortArray(4096)
            val buffer2 = Int16Buffer(shortArrayOf(-1, -2, -3, -4, -5, -6) + ShortArray(4096))
            arraycopy(buffer1.sliceArray(3 until 3 + 4000), 1, buffer2.slice(1), 2, 3000)
            assertEquals(listOf(-1, -2, -3, 5, 6, 0, 0, 0, 0, 0), IntArray(10) { buffer2[it].toInt() }.toList())
        }
        run {
            val buffer1 = Int16Buffer(shortArrayOf(1, 2, 3, 4, 5, 6) + ShortArray(4096))
            val buffer2 = shortArrayOf(-1, -2, -3, -4, -5, -6) + ShortArray(4096)
            arraycopy(buffer1.slice(3, 4000), 1, buffer2, 3, 3000)
            assertEquals(listOf(-1, -2, -3, 5, 6, 0, 0, 0, 0, 0), IntArray(10) { buffer2[it].toInt() }.toList())
        }
    }

    @Test
    fun testCopyBig32() {
        run {
            val buffer1 = Int32Buffer(intArrayOf(1, 2, 3, 4, 5, 6) + IntArray(4096))
            val buffer2 = Int32Buffer(intArrayOf(-1, -2, -3, -4, -5, -6) + IntArray(4096))
            arraycopy(buffer1.slice(3, 4000), 1, buffer2.slice(1), 2, 3000)
            assertEquals(listOf(-1, -2, -3, 5, 6, 0, 0, 0, 0, 0), IntArray(10) { buffer2[it].toInt() }.toList())
        }
        run {
            val buffer1 = intArrayOf(1, 2, 3, 4, 5, 6) + IntArray(4096)
            val buffer2 = Int32Buffer(intArrayOf(-1, -2, -3, -4, -5, -6) + IntArray(4096))
            arraycopy(buffer1.sliceArray(3 until 3 + 4000), 1, buffer2.slice(1), 2, 3000)
            assertEquals(listOf(-1, -2, -3, 5, 6, 0, 0, 0, 0, 0), IntArray(10) { buffer2[it].toInt() }.toList())
        }
        run {
            val buffer1 = Int32Buffer(intArrayOf(1, 2, 3, 4, 5, 6) + IntArray(4096))
            val buffer2 = intArrayOf(-1, -2, -3, -4, -5, -6) + IntArray(4096)
            arraycopy(buffer1.slice(3, 4000), 1, buffer2, 3, 3000)
            assertEquals(listOf(-1, -2, -3, 5, 6, 0, 0, 0, 0, 0), IntArray(10) { buffer2[it].toInt() }.toList())
        }
    }

    fun floatArrayOf(vararg values: Int): FloatArray = FloatArray(values.size) { values[it].toFloat() }
    fun doubleArrayOf(vararg values: Int): DoubleArray = DoubleArray(values.size) { values[it].toDouble() }

    @Test
    fun testCopyBigF32() {
        run {
            val buffer1 = Float32Buffer(floatArrayOf(1, 2, 3, 4, 5, 6) + FloatArray(4096))
            val buffer2 = Float32Buffer(floatArrayOf(-1, -2, -3, -4, -5, -6) + FloatArray(4096))
            arraycopy(buffer1.slice(3, 4000), 1, buffer2.slice(1), 2, 3000)
            assertEquals(listOf(-1, -2, -3, 5, 6, 0, 0, 0, 0, 0), IntArray(10) { buffer2[it].toInt() }.toList())
        }
        run {
            val buffer1 = floatArrayOf(1, 2, 3, 4, 5, 6) + FloatArray(4096)
            val buffer2 = Float32Buffer(floatArrayOf(-1, -2, -3, -4, -5, -6) + FloatArray(4096))
            arraycopy(buffer1.sliceArray(3 until 3 + 4000), 1, buffer2.slice(1), 2, 3000)
            assertEquals(listOf(-1, -2, -3, 5, 6, 0, 0, 0, 0, 0), IntArray(10) { buffer2[it].toInt() }.toList())
        }
        run {
            val buffer1 = Float32Buffer(floatArrayOf(1, 2, 3, 4, 5, 6) + FloatArray(4096))
            val buffer2 = floatArrayOf(-1, -2, -3, -4, -5, -6) + FloatArray(4096)
            arraycopy(buffer1.slice(3, 4000), 1, buffer2, 3, 3000)
            assertEquals(listOf(-1, -2, -3, 5, 6, 0, 0, 0, 0, 0), IntArray(10) { buffer2[it].toInt() }.toList())
        }
    }

    @Test
    fun testCopyBigF64() {
        run {
            val buffer1 = Float64Buffer(doubleArrayOf(1, 2, 3, 4, 5, 6) + DoubleArray(4096))
            val buffer2 = Float64Buffer(doubleArrayOf(-1, -2, -3, -4, -5, -6) + DoubleArray(4096))
            arraycopy(buffer1.slice(3, 4000), 1, buffer2.slice(1), 2, 3000)
            assertEquals(listOf(-1, -2, -3, 5, 6, 0, 0, 0, 0, 0), IntArray(10) { buffer2[it].toInt() }.toList())
        }
        run {
            val buffer1 = doubleArrayOf(1, 2, 3, 4, 5, 6) + DoubleArray(4096)
            val buffer2 = Float64Buffer(doubleArrayOf(-1, -2, -3, -4, -5, -6) + DoubleArray(4096))
            arraycopy(buffer1.sliceArray(3 until 3 + 4000), 1, buffer2.slice(1), 2, 3000)
            assertEquals(listOf(-1, -2, -3, 5, 6, 0, 0, 0, 0, 0), IntArray(10) { buffer2[it].toInt() }.toList())
        }
        run {
            val buffer1 = Float64Buffer(doubleArrayOf(1, 2, 3, 4, 5, 6) + DoubleArray(4096))
            val buffer2 = doubleArrayOf(-1, -2, -3, -4, -5, -6) + DoubleArray(4096)
            arraycopy(buffer1.slice(3, 4000), 1, buffer2, 3, 3000)
            assertEquals(listOf(-1, -2, -3, 5, 6, 0, 0, 0, 0, 0), IntArray(10) { buffer2[it].toInt() }.toList())
        }
    }

    @Test
    fun testCopyBigPosLen() {
        val buf1 = Int32Buffer(intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        val buf2 = Int32Buffer(intArrayOf(-1, -2, -3, -4, -5, -6, -7, -8, -9, -10))
        arraycopy(buf1, 6, buf2, 7, 3)
        assertEquals(listOf(-1, -2, -3, -4, -5, -6, -7, 7, 8, 9), IntArray(buf2.size) { buf2[it] }.toList())
    }

    @Test
    fun testEquality() {
        assertEquals(Int32Buffer(intArrayOf(1, 2, 3)), Int32Buffer(intArrayOf(1, 2, 3)))
        assertNotEquals(Int32Buffer(intArrayOf(1, 2, 3)), Int32Buffer(intArrayOf(1, 2, 4)))
        assertEquals(Int32Buffer(intArrayOf(1, 2, 3)).slice(1), Int32Buffer(intArrayOf(2, 3)))
    }
}
