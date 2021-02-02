package com.soywiz.korio.dynamic

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
    fun testEq() {
        assertEquals(true, (3.dyn seq 3.dyn))
        assertEquals(false, (3.dyn seq 4.dyn))

        assertEquals(false, (3.dyn sne 3.dyn))
        assertEquals(true, (3.dyn sne 4.dyn))

        assertEquals(false, (3.dyn seq "3".dyn))
        assertEquals(false, (3.dyn seq "4".dyn))

        assertEquals(true, (3.dyn eq "3".dyn))
        assertEquals(false, (3.dyn eq "4".dyn))

        assertEquals(false, (3.dyn ne "3".dyn))
        assertEquals(true, (3.dyn ne "4".dyn))
    }
}
