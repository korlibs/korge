package com.soywiz.kds

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExtraPropertyTest {
    var Demo.demo1 by extraProperty { "hello" }
    val Demo.demo by extraProperty { linkedSetOf<String>() }
    class Demo : Extra by Extra.Mixin()

    @Test
    fun test1() {
        val demo = Demo()
        assertEquals("hello", demo.demo1)
        demo.demo1 = "test"
        assertEquals("test", demo.demo1)
    }

    @Test
    fun test2() {
        val demo = Demo()
        demo.demo.add("hello")
        assertTrue { "hello" in demo.demo }
    }
}
