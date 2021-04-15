package com.soywiz.korio.net.http

import com.soywiz.korio.async.suspendTest
import kotlin.test.*

class HttpHeadersTest {
    @Test
    fun test() {
        assertEquals(HttpClient.DEFAULT_USER_AGENT, HttpClient.combineHeadersForHost(Http.Headers(), null)["User-agent"])
        assertEquals("Hello", HttpClient.combineHeadersForHost(Http.Headers("user-agent" to "Hello"), null)["user-agent"])
    }
    @Test
    fun testIgnoredWhitespaceInHeadersValue() = suspendTest {
        // this test checks "Whitespace before the value is ignored" rule
        // from https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers

        val fullUrl = "https://example.com"
        val client = HttpPortable.createClient()

        val result = client.request(Http.Method.HEAD, fullUrl)
        assertTrue(result.headers.items.isNotEmpty(), "headers of a valid request should be non-empty")
        assertTrue(
            result.headers.items.all { (_, v) -> v.trimStart() == v },
            "Whitespace before the value isn't ignored: ${result.headers}"
        )
    }
}
