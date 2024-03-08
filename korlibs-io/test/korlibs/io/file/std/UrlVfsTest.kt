package korlibs.io.file.std

import korlibs.io.async.suspendTest
import korlibs.io.net.http.FakeHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UrlVfsTest {
    @Test
    fun test() {
        assertEquals("http://127.0.0.1:8080/", UrlVfs("http://127.0.0.1:8080")["."].absolutePath)
    }

    @Test
    fun testDownloadFileUsingUrlVfsWithoutReceivingContentLength() = suspendTest {
        val client = FakeHttpClient().apply {
            onRequest().ok("test")
        }
        val vfs = UrlVfs("http://demo.test", client = client)
        assertEquals(false, vfs["test"].open().hasLength())
        assertFailsWith<UnsupportedOperationException> { vfs["test"].open().getLength() }
        assertEquals("test", vfs["test"].readString())
    }
}
