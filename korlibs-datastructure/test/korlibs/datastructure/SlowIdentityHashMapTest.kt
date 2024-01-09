package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class SlowIdentityHashMapTest {
    @Test
    fun test() {
        data class Test(val a: Int)
        val i1 = Test(1)
        val i2 = Test(1)
        val map = hashMapOf(i1 to 1, i2 to 2)
        val imap = slowIdentityHashMapOf(i1 to 1, i2 to 2)
        assertEquals(2, map[i1])
        assertEquals(2, map[i2])
        assertEquals(1, imap[i1])
        assertEquals(2, imap[i2])
    }
}
