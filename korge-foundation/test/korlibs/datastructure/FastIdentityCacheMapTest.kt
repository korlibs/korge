package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class FastIdentityCacheMapTest {
    @Test
    fun test() {
        val map = FastIdentityCacheMap<String, Int>()
        fun getOne(): Int = map.getOrPut("one") { 1 }
        fun getTwo(): Int = map.getOrPut("two") { 2 }
        fun getThree(): Int = map.getOrPut("three") { 3 }

        fun getMOne(): Int = map.getOrPut("one") { -1 }
        fun getMTwo(): Int = map.getOrPut("two") { -2 }
        fun getMThree(): Int = map.getOrPut("three") { -3 }

        for (n in 1..3) {
            map.clear()

            assertEquals(1, getOne())
            assertEquals(1, getMOne())
            if (n >= 2) {
                assertEquals(2, getTwo())
                assertEquals(2, getMTwo())
            }
            if (n >= 3) {
                assertEquals(3, getThree())
                assertEquals(3, getMThree())
            }

            map.clear()

            assertEquals(-1, getMOne())
            assertEquals(-1, getOne())
            if (n >= 2) {
                assertEquals(-2, getMTwo())
                assertEquals(-2, getTwo())
            }
            if (n >= 3) {
                assertEquals(-3, getMThree())
                assertEquals(-3, getThree())
            }
        }
    }
}
