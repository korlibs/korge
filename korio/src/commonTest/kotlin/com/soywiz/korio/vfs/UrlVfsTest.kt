package com.soywiz.korio.vfs

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.net.http.*
import kotlin.test.*

class UrlVfsTest {
	@Test
	fun name() = suspendTest {
		assertEquals(
			"http://test.com/demo/hello/world",
			UrlVfs("http://test.com/")["demo"].jail()["hello/world"].absolutePath
		)
		assertEquals(
			"http://test.com/demo/hello/world",
			UrlVfs("http://test.com/")["/demo"].jail()["/hello/world"].absolutePath
		)
	}

	@Test
	fun testRightRequests() = suspendTest {
		val httpClient = LogHttpClient()
		val url = UrlVfs("http://google.es/", httpClient)
		println(url.readString())
		assertEquals(
			listOf("GET, http://google.es/, Headers(), null"),
			httpClient.log
		)
	}

	@Test
	fun requestRightUrl() = suspendTest {
		val httpClient = LogHttpClient()
		val url = UrlVfs("http://google.es/demo/file.png", httpClient)
		println(url.readString())
		assertEquals(
			"[GET, http://google.es/demo/file.png, Headers(), null]",
			httpClient.getAndClearLog().toString()
		)
	}

	@Test
	fun testUrlParent() = suspendTest {
		assertEquals(
			"http://test.com/hello/",
			UrlVfs("http://test.com/hello/world")[".."].jail().absolutePath
		)
	}

    @Test
    fun testLength() = suspendTest {
        val LEN10File = UrlVfs("http://whatever", FakeHttpClient().apply { onRequest().header("Content-Length", 10).ok("hello") })
        val NOLENFile = UrlVfs("http://whatever", FakeHttpClient().apply { onRequest().ok("hello") })

        assertEquals(10L, LEN10File.stat().size)
        assertEquals(-1L, NOLENFile.stat().size)

        assertEquals(true, LEN10File.open().hasLength())
        assertEquals(false, NOLENFile.open().hasLength())

        assertEquals(10L, LEN10File.open().getLength())
    }
}
