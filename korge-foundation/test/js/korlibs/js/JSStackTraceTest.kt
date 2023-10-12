package korlibs.js

import kotlin.test.*

class JSStackTraceTest {
    @Test
    fun testEmpty() {
        val stack = JSStackTrace.parse("")
        assertEquals(JSStackTrace("", listOf(
            JSStackTrace.Entry("<unknown>", "<unknown>", -1)
        )), stack)
    }

    @Test
    fun test() {
        //new Error().stack
        //'Error\n    at <anonymous>:1:1'
        val stack = JSStackTrace.parse("""
            Error
                at <anonymous>:1:1
        """.trimIndent())
        assertEquals(JSStackTrace("", listOf(
            JSStackTrace.Entry("", "<anonymous>", 1, 1)
        )), stack)
    }

    @Test
    fun testChromeMultiLineMessage() {
        //new Error("hello\nworld\n").stack
        //'Error: hello\nworld\n\n    at <anonymous>:1:1'
        val stack = JSStackTrace.parse("Error: hello\nworld\n\n    at <anonymous>:1:1")
        assertEquals(JSStackTrace("hello\nworld\n", listOf(
            JSStackTrace.Entry("", "<anonymous>", 1, 1)
        )), stack)
    }


    @Test
    fun testChrome() {
        val stack = JSStackTrace.parse("""
            Error: test
                at a (demo.html:3:14)
                at b (demo.html:6:2)
                at demo.html:8:1
        """.trimIndent(), "test")
        assertEquals(JSStackTrace("test", listOf(
            JSStackTrace.Entry("a", "demo.html", 3, 14),
            JSStackTrace.Entry("b", "demo.html", 6, 2),
            JSStackTrace.Entry("", "demo.html", 8, 1),
        )), stack)
    }

    @Test
    fun testFirefox() {
        val stack = JSStackTrace.parse("""
            a@http://127.0.0.1:8080/demo.html:3:14
            b@http://127.0.0.1:8080/demo.html:6:2
            @http://127.0.0.1:8080/demo.html:8:1
        """.trimIndent(), "test")
        assertEquals(JSStackTrace("test", listOf(
            JSStackTrace.Entry("a", "http://127.0.0.1:8080/demo.html", 3, 14),
            JSStackTrace.Entry("b", "http://127.0.0.1:8080/demo.html", 6, 2),
            JSStackTrace.Entry("", "http://127.0.0.1:8080/demo.html", 8, 1),
        )), stack)
    }

    @Test
    fun testFirefox14_29() {
        val stack = JSStackTrace.parse("""
            a@http://127.0.0.1:8080/demo.html:3
            b@http://127.0.0.1:8080/demo.html:6
            @http://127.0.0.1:8080/demo.html:8
        """.trimIndent(), "test")
        assertEquals(JSStackTrace("test", listOf(
            JSStackTrace.Entry("a", "http://127.0.0.1:8080/demo.html", 3),
            JSStackTrace.Entry("b", "http://127.0.0.1:8080/demo.html", 6),
            JSStackTrace.Entry("", "http://127.0.0.1:8080/demo.html", 8),
        )), stack)
    }

    @Test
    fun testFirefox1_13() {
        val stack = JSStackTrace.parse("""
            Error("myError")@:0
            trace()@file:///C:/example.html:9
            b(3,4,"\n\n",(void 0),[object Object])@file:///C:/example.html:16
            a("first call, firstarg")@file:///C:/example.html:19
            @file:///C:/example.html:21
        """.trimIndent(), "test")
        assertEquals(JSStackTrace("test", listOf(
            JSStackTrace.Entry("Error(\"myError\")", "", 0),
            JSStackTrace.Entry("trace()", "file:///C:/example.html", 9),
            JSStackTrace.Entry("b(3,4,\"\\n\\n\",(void 0),[object Object])", "file:///C:/example.html", 16),
            JSStackTrace.Entry("a(\"first call, firstarg\")", "file:///C:/example.html", 19),
            JSStackTrace.Entry("", "file:///C:/example.html", 21),
        )), stack)

    }

    @Test
    fun testSafari() {
        val stack = JSStackTrace.parse("""
            a@http://127.0.0.1:8080/demo.html:3:23
            b@http://127.0.0.1:8080/demo.html:6:3
            global code@http://127.0.0.1:8080/demo.html:8:2
        """.trimIndent(), "test")
        assertEquals(JSStackTrace("test", listOf(
            JSStackTrace.Entry("a", "http://127.0.0.1:8080/demo.html", 3, 23),
            JSStackTrace.Entry("b", "http://127.0.0.1:8080/demo.html", 6, 3),
            JSStackTrace.Entry("global code", "http://127.0.0.1:8080/demo.html", 8, 2),
        )), stack)

    }
}
