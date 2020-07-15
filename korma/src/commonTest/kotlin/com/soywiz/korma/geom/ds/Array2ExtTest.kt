package com.soywiz.korma.geom.ds

import com.soywiz.kds.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class Array2ExtTest {
    val array = Array2(10, 10) { 0 }

    @Test
    fun test() {
        array[PointInt(5, 5)] = 10
        assertEquals(10, array[5, 5])
    }
}
