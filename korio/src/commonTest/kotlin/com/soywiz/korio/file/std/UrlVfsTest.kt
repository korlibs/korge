package com.soywiz.korio.file.std

import kotlin.test.*

class UrlVfsTest {
    @Test
    fun test() {
        assertEquals("http://127.0.0.1:8080/", UrlVfs("http://127.0.0.1:8080")["."].absolutePath)
    }
}
