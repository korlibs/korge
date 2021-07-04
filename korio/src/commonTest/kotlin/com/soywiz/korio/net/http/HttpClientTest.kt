package com.soywiz.korio.net.http

import com.soywiz.korio.async.suspendTest
import com.soywiz.krypto.encoding.fromBase64
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpClientTest {
    @Test
    fun test() = suspendTest {
        val client = FakeHttpClient().apply {
            val bytes = "H4sIAAAAAAAA/8tIzcnJBwCGphA2BQAAAA==".fromBase64() // hello in gzip+base64
            onRequest()
                .headers(Http.Headers(
                    "Content-Type" to "text/html",
                    "Content-Length" to "${bytes.size}",
                    "Connection" to "close",
                    "Content-Encoding" to "gzip",
                    "Date" to "Tue, 20 Apr 2021 06:57:22 GMT",
                    "Server" to "ECSF (dcb/7F83)",
                ))
                .ok(bytes)
        }

        val response = client.request(Http.Method.GET, "https://raw.githubusercontent.com/korlibs/korio/master/README.md")
        assertEquals("hello", response.readAllString())
    }
}
