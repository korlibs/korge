package korlibs.math.random

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class RandomExtKtTest {
    @Test
    fun ints() {
        assertEquals(listOf(4, 8, 7, 7, 2, 7, 1, 7, 4, 9), Random(0L).ints(0, 10).take(10).toList())
    }

    @Test
    fun weighted() {
        val random = Random(0L)
        val weights = RandomWeights("a" to 1.0, "b" to 1.0)
        assertEquals(mapOf("a" to 4987, "b" to 5013), (0 until 10000).map { random.weighted(weights) }.countMap())
    }

    @Test
    fun weighted2() {
        val random = Random(0L)
        val weights = RandomWeights("a" to 1.0, "b" to 4.0)
        assertEquals(mapOf("a" to 3367, "b" to 6633), (0 until 10000).map { random.weighted(weights) }.countMap())
    }

    @Test
    fun weighted3() {
        val random = Random(0L)
        val weights = RandomWeights("a" to 1.0, "b" to 4.0, "c" to 8.0)
        assertEquals(
            mapOf("a" to 1543, "b" to 3258, "c" to 5199),
            (0 until 10000).map { random.weighted(weights) }.countMap()
        )
    }

    private fun <T> List<T>.countMap(): Map<T, Int> {
        val counts = hashMapOf<T, Int>()
        for (key in this) {
            if (key !in counts) counts[key] = 0
            counts[key] = counts[key]!! + 1
        }
        return counts
    }
}
