package com.soywiz.korio.net

import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.net.http.FakeHttpClient
import com.soywiz.korio.net.http.rest
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpRestClientTest {
    @Test
    fun test() = suspendTest {
        val client = FakeHttpClient()
        val rest = client.rest("http://example.com/api/")
        rest.post("method", mapOf<String, Any?>())
        assertEquals(
            listOf("POST, http://example.com/api/method, Headers((Content-Length, [2]), (Content-Type, [application/json])), {}"),
            client.log
        )
    }
}
