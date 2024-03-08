package korlibs.io.stream

import korlibs.io.lang.*
import kotlin.test.*

class CharReaderTest {
    @Test
    fun test() {
        val reader = "áéíóúñ".toByteArray(UTF8).toCharReader(UTF8)
        assertEquals("á,éí,óúñ", listOf(reader.read(1), reader.read(2), reader.read(10)).joinToString(","))
    }


    @Test
    fun test2() {
        val reader = "áéíóúñ".repeat(10).toByteArray(UTF8).toCharReader(charset = UTF8, chunkSize = 8)
        assertEquals("á,éí,óúñ", listOf(reader.read(1), reader.read(2), reader.read(3)).joinToString(","))

        assertFailsWith<IllegalArgumentException> { "áéíóúñ".toByteArray(UTF8).toCharReader(charset = UTF8, chunkSize = CharReaderFromSyncStream.MIN_CHUNK_SIZE -1) }

    }

    @Test
    fun testCharReaderWithRandomStrings() {
        val randomStrings = listOf(
            "abcdefghijklm",
            "ä<a>ä</a>",
            "©頷ӨҤもタ編倏病Ҿ0沑âチ麕üӨҀ🙌とガӃ🙄Ҥzせø觧ҥ",
            "Ђヹ肯みцë匓ンê😺り磬バëӇØ゚琫ら儂脸😨D亢JZEÕキ燗😨🙉ュӼ`ぺ",
            "😳捇😗Ҧヿィ😲😵SӚåぐルҩS😯yѹ=ӪӠÀrキえÄ🙎へ¶Mたじ😞冃😃a\\xa0ÙҒ樗ボ😨",
            "らーçウごネ粦😓õ姗ӏ(",
            "Sm糛҆Òう楢ょ😽ê.",
            "X5O!P%@AP[4\\PZX54(P^)7CC)7}\$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!\$H+H*"
        )
        (1..30).forEach { readCount ->
            randomStrings.forEach { inputData ->
                val dataSegments: List<String> = inputData.splitInChunks(readCount)

                for (chunkSize in 8 until 2000) {
                    val charReader = inputData.openSync().toCharReader(charset = Charsets.UTF8, chunkSize = chunkSize)
                    dataSegments.forEach { data ->
                        val strBuilder = StringBuilder()
                        assertEquals(data.length, charReader.read(strBuilder, readCount))
                        assertEquals(data, strBuilder.toString())
                    }
                    assertEquals("", charReader.read(1)) // stream is empty all chars already read
                }
            }
        }
    }
}
