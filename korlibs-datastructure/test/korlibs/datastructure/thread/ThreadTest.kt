package korlibs.datastructure.thread

import korlibs.datastructure.*
import kotlin.test.*

class ThreadTest {
    @Test
    fun test() {
        val KEY = "hello"
        val VALUE = "world"
        val extra = NativeThread { }.extra
        extra.setExtra(KEY, VALUE)
        assertEquals(VALUE, extra.getExtra(KEY))
    }
}
