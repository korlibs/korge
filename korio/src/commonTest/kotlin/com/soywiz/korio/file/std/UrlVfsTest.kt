package com.soywiz.korio.file.std

import com.soywiz.korio.async.*
import com.soywiz.korio.net.http.*
import kotlin.test.*

class UrlVfsTest {
    @Test
    fun test() {
        assertEquals("http://127.0.0.1:8080/", UrlVfs("http://127.0.0.1:8080")["."].absolutePath)
    }

    @Test
    fun testDownloadFileUsingUrlVfsWithoutReceivingContentLength() = suspendTest {
        val client = FakeHttpClient().apply {
            onRequest().ok("test")
        }
        val vfs = UrlVfs("http://demo.test", client = client)
        assertEquals(false, vfs["test"].open().hasLength())
        assertFailsWith<UnsupportedOperationException> { vfs["test"].open().getLength() }
        assertEquals("test", vfs["test"].readString())
    }
}
