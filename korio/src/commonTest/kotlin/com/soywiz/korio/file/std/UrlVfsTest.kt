package com.soywiz.korio.file.std

import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.stream.readAll
import kotlin.test.*

class UrlVfsTest {
    @Test
    fun test() {
        assertEquals("http://127.0.0.1:8080/", UrlVfs("http://127.0.0.1:8080")["."].absolutePath)
    }
    @Test
    fun testUrlVfsRead() = suspendTest {
        assertTrue(UrlVfs("https://example.com").readAll().isNotEmpty())
        assertTrue(UrlVfs("https://example.com").open().readAll().isNotEmpty())
    }
}
