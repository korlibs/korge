package korlibs.io.net.http

import korlibs.io.async.suspendTest
import korlibs.io.lang.UTF8
import korlibs.io.lang.toString
import korlibs.io.stream.openAsync
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpBodyContentMultiPartFormDataTest {
    @Test
    fun test() = suspendTest {
        val data = HttpBodyContentMultiPartFormData("...123")
            .add("hello", "world")
            .add("demo", "test", fileName = "file.txt")
            .add("hey", "lol", fileName = "wat.html", contentType = "text/html")

        assertEquals(
            """
                multipart/form-data; boundary=...123
                --...123
                Content-Disposition: form-data; name="hello"
                
                world
                --...123
                Content-Disposition: form-data; name="demo"; filename="file.txt"
                
                test
                --...123
                Content-Disposition: form-data; name="hey"; filename="wat.html"
                Content-Type: text/html
                
                lol
                --...123--
            """.trimIndent(),
            data.toDebugString()
        )
    }

    @Test
    fun testHttpClient() = suspendTest {
        val log = arrayListOf<String>()
        //val client = HttpClient()
        val client = FakeHttpClient().apply {
            onRequest().handler { method, url, headers, content ->
                log += "method=$method\nurl=$url\nheaders=$headers\ncontent=${content?.toString(UTF8)}"
                HttpClient.Response(200, "test", Http.Headers(), "".openAsync())
            }
        }
        val result = client.post("http://127.0.0.1:8080/", HttpBodyContentMultiPartFormData("...123")
            .add("hello", "world")
            .add("demo", "test", fileName = "file.txt")
            .add("hey", "lol", fileName = "wat.html", contentType = "text/html")
        ).readAllString()
        if (result.isNotEmpty()) println(result)
        assertEquals(
            """
                method=POST
                url=http://127.0.0.1:8080/
                headers=Headers((Content-Length, [253]), (Content-Type, [multipart/form-data; boundary=...123]))
                content=--...123
                Content-Disposition: form-data; name="hello"
                
                world
                --...123
                Content-Disposition: form-data; name="demo"; filename="file.txt"
                
                test
                --...123
                Content-Disposition: form-data; name="hey"; filename="wat.html"
                Content-Type: text/html
                
                lol
                --...123--
            """.trimIndent(),
            log.joinToString("\n\n")
        )
    }
}
