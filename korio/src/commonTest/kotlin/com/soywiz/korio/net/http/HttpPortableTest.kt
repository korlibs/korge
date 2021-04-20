package com.soywiz.korio.net.http

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.net.*
import com.soywiz.korio.stream.*
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

    val fakeSocketFactory = object : AsyncSocketFactory() {
        override suspend fun createClient(secure: Boolean): AsyncClient = FakeAsyncClient().apply {
            serverToClient.writeString(
                "HTTP/1.0 200 OK\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length: 1\r\n" +
                    "Connection: close\r\n" +
                    "Date: Tue, 20 Apr 2021 06:57:22 GMT\r\n" +
                    "Server: ECSF (dcb/7F83)\r\n" +
                    "\r\n."
            )
        }
    }

    @Test
    fun testIgnoredWhitespaceInHeadersValue() = suspendTest {
        // this test checks "Whitespace before the value is ignored" rule
        // from https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers

        val fullUrl = "https://example.com"
        val client = HttpPortable(fakeSocketFactory).createClient()

        val result = client.request(Http.Method.HEAD, fullUrl)
        assertTrue(result.headers.items.isNotEmpty(), "headers of a valid request should be non-empty")
        assertTrue(
            result.headers.items.all { (_, v) -> v.trimStart() == v },
            "Whitespace before the value isn't ignored: ${result.headers}"
        )
    }

    @Test
    fun testUrlVfsRead() = suspendTest {
        assertTrue(UrlVfs("https://example.com", client = HttpPortable(fakeSocketFactory).createClient()).readAll().isNotEmpty())
        assertTrue(UrlVfs("https://example.com", client = HttpPortable(fakeSocketFactory).createClient()).open().readAll().isNotEmpty())
    }
}
