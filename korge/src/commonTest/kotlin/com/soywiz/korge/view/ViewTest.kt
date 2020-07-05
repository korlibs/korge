package com.soywiz.korge.view

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
}
