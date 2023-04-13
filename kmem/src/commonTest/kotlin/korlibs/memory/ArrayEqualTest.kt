package korlibs.memory

import kotlin.test.*

class ArrayEqualTest {
    fun arrayequal64(src: Buffer, srcPos: Int, dst: Buffer, dstPos: Int, size: Int): Boolean = Buffer.equalsCommon(src, srcPos, dst, dstPos, size, use64 = true)
    fun arrayequal32(src: Buffer, srcPos: Int, dst: Buffer, dstPos: Int, size: Int): Boolean = Buffer.equalsCommon(src, srcPos, dst, dstPos, size, use64 = false)

    @Test
    fun testEqualsSimple() = testEqualsSimple(::arrayequal)
    @Test
    fun testEqualsOffset() = testEqualsOffset(::arrayequal)
    @Test
    fun testNotEqualsLoop() = testNotEqualsLoop(::arrayequal)
    @Test
    fun testNotEqualsSimple() = testNotEqualsSimple(::arrayequal)

    @Test
    fun testEqualsSimple64() = testEqualsSimple(::arrayequal64)
    @Test
    fun testEqualsOffset64() = testEqualsOffset(::arrayequal64)
    @Test
    fun testNotEqualsLoop64() = testNotEqualsLoop(::arrayequal64)
    @Test
    fun testNotEqualsSimple64() = testNotEqualsSimple(::arrayequal64)

    @Test
    fun testEqualsSimple32() = testEqualsSimple(::arrayequal32)
    @Test
    fun testEqualsOffset32() = testEqualsOffset(::arrayequal32)
    @Test
    fun testNotEqualsLoop32() = testNotEqualsLoop(::arrayequal32)
    @Test
    fun testNotEqualsSimple32() = testNotEqualsSimple(::arrayequal32)


    private fun testEqualsSimple(arrayequal: (src: Buffer, srcPos: Int, dst: Buffer, dstPos: Int, size: Int) -> Boolean) {
        for (n in 0 until 512) {
            assertEquals(true, arrayequal(Buffer(ByteArray(n) { n.toByte() }), 0, Buffer(ByteArray(n) { n.toByte() }), 0, n), "simple $n")
        }
    }

    private fun testEqualsOffset(arrayequal: (src: Buffer, srcPos: Int, dst: Buffer, dstPos: Int, size: Int) -> Boolean) {
        for (m in 0 until 8) {
            for (n in 0 until 64) {
                assertEquals(true, arrayequal(Buffer(ByteArray(m) + ByteArray(n) { n.toByte() }), m, Buffer(ByteArray(n) { n.toByte() } + byteArrayOf(7)), 0, n), "offset[a] $m, $n")
                assertEquals(true, arrayequal(Buffer(ByteArray(n) { n.toByte() } + byteArrayOf(7)), 0, Buffer(ByteArray(m) + ByteArray(n) { n.toByte() }), m, n), "offset[b] $m, $n")
            }
        }
    }

    private fun testNotEqualsLoop(arrayequal: (src: Buffer, srcPos: Int, dst: Buffer, dstPos: Int, size: Int) -> Boolean) {
        for (m in 0 until 8) {
            for (n in 1 until 64) {
                val array1 = ByteArray(n) { n.toByte() }
                val array2 = ByteArray(n) { n.toByte() }
                array2[n - 1]--
                assertEquals(false, arrayequal(Buffer(ByteArray(m) + array1), m, Buffer(array2), 0, n), "offset[a] $m, $n")
                assertEquals(false, arrayequal(Buffer(array1), 0, Buffer(ByteArray(m) + array2), m, n), "offset[a] $m, $n")
            }
        }
    }

    private fun testNotEqualsSimple(arrayequal: (src: Buffer, srcPos: Int, dst: Buffer, dstPos: Int, size: Int) -> Boolean) {
        assertEquals(false, arrayequal(Buffer(byteArrayOf(1)), 0, Buffer(byteArrayOf(2)), 0, 1))
        assertEquals(false, arrayequal(Buffer(byteArrayOf(2)), 0, Buffer(byteArrayOf(1)), 0, 1))
        assertEquals(false, arrayequal(Buffer(byteArrayOf(1, 2, 3)), 0, Buffer(byteArrayOf(1, 2, 4)), 0, 3))
        assertEquals(false, arrayequal(Buffer(byteArrayOf(1, 2, 3)), 1, Buffer(byteArrayOf(1, 2, 4)), 1, 2))
        assertEquals(true, arrayequal(Buffer(byteArrayOf(1, 2, 3)), 0, Buffer(byteArrayOf(1, 2, 4)), 0, 2))
        assertEquals(true, arrayequal(Buffer(byteArrayOf(1, 2, 3)), 1, Buffer(byteArrayOf(1, 2, 4)), 1, 1))
    }
}
