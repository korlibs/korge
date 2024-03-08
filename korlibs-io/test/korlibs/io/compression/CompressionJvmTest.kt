package korlibs.io.compression

import korlibs.io.compression.deflate.Deflate
import korlibs.io.compression.deflate.DeflatePortable
import korlibs.io.compression.deflate.GZIP
import korlibs.io.compression.deflate.GZIPNoCrc
import korlibs.io.compression.deflate.ZLib
import korlibs.io.lang.UTF8
import korlibs.io.lang.toByteArray
import korlibs.io.lang.toString
import korlibs.encoding.fromBase64
import korlibs.memory.*
import kotlin.test.Test
import kotlin.test.assertEquals

class CompressionJvmTest {
	val compressedData =
		"H4sIAAAAAAAAA+3SsREAEBSD4WcFm2ACTID9dxGFxgDcub/4mjQpEmdmDuYPKwsSJT3qz1KkXu7fWZMu4/IGr78AAAAAAD+a6ywcnAAQAAA=".fromBase64()
	val expectedData = "" +
			"1111111111111111111111111111111111111111111111111111111111111111111818181814950511111111111111111111111111818181816566671111111" +
			"1111111111111111118181811818283111111111111111111111111118181111111111111111111111111111111111111111111111111111111111111111111" +
			"1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
			"1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
			"1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
			"1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
			"1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
			"1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
			"111111111111111111111111111111"

    @Test
    fun gzip() {
        val data = compressedData
        val res = data.uncompress(GZIPNoCrc)
        val res2 = res.getS32ArrayLE(0, 4096 / 4)
        val actualData = res2.toList().joinToString("")
        if (expectedData != actualData) {
            println("EX: $expectedData")
            println("AC: $actualData")
        }
        assertEquals(expectedData, actualData)
    }

    @Test
    fun gzip2() {
        val data = compressedData
        val res = data.uncompress(GZIPNoCrc)
        val res2 = res.getS32ArrayLE(0, 4096 / 4)
        assertEquals(expectedData, res2.toList().joinToString(""))
    }

	@Test
	fun compressGzipNoCrcSync() = compressSync(GZIPNoCrc)

	@Test
	fun compressGzipSync() = compressSync(GZIP)

	@Test
	fun compressZlibSync() = compressSync(ZLib)

	@Test
	fun compressDeflatePortableSync() = compressSync(DeflatePortable)

	@Test
	fun compressDeflateSync() = compressSync(Deflate)

	fun compressSync(method: CompressionMethod) {
		val str = "HELLO HELLO HELLO!"
		val uncompressed = str.toByteArray(UTF8)
		val compressed = uncompressed.compress(method)
		val decompressed = compressed.uncompress(method)
		assertEquals(decompressed.toString(UTF8), str, "With $method")
	}
}
