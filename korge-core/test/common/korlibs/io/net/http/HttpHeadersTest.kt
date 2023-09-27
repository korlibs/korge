package korlibs.io.net.http

import kotlin.test.Test
import kotlin.test.assertEquals

class HttpHeadersTest {
    @Test
    fun test() {
        assertEquals(HttpClient.DEFAULT_USER_AGENT, HttpClient.combineHeadersForHost(Http.Headers(), null)["User-agent"])
        assertEquals("Hello", HttpClient.combineHeadersForHost(Http.Headers("user-agent" to "Hello"), null)["user-agent"])
    }
}
