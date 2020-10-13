package com.soywiz.korio.net

import com.soywiz.korio.async.*
import com.soywiz.korio.net.http.*
import kotlin.test.*

class HttpClientTest {
	@Test
	fun testFullRedirections() = suspendTest {
		val httpClient = LogHttpClient().apply {
			onRequest().redirect("https://www.google.es/")
			onRequest(url = "https://www.google.es/").ok("Worked!")
		}

		assertEquals("Worked!", httpClient.request(Http.Method.GET, "https://google.es/").readAllString())
		assertEquals(
			"[GET, https://google.es/, Headers(), null, GET, https://www.google.es/, Headers((Referer, [https://google.es/])), null]",
			httpClient.getAndClearLog().toString()
		)
	}

	//@Test
	//fun testConnection() = suspendTest {
	//	val request = HttpClient().request(Http.Method.GET, "http://google.com/")
	//	assertEquals(301, request.status)
	//	assertEquals("Moved Permanently", request.statusText)
	//}
}
