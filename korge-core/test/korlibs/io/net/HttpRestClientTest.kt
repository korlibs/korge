package korlibs.io.net

import korlibs.io.async.*
import korlibs.io.net.http.*
import korlibs.io.serialization.json.*
import kotlin.test.*

class HttpRestClientTest {
    @Test
    fun test() = suspendTest {
        val client = FakeHttpClient()
        val rest = client.rest("http://example.com/api/")
        rest.post("method", Json.stringify(mapOf<String, Any?>()))
        assertEquals(
            listOf("POST, http://example.com/api/method, Headers((Content-Length, [2]), (Content-Type, [application/json])), {}"),
            client.log
        )
    }
}
