package korlibs.io.vfs

import korlibs.io.async.suspendTest
import korlibs.io.file.std.LocalVfs
import korlibs.io.file.std.MemoryVfs
import korlibs.io.file.std.UniSchema
import korlibs.io.file.std.UniversalVfs
import korlibs.io.file.std.UrlVfs
import korlibs.io.file.std.defaultUniSchema
import korlibs.io.file.std.plus
import korlibs.io.file.std.registerUniSchemaTemporarily
import korlibs.io.file.std.uniVfs
import korlibs.io.lang.InvalidOperationException
import korlibs.io.net.http.LogHttpClient
import korlibs.io.util.expectException
import korlibs.platform.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UniversalVfsTest {
    private val fileIsLocalVfs get() = !Platform.isJsBrowser && !Platform.isNative

	@Test
	fun testProperVfsIsResolved() {
		if (fileIsLocalVfs) {
			assertTrue("file:///path/to/my/file".uniVfs.vfs is LocalVfs)
		}
		assertTrue("http://google.es/".uniVfs.vfs is UrlVfs)
		assertTrue("https://google.es/".uniVfs.vfs is UrlVfs)
	}

	@Test
	fun testProperPathIsResolved() {
		if (fileIsLocalVfs) {
			assertEquals("/path/to/my/file", "file:///path/to/my/file".uniVfs.absolutePath)
		}
		assertEquals("http://google.es/", "http://google.es/".uniVfs.absolutePath)
		assertEquals("https://google.es/", "https://google.es/".uniVfs.absolutePath)
	}

	@Test
	fun testProperRequestIsDone() = suspendTest {
		val httpClient = LogHttpClient().apply {
			onRequest().redirect("https://www.google.es/")
			onRequest(url = "https://www.google.es/").response("Worked!")
		}

		assertEquals(
			"Worked!",
			UniversalVfs(
				"https://google.es/",
				defaultUniSchema + UniSchema("https") { UrlVfs(it, httpClient) }
			).readString()
		)

		assertEquals(
			"[GET, https://google.es/, Headers(), null, GET, https://www.google.es/, Headers((Referer, [https://google.es/])), null]",
			httpClient.getAndClearLog().toString()
		)
	}

	@Test
	fun testTemporalSet() = suspendTest {
		var called = false
		val mem = MemoryVfs()
		registerUniSchemaTemporarily(UniSchema("mem") {
			mem[it.fullUrlWithoutScheme]
		}) {
			"mem://hello.txt".uniVfs.writeString("HELLO")
			assertEquals("HELLO", "mem://hello.txt".uniVfs.readString())
			called = true
		}
		assertEquals(true, called)

		expectException<InvalidOperationException> {
			"mem://hello.txt".uniVfs.readString()
		}
	}
}
