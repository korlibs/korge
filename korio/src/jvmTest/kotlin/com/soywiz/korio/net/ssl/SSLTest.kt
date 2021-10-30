package com.soywiz.korio.net.ssl

import com.soywiz.korio.async.*
import com.soywiz.korio.net.http.*
import kotlin.test.*

class SSLTest {
    @Test
    @Ignore
    fun test() = suspendTest {
        val client = createHttpClient()
        val content = client.requestAsString(Http.Method.GET, "https://google.es/")
        //val content = client.requestAsString(Http.Method.GET, "http://google.es/")
        println(content.headers)
        println(content.content)
    }
}
