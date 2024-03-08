package korlibs.io.net

import korlibs.io.async.suspendTest
import korlibs.io.net.http.Http
import korlibs.io.net.http.LogHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals

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
