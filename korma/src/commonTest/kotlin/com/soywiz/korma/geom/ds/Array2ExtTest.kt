package com.soywiz.korma.geom.ds

import com.soywiz.kds.Array2
import com.soywiz.korma.geom.PointInt
import kotlin.test.Test
import kotlin.test.assertEquals

class Array2ExtTest {
    val array = Array2(10, 10) { 0 }

    @Test
    fun test() {
        array[PointInt(5, 5)] = 10
        assertEquals(10, array[5, 5])
    }
}
