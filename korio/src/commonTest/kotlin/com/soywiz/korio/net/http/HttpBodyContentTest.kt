package com.soywiz.korio.net.http

import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.lang.UTF8
import com.soywiz.korio.lang.toString
import com.soywiz.korio.stream.openAsync
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpBodyContentTest {
    @Test
    fun test() = suspendTest {
        val data = HttpBodyContent(
            "application/x-www-form-urlencoded"
        ) { "hello=world&demo=test".openAsync() }
        assertEquals(
            """
                application/x-www-form-urlencoded
                hello=world&demo=test
            """.trimIndent(),
            data.toDebugString()
        )
    }

    @Test
    fun testHttpClient() = suspendTest {
        val log = arrayListOf<String>()
        //val client = HttpClient()
        val client = FakeHttpClient().apply {
            onRequest().handler { method, url, headers, content ->
                log += "method=$method\nurl=$url\nheaders=$headers\ncontent=${content?.toString(UTF8)}"
                HttpClient.Response(200, "test", Http.Headers(), "".openAsync())
            }
        }
        val result = client.post("http://127.0.0.1:8080/", HttpBodyContent("application/x-www-form-urlencoded", "hello=world&demo=test"))
            .readAllString()
        if (result.isNotEmpty()) println(result)
        assertEquals(
            """
                method=POST
                url=http://127.0.0.1:8080/
                headers=Headers((Content-Length, [21]), (Content-Type, [application/x-www-form-urlencoded]))
                content=hello=world&demo=test
            """.trimIndent(),
            log.joinToString("\n\n")
        )
    }
}
