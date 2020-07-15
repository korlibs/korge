package com.soywiz.korio.lang

import kotlin.test.*

class CharsetTest {
    @Test
    fun testSurrogatePairs() {
        val text = "{Test\uD83D\uDE00}"
        assertEquals(
            listOf(123, 84, 101, 115, 116, -16, -97, -104, -128, 125),
            text.toByteArray(UTF8).map { it.toInt() }
        )
    }

    @Test
    fun testSurrogatePairsTwo() {
        val text = "{Test\uD83D\uDE00}"
        assertEquals(
            text,
            text.toByteArray(UTF8).toString(UTF8).toByteArray(UTF8).toString(UTF8)
        )
    }

    @Test
    fun testDecode() {
        val text = byteArrayOf(-87, 32, 50, 48, 48, 57, 32, 45, 32, 50, 48, 49)
        assertEquals(
            "\uFFFD 2009 - 201",
            text.toString(UTF8)
        )
    }

    @Test
    fun testSample() {
        val text = (0 until 255).map { it.toChar() }.joinToString("")
        assertEquals(
            text,
            text.toByteArray(UTF8).toString(UTF8)
        )
    }
}
