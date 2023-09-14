package korlibs.memory.pack

import kotlin.test.*

class Int2PackTest {
    @Test
    fun test() {
        val pack1 = int2PackOf(100, 200)
        val pack2 = int2PackOf(-33, -77)
        assertEquals(300, pack1.i0 + pack1.i1)
        assertEquals(-110, pack2.i0 + pack2.i1)
        assertEquals(listOf(100, 200), listOf(pack1.i0, pack1.i1))
        assertEquals("-999", (-999L).toString()) // Ensure we have not modified the -999 constant
    }

    @Test
    fun testCompareValue() {
        assertTrue("zeroEquals") { int2PackOf(0, 0) == int2PackOf(0, 0) }
    }

    @Test
    fun testCompareObject() {
        assertEquals(int2PackOf(0, 0), int2PackOf(0, 0))
    }

    @Test
    @Ignore
    fun performanceTest() {
        var sum = 0f
        for (n in 0 until 100000000) {
            val pack1 = int2PackOf(100 * n, 200)
            val pack2 = int2PackOf(-33, -77 * n)
            sum += pack1.i0 + pack1.i1
            sum += pack2.i0 + pack2.i1
        }
        println(sum)
    }
}
