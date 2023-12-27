package korlibs.datastructure

import kotlin.test.*

class ArrayExtTest {
    val array = intArrayOf(1, 2, 3, 4)

    @Test
    fun testSwap() {
        assertEquals(listOf(4, 2, 3, 1), array.copyOf().also { it.swap(0, 3) }.toList())
        assertEquals(listOf(1, 3, 2, 4), array.copyOf().also { it.swap(1, 2) }.toList())
        assertEquals(listOf(3, 2, 1, 4), array.copyOf().also { it.swap(0, 2) }.toList())
        assertEquals(listOf(3, 2, 1, 4), mutableListOf(1, 2, 3, 4).also { it.swap(0, 2) }.toList())
    }

    @Test
    fun testRotatedRight() {
        assertEquals(listOf(4, 1, 2, 3), array.rotatedRight(+1).toList())
        assertEquals(listOf(3, 4, 1, 2), array.rotatedRight(+2).toList())
        assertEquals(listOf(2, 3, 4, 1), array.rotatedRight(+3).toList())
        assertEquals(listOf(1, 2, 3, 4), array.rotatedRight(+4).toList())
        assertEquals(listOf(1, 2, 3, 4), array.rotatedRight(0).toList())
        assertEquals(listOf(2, 3, 4, 1), array.rotatedRight(-1).toList())
        assertEquals(listOf(3, 4, 1, 2), array.rotatedRight(-2).toList())
        assertEquals(listOf(4, 1, 2, 3), array.rotatedRight(-3).toList())
        assertEquals(listOf(1, 2, 3, 4), array.rotatedRight(-4).toList())
    }

    @Test
    fun testRotatedLeft() {
        assertEquals(listOf(4, 1, 2, 3), array.rotatedLeft(-1).toList())
        assertEquals(listOf(3, 4, 1, 2), array.rotatedLeft(-2).toList())
        assertEquals(listOf(2, 3, 4, 1), array.rotatedLeft(-3).toList())
        assertEquals(listOf(1, 2, 3, 4), array.rotatedLeft(-4).toList())
        assertEquals(listOf(1, 2, 3, 4), array.rotatedLeft(0).toList())
        assertEquals(listOf(2, 3, 4, 1), array.rotatedLeft(+1).toList())
        assertEquals(listOf(3, 4, 1, 2), array.rotatedLeft(+2).toList())
        assertEquals(listOf(4, 1, 2, 3), array.rotatedLeft(+3).toList())
        assertEquals(listOf(1, 2, 3, 4), array.rotatedLeft(+4).toList())
    }

    @Test
    fun testVariants() {
        assertEquals(listOf(3, 4, 1, 2), listOf(1, 2, 3, 4).rotatedRight(-2).toList())
        assertEquals(listOf(3, 4, 1, 2), arrayOf<Int>(1, 2, 3, 4).rotatedRight(-2).toList())
        assertEquals(listOf(3, 4, 1, 2), byteArrayOf(1, 2, 3, 4).rotatedRight(-2).map { it.toInt() }.toList())
        assertEquals(listOf(3, 4, 1, 2), charArrayOf(1.toChar(), 2.toChar(), 3.toChar(), 4.toChar()).rotatedRight(-2).map { it.toInt() }.toList())
        assertEquals(listOf(3, 4, 1, 2), shortArrayOf(1, 2, 3, 4).rotatedRight(-2).map { it.toInt() }.toList())
        assertEquals(listOf(3, 4, 1, 2), longArrayOf(1, 2, 3, 4).rotatedRight(-2).map { it.toInt() }.toList())
        assertEquals(listOf(3, 4, 1, 2), floatArrayOf(1f, 2f, 3f, 4f).rotatedRight(-2).map { it.toInt() }.toList())
        assertEquals(listOf(3, 4, 1, 2), doubleArrayOf(1.0, 2.0, 3.0, 4.0).rotatedRight(-2).map { it.toInt() }.toList())

        assertEquals(listOf(4, 1, 2, 3), array.rotatedLeft(-1).toList())
        assertEquals(listOf(4, 1, 2, 3), listOf(1, 2, 3, 4).rotatedLeft(-1).toList())
        assertEquals(listOf(4, 1, 2, 3), arrayOf(1, 2, 3, 4).rotatedLeft(-1).toList())
        assertEquals(listOf(4, 1, 2, 3), byteArrayOf(1, 2, 3, 4).rotatedLeft(-1).map { it.toInt() }.toList())
        assertEquals(listOf(4, 1, 2, 3), charArrayOf(1.toChar(), 2.toChar(), 3.toChar(), 4.toChar()).rotatedLeft(-1).map { it.toInt() }.toList())
        assertEquals(listOf(4, 1, 2, 3), shortArrayOf(1, 2, 3, 4).rotatedLeft(-1).map { it.toInt() }.toList())
        assertEquals(listOf(4, 1, 2, 3), longArrayOf(1, 2, 3, 4).rotatedLeft(-1).map { it.toInt() }.toList())
        assertEquals(listOf(4, 1, 2, 3), floatArrayOf(1f, 2f, 3f, 4f).rotatedLeft(-1).map { it.toInt() }.toList())
        assertEquals(listOf(4, 1, 2, 3), doubleArrayOf(1.0, 2.0, 3.0, 4.0).rotatedLeft(-1).map { it.toInt() }.toList())
    }
}
