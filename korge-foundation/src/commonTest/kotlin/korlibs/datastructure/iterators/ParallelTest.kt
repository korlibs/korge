package korlibs.datastructure.iterators

import korlibs.datastructure.*
import kotlin.test.*

class ParallelTest {
    @Test
    fun test() {
        //println("ParallelTest.CONCURRENCY_COUNT: $CONCURRENCY_COUNT")
        assertTrue("CONCURRENCY_COUNT:$CONCURRENCY_COUNT >= 1") { CONCURRENCY_COUNT >= 1 }
        //for (n in 0 until 3000) {
        for (n in 0 until 128) {
            val list = (0 until n).mapInt { it }
            //assertEquals(list.map { it * 2 }, list.parallelMap { it * 2 })
            assertEquals(list.mapInt { it * 2 }.toIntArrayList(), list.parallelMapInt { it * 2 }.toIntArrayList())
        }
    }
}
