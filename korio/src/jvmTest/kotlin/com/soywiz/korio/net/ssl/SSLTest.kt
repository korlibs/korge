package com.soywiz.korio.net.ssl

import com.soywiz.korio.async.*
import com.soywiz.korio.net.http.*
import kotlin.test.*

class SSLTest {
    @Test
    //@Ignore
    fun testDownloadHttpsFile() = suspendTest {
        val client = createHttpClient()
        val result = client.requestAsString(Http.Method.GET, "https://docs.korge.org/ssltest.txt")
        //println(result.headers)
        //println(result.content)

        assertEquals("file used for SSL tests\n", result.content)
    }
}
