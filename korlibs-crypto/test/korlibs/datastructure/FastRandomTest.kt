package korlibs.datastructure

import korlibs.datastructure.random.FastRandom
import korlibs.datastructure.random.fastRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class FastRandomTest {
    val random = FastRandom(0L)

    @Test
    fun testLong() {
        assertEquals(
            "[512, 774, 807, 39, 6, 289, 517, 769, 807, 32]",
            (0 until 10).map { random.nextLong(0L, 1000L) }.toString()
        )
    }

    @Test
    fun testInt() {
        assertEquals(
            "[214, 848, 257, 367, 882, 107, 961, 727, 814, 459]",
            (0 until 10).map { random.nextInt(0, 1000) }.toString()
        )
    }

    @Test
    fun testFail() {
        assertFails { random.nextInt(10, 0) }
        assertFails { random.nextLong(10L, 0L) }
    }

    @Test
    fun testGlobal() {
        FastRandom.Default.nextBits(0)
        FastRandom.nextBits(0)
        FastRandom()
        assertTrue { (0 until 20).map { FastRandom.nextBits(0) }.distinct().count() > 1 }
        assertTrue { (0 until 20).map { FastRandom.Default.nextBits(0) }.distinct().count() > 1 }
        assertTrue { (0 until 20).map { FastRandom().nextBits(0) }.distinct().count() > 1 }
    }

    @Test
    fun testArrayExtensionsFail() {
        assertFails { listOf<String>().fastRandom() }
        assertFails { arrayOfNulls<Int>(0).fastRandom() }
        assertFails { arrayOf<String>().fastRandom() }
        assertFails { BooleanArray(0).fastRandom() }
        assertFails { CharArray(0).fastRandom() }
        assertFails { ShortArray(0).fastRandom() }
        assertFails { IntArray(0).fastRandom() }
        assertFails { LongArray(0).fastRandom() }
        assertFails { FloatArray(0).fastRandom() }
        assertFails { DoubleArray(0).fastRandom() }
    }

    @Test
    fun testArrayExtensions() {
        assertEquals("a", listOf("a").fastRandom())
        assertEquals("a", arrayOf("a").fastRandom())
        assertEquals(true, booleanArrayOf(true).fastRandom())
        assertEquals('a', charArrayOf('a').fastRandom())
        assertEquals(-10, shortArrayOf(-10).fastRandom())
        assertEquals(-10, intArrayOf(-10).fastRandom())
        assertEquals(-10L, longArrayOf(-10L).fastRandom())
        assertEquals(3f, floatArrayOf(3f).fastRandom())
        assertEquals(7.0, doubleArrayOf(7.0).fastRandom())
    }

    @Test
    fun testArrayExtensions2() {
        val random = FastRandom(0L)
        assertEquals("a", listOf("a").random(random))
        assertEquals("a", arrayOf("a").random(random))
        assertEquals(true, booleanArrayOf(true).random(random))
        assertEquals('a', charArrayOf('a').random(random))
        assertEquals(-10, shortArrayOf(-10).random(random))
        assertEquals(-10, intArrayOf(-10).random(random))
        assertEquals(-10L, longArrayOf(-10L).random(random))
        assertEquals(3f, floatArrayOf(3f).random(random))
        assertEquals(7.0, doubleArrayOf(7.0).random(random))
    }

    @Test
    fun testGlobalInstance() {
        println((0 until 10).map { FastRandom.nextInt() })
        assertTrue { (0 until 10).map { FastRandom.nextInt() }.distinct().size >= 2 }
    }
}
