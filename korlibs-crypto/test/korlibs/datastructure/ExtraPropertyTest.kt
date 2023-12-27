package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private var Demo.demo1 by extraProperty { "hello" }
private val Demo.demo by extraProperty { linkedSetOf<String>() }
private var Demo.demo2 by extraPropertyThis(transform = { if (it % 10 == 0) it else it - (it % 10) }) { 10 }
private class Demo : Extra by Extra.Mixin()

class ExtraPropertyTest {
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

    @Test
    fun testTransformer() {
        val demo = Demo()
        assertEquals(10, demo.demo2)
        demo.demo2 = 12
        assertEquals(10, demo.demo2)
        demo.demo2 = 22
        assertEquals(20, demo.demo2)
        demo.demo2 = 39
        assertEquals(30, demo.demo2)
    }
}
