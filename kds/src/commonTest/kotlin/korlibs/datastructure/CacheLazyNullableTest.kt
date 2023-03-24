package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class CacheLazyNullableTest {
    private var called = 0
    private var _myfield: Int? = null
    val myfield: Int get() = cacheLazyNullable(::_myfield) { 10.also { called++ } }

    @Test
    fun test() {
        assertEquals(0, called)
        repeat(3) { assertEquals(10, myfield) }
        assertEquals(10, myfield)
        assertEquals(1, called)
    }
}
