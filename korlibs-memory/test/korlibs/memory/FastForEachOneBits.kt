package korlibs.memory

import kotlin.test.*

class FastForEachOneBits {
    fun Int.listOneIndices(): List<Int> {
        val log = arrayListOf<Int>()
        fastForEachOneBits { log += it }
        return log
    }
    @Test
    fun test() {
        assertEquals(listOf(), 0.listOneIndices())
        assertEquals(listOf(0), 1.listOneIndices())
        assertEquals(listOf(1), 2.listOneIndices())
        assertEquals(listOf(0, 1), 3.listOneIndices())
        assertEquals(listOf(0, 5), 0b100001.listOneIndices())
        assertEquals(listOf(0, 1, 6, 10, 11), 0b110001000011.listOneIndices())
        assertEquals((0..31).toList(), (-1).listOneIndices())
    }
}
