package com.soywiz.korge.view

import com.soywiz.korma.geom.*
import kotlin.test.*

class HitTestDirectionTest {
    @Test
    fun test() {
        assertEquals(HitTestDirection.RIGHT, HitTestDirection.fromAngle(0.degrees))
        assertEquals(HitTestDirection.DOWN, HitTestDirection.fromAngle(90.degrees))
        assertEquals(HitTestDirection.LEFT, HitTestDirection.fromAngle(180.degrees))
        assertEquals(HitTestDirection.UP, HitTestDirection.fromAngle(270.degrees))

        //println("x=${0.degrees.cosine}, y=${0.degrees.sine}")
        //println("x=${90.degrees.cosine}, y=${90.degrees.sine}")
    }
}
