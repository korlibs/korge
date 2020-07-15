package com.soywiz.korio.net.http

import com.soywiz.korio.net.*
import kotlin.test.*

class HttpPortableTest {
    @Test
    fun test() {
        assertEquals(
            "GET / HTTP/1.1\r\n" +
                "Hello: World\r\n" +
            "\r\n",
            HttpPortable.computeHeader(Http.Method.GET, URL("Http://localhost:8000/"), Http.Headers("Hello" to "World"))
        )
    }
}
