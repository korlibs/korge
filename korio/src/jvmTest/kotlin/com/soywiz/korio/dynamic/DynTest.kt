package com.soywiz.korio.dynamic

import org.junit.*
import org.junit.Test
import kotlin.test.*

class DynTest {
    @Test
    fun test() {
        assertEquals("10", Dyn.global["java.lang.String"].dynamicInvoke("valueOf", 10).value)
        assertEquals("10", Dyn.global["java"]["lang.String"].dynamicInvoke("valueOf", 10).value)
        assertEquals("HELLO", "hello".dyn.dynamicInvoke("toUpperCase").value)
        assertEquals("a", Demo().dyn.dynamicInvoke("demo").value)
        assertEquals("b", Demo().dyn.dynamicInvoke("demo", "1").value)
        assertEquals("c", Demo().dyn.dynamicInvoke("demo", 1).value)
    }

    @Test
    fun test2() {
        assertEquals("c", Demo().dyn.dynamicInvoke("demo", 1).value)
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    class Demo {
        fun demo(): String = "a"
        fun demo(a: String): String = "b"
        fun demo(a: Int): String = "c"
    }

    @Test
    fun test3() {
        assertEquals(10, mapOf("a" to 10).dyn["a"].int)
    }

    @Suppress("unused")
    class Demo2 {
        var z = 3
        fun setA(value: Int) {
            z = value * 2
        }
        fun getA() = z * 3
    }

    @Test
    fun test4() {
        assertEquals(20, Demo2().also { it.dyn["a"] = 10 }.z)
        assertEquals(9, Demo2().dyn["a"].value )
        assertEquals(3, Demo2().dyn["z"].value )
    }

    @Test
    fun test5() {
        assertEquals(Integer.TYPE, Dyn.global["java.lang.Integer"]["TYPE"].value)
    }
}
