package korlibs.io.dynamic

import kotlin.test.*

class DynCommonTest {
    val data1 = mapOf("a" to mapOf("b" to 10)).dyn

    @Test
    fun test() {
        assertEquals(10, data1["a"]["b"].int)
    }

    @Test
    fun testBinop() {
        assertEquals(11, (10.0.dyn + 1.dyn).int)
        assertEquals(9, (10.0.dyn - 1.dyn).int)
        assertEquals(20, (10.0.dyn * 2.dyn).int)
        assertEquals(5, (10.0.dyn / 2.0.dyn).int)
        assertEquals(0, (10.0.dyn % 2.0.dyn).int)
        assertEquals(100, (10.0.dyn pow 2.0.dyn).int)
        assertEquals(2, (3.dyn bitAnd 6.dyn).int)
        assertEquals(7, (3.dyn bitOr 6.dyn).int)
        assertEquals(5, (3.dyn bitXor 6.dyn).int)
        assertEquals(true, (3.dyn and 6.dyn))
        assertEquals(true, (3.dyn or 6.dyn))
    }

    @Test
    fun testSEq() {
        val THREE = 3.dyn
        val FOUR = 4.dyn
        val THREE_STR = "3".dyn
        val FOUR_STR = "4".dyn
        assertEquals(true, (THREE seq THREE), "a")
        assertEquals(false, (THREE seq FOUR), "b")

        assertEquals(false, (THREE sne THREE), "c")
        assertEquals(true, (THREE sne FOUR), "d")

        assertEquals(false, (THREE seq THREE_STR), "e")
        assertEquals(false, (THREE seq FOUR_STR), "f")
    }

    @Test
    @Ignore // This fails in WASM because probably it is not caching small numbers
    fun testSEq2() {
        assertEquals(true, (3.dyn seq 3.dyn), "a")
        assertEquals(false, (3.dyn seq 4.dyn), "b")

        assertEquals(false, (3.dyn sne 3.dyn), "c")
        assertEquals(true, (3.dyn sne 4.dyn), "d")

        assertEquals(false, (3.dyn seq "3".dyn), "e")
        assertEquals(false, (3.dyn seq "4".dyn), "f")
    }

    @Test
    fun testEq() {
        assertEquals(true, (3.dyn eq "3".dyn), "a")
        assertEquals(false, (3.dyn eq "4".dyn), "b")

        assertEquals(false, (3.dyn ne "3".dyn), "c")
        assertEquals(true, (3.dyn ne "4".dyn), "d")
    }

    @Test
    fun testList() {
        assertEquals(emptyList(), null.dyn.toList())
        assertEquals(listOf("1", "2", "3"), "123".dyn.toList().map { it.str })
        assertEquals(listOf("1", "2", "3"), listOf(1, 2, 3).dyn.toList().map { it.str })
        assertEquals(listOf("(a, 1)", "(b, 2)"), mapOf("a" to 1, "b" to 2).dyn.toList().map { it.str })
    }
}
