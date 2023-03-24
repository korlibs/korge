package korlibs.io.net

import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.io.lang.*
import korlibs.io.net.http.*
import korlibs.io.stream.*
import korlibs.crypto.encoding.*
import kotlin.test.*

class MingwX64NativeSocketTest {
    @Test
    @Ignore
    fun test() = suspendTest {
        //val http = createHttpClient()
        //localVfs("C:/temp/demo.bin").writeBytes(http.request(Http.Method.GET, "https://www.google.es/").rawContent.readAll())

        //println(http.readString("https://google.es/"))

        val client = createTcpClient(secure = true)
        client.connect("www.google.es", 443)
        client.write("GET / HTTP/1.1\r\nHost: google.es\r\nConnection: close\r\n\r\n".toByteArray(UTF8))
        val bytes = client.readAll()
        println("READ BYTES ${bytes.size} ${bytes.hex}")
        println(bytes.toString(UTF8))
    }
}