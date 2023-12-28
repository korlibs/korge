package korlibs.io.net

import korlibs.io.file.*
import korlibs.io.lang.toByteArray
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun testNormalize() {
        assertEquals("g", "./g/.".pathInfo.normalize())
        assertEquals("g", "././g".pathInfo.normalize())
        assertEquals("g", "./g/.".pathInfo.normalize())
        assertEquals("g", "g/".pathInfo.normalize())
        assertEquals("/g", "/./g".pathInfo.normalize())
        assertEquals("/g", "/../g".pathInfo.normalize())
        assertEquals("g", "./g".pathInfo.normalize())
        assertEquals("g", "g/.".pathInfo.normalize())
        assertEquals("g/", "g/.".normalizeUrl())
        assertEquals("g/", "g/".normalizeUrl())
        assertEquals("g/", "./g/.".normalizeUrl())
        assertEquals("g/", "./g/.".normalizeUrl())
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

        assertEquals("http://example.com/one/two?three", URL.resolve("http://example.com", "./one/two?three"))
        assertEquals("http://example.com/one/two?three", URL.resolve("http://example.com?one", "./one/two?three"))
        assertEquals("http://example.com/one/two?three#four", URL.resolve("http://example.com", "./one/two?three#four"))
        assertEquals("https://example.com/one", URL.resolve("http://example.com/", "https://example.com/one"))
        assertEquals("http://example.com/one/two.html", URL.resolve("http://example.com/two/", "../one/two.html"))
        assertEquals("https://example2.com/one", URL.resolve("https://example.com/", "//example2.com/one"))
        assertEquals("https://example.com:8080/one", URL.resolve("https://example.com:8080", "./one"))
        assertEquals("https://example2.com/one", URL.resolve("http://example.com/", "https://example2.com/one"))
        assertEquals("https://example.com/one", URL.resolve("wrong", "https://example.com/one"))
        assertEquals("https://example.com/one", URL.resolve("https://example.com/one", ""))
        assertEquals("https://example.com/one/two.c", URL.resolve("https://example.com/one/two/", "../two.c"))
        assertEquals("https://example.com/two.c", URL.resolve("https://example.com/one/two", "../two.c"))
        assertEquals("ftp://example.com/one", URL.resolve("ftp://example.com/two/", "../one"))
        assertEquals("ftp://example.com/one/two.c", URL.resolve("ftp://example.com/one/", "./two.c"))
        assertEquals("ftp://example.com/one/two.c", URL.resolve("ftp://example.com/one/", "two.c"))
        // examples taken from rfc3986 section 5.4.2
        assertEquals("http://example.com/g", URL.resolve("http://example.com/b/c/d;p?q", "../../../g"))
        assertEquals("http://example.com/g", URL.resolve("http://example.com/b/c/d;p?q", "../../../../g"))
        assertEquals("http://example.com/g", URL.resolve("http://example.com/b/c/d;p?q", "/./g"))
        assertEquals("http://example.com/g", URL.resolve("http://example.com/b/c/d;p?q", "/../g"))
        assertEquals("http://example.com/b/c/g.", URL.resolve("http://example.com/b/c/d;p?q", "g."))
        assertEquals("http://example.com/b/c/.g", URL.resolve("http://example.com/b/c/d;p?q", ".g"))
        assertEquals("http://example.com/b/c/g..", URL.resolve("http://example.com/b/c/d;p?q", "g.."))
        assertEquals("http://example.com/b/c/..g", URL.resolve("http://example.com/b/c/d;p?q", "..g"))
        assertEquals("http://example.com/b/g", URL.resolve("http://example.com/b/c/d;p?q", "./../g"))
        assertEquals("http://example.com/b/c/g/", URL.resolve("http://example.com/b/c/d;p?q", "./g/."))
        assertEquals("http://example.com/b/c/g/h", URL.resolve("http://example.com/b/c/d;p?q", "g/./h"))
        assertEquals("http://example.com/b/c/h", URL.resolve("http://example.com/b/c/d;p?q", "g/../h"))
        assertEquals("http://example.com/b/c/g;x=1/y", URL.resolve("http://example.com/b/c/d;p?q", "g;x=1/./y"))
        assertEquals("http://example.com/b/c/y", URL.resolve("http://example.com/b/c/d;p?q", "g;x=1/../y"))
        assertEquals("http://example.com/b/c/g?y/./x", URL.resolve("http://example.com/b/c/d;p?q", "g?y/./x"))
        assertEquals("http://example.com/b/c/g?y/../x", URL.resolve("http://example.com/b/c/d;p?q", "g?y/../x"))
        assertEquals("http://example.com/b/c/g#s/./x", URL.resolve("http://example.com/b/c/d;p?q", "g#s/./x"))
        assertEquals("http://example.com/b/c/g#s/../x", URL.resolve("http://example.com/b/c/d;p?q", "g#s/../x"))
        assertEquals("https://example.com/path/bar.html?foo", URL.resolve("https://example.com/path/file?bar", "bar.html?foo"))
        assertEquals("https://example.com/path/file?foo", URL.resolve("https://example.com/path/file?bar", "?foo"))
        assertEquals("https://example.com/foo bar/", URL.resolve("https://example.com/example/", "../foo bar/"))
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
