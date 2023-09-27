package korlibs.datastructure.algo

import korlibs.datastructure.*
import kotlin.test.*

class HistoriogramTest {
    @Test
    fun test() {
        assertEquals(
            intIntMapOf((1 to 3), (5 to 2), (9 to 1)),
            Historiogram.values(intArrayOf(1, 1, 5, 1, 9, 5))
        )
    }

    @Test
    fun test2() {
        val a = Historiogram()
        a.also { it.add(1) }.also { it.add(2) }
        val b = a.clone()
        a.also { it.add(3) }
        b.also { it.add(1) }
        assertEquals(intIntMapOf(1 to 1, 2 to 1, 3 to 1), a.getMapCopy())
        assertEquals(intIntMapOf(1 to 2, 2 to 1), b.getMapCopy())
    }
}
