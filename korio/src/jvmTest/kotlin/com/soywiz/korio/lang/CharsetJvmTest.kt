package com.soywiz.korio.lang

import kotlin.test.Test
import kotlin.test.assertEquals

class CharsetJvmTest {
    @Test
    fun testSurrogatePairs() {
        val text = "{Test\uD83D\uDE00}"
        assertEquals(
            text.toByteArray(UTF8).toList(),
            text.toByteArray(kotlin.text.Charsets.UTF_8).toList()
        )
    }

    @Test
    fun testSurrogatePairsTwo() {
        val text = "{Test\uD83D\uDE00}"
        //val text = "\uD83D\uDE00"
        assertEquals(
            text,
            text.toByteArray(UTF8).toString(UTF8).toByteArray(UTF8).toString(UTF8)
        )
    }
}
