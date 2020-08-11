package com.soywiz.korge.view

import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class ViewTest {
    @Test
    fun testAncestorCount() {
        val v0: View? = null
        assertEquals(0, v0.ancestorCount)
        val c2 = Container()
        val c = Container()
        val v = DummyView()
        assertEquals(0, v.ancestorCount)
        c.addChild(v)
        assertEquals(1, v.ancestorCount)
        c2.addChild(c)
        assertEquals(2, v.ancestorCount)
    }

    @Test
    fun testPositionRelativeTo() {
        lateinit var rect: SolidRect
        lateinit var rectParent: Container
        val container = Container().apply {
            scale = 2.0
            position(10, 10)
            rectParent = container {
                scale = 3.0
                rect = solidRect(100, 100).position(30, 30)
            }
        }
        assertEquals("(30, 30), (90, 90)", "${rect.pos}, ${rect.getPositionRelativeTo(container)}")
        rect.setPositionRelativeTo(container, Point(240, 240))
        assertEquals("(80, 80), (240, 240)", "${rect.pos}, ${rect.getPositionRelativeTo(container)}")
    }
}
