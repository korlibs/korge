package com.soywiz.korio.net

import com.soywiz.korio.lang.*
import kotlin.test.*

class URLTest {
	data class UrlInfo(val url: String, val componentString: String, val isAbsolute: Boolean, val isOpaque: Boolean)

	val URLS = listOf(
		UrlInfo("", componentString = "URL(path=)", isAbsolute = false, isOpaque = false),
		UrlInfo("hello", componentString = "URL(path=hello)", isAbsolute = false, isOpaque = false),
		UrlInfo("/hello", componentString = "URL(path=/hello)", isAbsolute = false, isOpaque = false),
		UrlInfo(
			"/hello?world",
			componentString = "URL(path=/hello, query=world)",
			isAbsolute = false,
			isOpaque = false
		),
		UrlInfo(
			"/hello?world?world",
			componentString = "URL(path=/hello, query=world?world)",
			isAbsolute = false,
			isOpaque = false
		),
		UrlInfo("http://", componentString = "URL(scheme=http, path=)", isAbsolute = true, isOpaque = false),
		UrlInfo(
			"http://hello",
			componentString = "URL(scheme=http, host=hello, path=)",
			isAbsolute = true,
			isOpaque = false
		),
		UrlInfo(
			"http://hello/",
			componentString = "URL(scheme=http, host=hello, path=/)",
			isAbsolute = true,
			isOpaque = false
		),
		UrlInfo(
			"http://user:pass@hello",
			componentString = "URL(scheme=http, userInfo=user:pass, host=hello, path=)",
			isAbsolute = true,
			isOpaque = false
		),
		UrlInfo(
			"http://user:pass@hello/path",
			componentString = "URL(scheme=http, userInfo=user:pass, host=hello, path=/path)",
			isAbsolute = true,
			isOpaque = false
		),
		UrlInfo(
			"http://user:pass@hello/path?query",
			componentString = "URL(scheme=http, userInfo=user:pass, host=hello, path=/path, query=query)",
			isAbsolute = true,
			isOpaque = false
		),
		UrlInfo(
			"http://hello/path",
			componentString = "URL(scheme=http, host=hello, path=/path)",
			isAbsolute = true,
			isOpaque = false
		),
		UrlInfo(
			"http://hello?query",
			componentString = "URL(scheme=http, host=hello, path=, query=query)",
			isAbsolute = true,
			isOpaque = false
		),
		UrlInfo(
			"mailto:demo@host.com",
			componentString = "URL(scheme=mailto, userInfo=demo, host=host.com, path=)",
			isAbsolute = true,
			isOpaque = true
		),
		UrlInfo(
			"http://hello?query#hash",
			componentString = "URL(scheme=http, host=hello, path=, query=query, fragment=hash)",
			isAbsolute = true,
			isOpaque = false
		)
	)

	@Test
	fun testParsing() {
		for (url in URLS) assertEquals(url.componentString, URL(url.url).toComponentString(), url.url)
	}

	@Test
	fun testFullUrl() {
		for (url in URLS) assertEquals(url.url, URL(url.url).fullUrl, url.url)
	}

	@Test
	fun testIsAbsolute() {
		for (url in URLS) assertEquals(url.isAbsolute, URL(url.url).isAbsolute, url.url)
	}

	@Test
	fun testIsOpaque() {
		for (url in URLS) assertEquals(url.isOpaque, URL(url.url).isOpaque, url.url)
	}

	@Test
	fun testResolve() {
		assertEquals("https://www.google.es/", URL.resolve("https://google.es/", "https://www.google.es/"))
		assertEquals("https://google.es/demo", URL.resolve("https://google.es/path", "demo"))
		assertEquals("https://google.es/path/demo", URL.resolve("https://google.es/path/", "demo"))
		assertEquals("https://google.es/demo", URL.resolve("https://google.es/path/", "/demo"))
		assertEquals("https://google.es/test", URL.resolve("https://google.es/path/path2", "../test"))
		assertEquals("https://google.es/path/test", URL.resolve("https://google.es/path/path2/", "../test"))
		assertEquals("https://google.es/test", URL.resolve("https://google.es/path/path2/", "../../../test"))
	}

	@Test
	fun testEncode() {
		assertEquals("hello%20world", URL.encodeComponent("hello world"))
		assertEquals("hello+world", URL.encodeComponent("hello world", formUrlEncoded = true))

		assertEquals("hello%2Bworld", URL.encodeComponent("hello+world"))
		assertEquals("hello%2Bworld", URL.encodeComponent("hello+world", formUrlEncoded = true))

		assertEquals("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWZ0123456789%20-_.*", URL.encodeComponent("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWZ0123456789 -_.*"))
		assertEquals("%C3%A1%C3%A9%C3%AD%C3%B3%C3%BA", URL.encodeComponent("áéíóú"))
	}

	@Test
	fun testDecode() {
		assertEquals("hello world", URL.decodeComponent("hello%20world"))
		assertEquals("hello+world+", URL.decodeComponent("hello+world%2B"))
		assertEquals("hello world+", URL.decodeComponent("hello+world%2B", formUrlEncoded = true))
		assertEquals("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWZ0123456789 -_.*", URL.decodeComponent("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWZ0123456789%20-_.*"))
		assertEquals("áéíóú", URL.decodeComponent("%C3%A1%C3%A9%C3%AD%C3%B3%C3%BA"))
	}

	@Test
	fun test() {
		assertEquals("data:text/plain;base64,aGVsbG8td29ybGQ=", createBase64URLForData("hello-world".toByteArray(), "text/plain"))
	}

    @Test
    fun testCustomWsPort() {
        val uri = URL("ws://websocket.domain.com:8080")
        assertEquals("ws", uri.scheme)
        assertEquals(false, uri.isSecureScheme)
        assertEquals("websocket.domain.com", uri.host)
        assertEquals(8080, uri.port)
    }
}
