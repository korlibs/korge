package korlibs.datastructure

import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun Random.intStream(): Sequence<Int> = sequence { while (true) yield(nextInt()) }
fun Random.intStream(from: Int, until: Int): Sequence<Int> = sequence { while (true) yield(nextInt(from, until)) }
fun Random.intStream(range: IntRange): Sequence<Int> = intStream(range.start, range.endInclusive + 1)

class IntMapTest {
    @Test
    fun smoke() {
        val values = Random(0).intStream().take(10000).toList().distinct()
        val valuesSet = values.toSet()
        val m = IntMap<String>()
        for (value in values) m[value] = "value"
        assertEquals(m.size, values.size)
        val notIn = (0 until 10001).asSequence().firstOrNull { it !in valuesSet } ?: error("Unexpected")
        assertEquals(false, m.contains(notIn))
        for (key in m.keys.toList()) {
            assertEquals(true, m.contains(key))
            assertEquals(true, key in m)
        }

        val removeValues = values.take(values.size / 2)
        for (key in removeValues) m.remove(key)
        assertEquals(values.size - removeValues.size, m.size)

        val containingValues = values.drop(removeValues.size)
        for (key in removeValues) assertEquals(false, key in m)
        for (key in containingValues) assertEquals(true, key in m)
    }

    @Test
    fun simple() {
        val m = IntMap<String>()
        assertEquals(0, m.size)
        assertEquals(null, m[0])

        m[0] = "test"
        assertEquals(1, m.size)
        assertEquals("test", m[0])
        assertEquals(null, m[1])

        m[0] = "test2"
        assertEquals(1, m.size)
        assertEquals("test2", m[0])
        assertEquals(null, m[1])

        m.remove(0)
        assertEquals(0, m.size)
        assertEquals(null, m[0])
        assertEquals(null, m[1])

        m.remove(0)
    }

    @Test
    fun name2() {
        val m = IntMap<Int>()
        for (n in 0 until 1000) m[n] = n * 1000
        for (n in 0 until 1000) assertEquals(n * 1000, m[n])
        assertEquals(null, m[-1])
        assertEquals(null, m[1001])
    }

    @Test
    fun testIntMapOf() {
        val map = intMapOf(1 to "one", 2 to "two")
        assertEquals(null, map[0])
        assertEquals("one", map[1])
        assertEquals("two", map[2])
        assertEquals(null, map[3])
    }

    @Test
    fun testIntMapGetOrPut() {
        val map = intMapOf<String>()
        for (n in 0 until 3) map.getOrPut(n) { "${-it}" }
        assertEquals("0=0, 1=-1, 2=-2", map.toMap().entries.sortedBy { it.key }.joinToString(", "))
    }
}
