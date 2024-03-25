package korlibs.io.dynamic

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

    @Test
    fun testToStringOrNull() {
        assertEquals("test", Dyn("test").toStringOrNull())
        assertEquals("10", Dyn(10).toStringOrNull())
        assertEquals("null", Dyn("null").toStringOrNull())
        assertEquals("true", Dyn(true).toStringOrNull())
        assertEquals("false", Dyn(false).toStringOrNull())
        assertEquals(null, Dyn(null).toStringOrNull())
    }

    @Test
    fun testContains() {
        val list = listOf("a", "b")
        val set = setOf("a", "b")
        val map = mapOf("a" to 1, "b" to 2)

        assertEquals(true, "hello".dyn.contains("ll"))
        assertEquals(false, "hello".dyn.contains("le"))

        assertEquals(true, map.dyn.contains("a"))
        assertEquals(true, map.dyn.contains("b"))
        assertEquals(false, map.dyn.contains("c"))

        assertEquals(true, set.dyn.contains("a"))
        assertEquals(true, set.dyn.contains("b"))
        assertEquals(false, set.dyn.contains("c"))

        assertEquals(true, list.dyn.contains("a"))
        assertEquals(true, list.dyn.contains("b"))
        assertEquals(false, list.dyn.contains("c"))
    }
}
