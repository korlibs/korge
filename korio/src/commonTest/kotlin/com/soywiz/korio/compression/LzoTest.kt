package com.soywiz.korio.compression

import com.soywiz.korio.compression.lzo.*
import com.soywiz.korio.lang.*
import com.soywiz.krypto.encoding.*
import kotlin.test.*

class LzoTest {
    val REF_TEXT = "HELLO THIS IS A HELLO THIS IS A HELLO WORLD HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO WORLD HELLO WORLD HELLO HELLO HELLO\n"

    @Test
    fun testDecompression() {
        val lzoCompressed = "894C5A4F000D0A1A0A104020A00940030903000001000081A4617DD526000000000968656C6C6F2E747874730E081300000086000000293966230D1C48454C4C4F2054484953204A004120343C0002574F524C44CC01200F14002AEC00351D010A11000000000000".unhex
        val uncompressed = lzoCompressed.uncompress(LZO)
        assertEquals(
            REF_TEXT,
            uncompressed.toString(UTF8)
        )
    }

    @Test
    fun testCompression() {
        val baseString = REF_TEXT
        val uncompressed = baseString
            .toByteArray(UTF8)
            .compress(LZO)
            .uncompress(LZO)
            .toString(UTF8)

        assertEquals(baseString, uncompressed)
    }
}
