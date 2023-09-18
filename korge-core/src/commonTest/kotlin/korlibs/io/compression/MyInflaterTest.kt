package korlibs.io.compression

import korlibs.platform.Platform
import korlibs.io.async.suspendTest
import korlibs.io.compression.deflate.Deflate
import korlibs.io.compression.deflate.DeflatePortable
import korlibs.io.lang.UTF8
import korlibs.io.lang.toString
import korlibs.io.stream.openAsync
import korlibs.io.stream.readAll
import korlibs.encoding.unhexIgnoreSpaces
import kotlin.test.Test
import kotlin.test.assertEquals

class MyInflaterTest {
	@Test
	fun test() {
		val result = "CB 48 CD C9 C9 57 28 CF 2F CA 49 01 00".unhexIgnoreSpaces.uncompress(DeflatePortable)
		assertEquals("hello world", result.toString(UTF8))
	}

	@Test
	fun test2() {
		val result = "CB 48 CD C9 C9 57 28 CF 2F CA 49 D1 51 C8 20 86 03 00".unhexIgnoreSpaces.uncompress(DeflatePortable)
		assertEquals("hello world, hello world, hello world, hello world", result.toString(UTF8))
	}

	@Test
	fun test2Stream() = suspendTest {
		val input = "CB 48 CD C9 C9 57 28 CF 2F CA 49 D1 51 C8 20 86 03 00".unhexIgnoreSpaces.openAsync()
		val result = DeflatePortable.uncompressStream(input).readAll()
		assertEquals("hello world, hello world, hello world, hello world", result.toString(UTF8))
	}

	@Test
	fun test2Stream2() {
		val result = "CB 48 CD C9 C9 57 28 CF 2F CA 49 D1 51 C8 20 86 03 00".unhexIgnoreSpaces.uncompress(DeflatePortable)
		assertEquals("hello world, hello world, hello world, hello world", result.toString(UTF8))
	}

	@Test
	fun test3a() {
		val compressed = "ED C2 31 0D 00 00 00 02 A0 4A 06 B2 7F 0E 53 F8 31 68 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 9C 0D".unhexIgnoreSpaces
		val result = compressed.uncompress(DeflatePortable)
		for (n in 0 until 1000) {
			compressed.uncompress(DeflatePortable)
		}
		assertEquals(0x5000, result.size)
		assertEquals("\\0".repeat(0x2800), result.toString(UTF8))
	}

	@Test
	fun test3b() {
        // On Windows new memory manager, there is a crash here in tests in CI. Happens frequently but it is not consistently.
        // - https://youtrack.jetbrains.com/issue/KT-52651/Native-korio-tests-crash-on-Windows-with-new-memory-manager-enabled
        // - https://discord.com/channels/728582275884908604/728616376910217258/1011220692202700841
        if (Platform.os.isWindows && Platform.runtime.isNative) return

		val compressed = "ED C2 31 0D 00 00 00 02 A0 4A 06 B2 7F 0E 53 F8 31 68 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 9C 0D".unhexIgnoreSpaces
		val result = compressed.uncompress(Deflate)
		for (n in 0 until 1000) {
			compressed.uncompress(Deflate)
		}
		assertEquals(0x5000, result.size)
		assertEquals("\\0".repeat(0x2800), result.toString(UTF8))
	}
}
