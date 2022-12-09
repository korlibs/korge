package com.soywiz.korio.net

import com.soywiz.korio.async.*
import com.soywiz.korio.net.http.*
import com.soywiz.korio.serialization.json.*
import kotlin.test.*

class HttpRestClientTest {
    @Test
    fun test() = suspendTest {
        val client = FakeHttpClient()
        val rest = client.rest("http://example.com/api/")
        rest.post("method", Json.stringify(mapOf<String, Any?>()))
        assertEquals(
            listOf("POST, http://example.com/api/method, Headers((Content-Length, [2]), (Content-Type, [application/json])), {}"),
            client.log
        )
    }
}
