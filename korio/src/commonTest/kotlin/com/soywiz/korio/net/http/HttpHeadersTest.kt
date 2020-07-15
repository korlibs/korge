package com.soywiz.korio.net.http

import kotlin.test.*

class HttpHeadersTest {
    @Test
    fun test() {
        assertEquals(HttpClient.DEFAULT_USER_AGENT, HttpClient.combineHeadersForHost(Http.Headers(), null)["User-agent"])
        assertEquals("Hello", HttpClient.combineHeadersForHost(Http.Headers("user-agent" to "Hello"), null)["user-agent"])
    }
}
