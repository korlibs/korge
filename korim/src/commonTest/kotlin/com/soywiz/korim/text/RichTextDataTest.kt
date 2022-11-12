package com.soywiz.korim.text

import com.soywiz.korim.font.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class RichTextDataTest {
    //val data = RichTextData(RichTextData.Node("hello, world", 16.0, DefaultTtfFont))

    @Test
    fun testTokenize() {
        assertEquals(
            listOf("hello", ",", " ", "world"),
            RichTextData.tokenize("hello, world")
        )
        assertEquals(
            listOf("hello", ",", " ", "world"),
            RichTextData.divide("hello, world")
        )
        assertEquals(
            listOf("h", "e", "l", "l", "o"),
            RichTextData.divide("hello")
        )
    }

    @Test
    fun testLimit() {
        val data = RichTextData(RichTextData.Line(RichTextData.Node("hello, world", 16.0, DefaultTtfFont)))
        assertEquals("hello, world", data.text)
        assertEquals("hello, \nworld", data.wordWrap(Size(40.0, 32.0)).text)
    }

    @Test
    fun testConstructSingle() {
        val data2 = RichTextData("hello\nworld!", 16.0, DefaultTtfFont)
        assertEquals("hello\nworld!", data2.text)
    }

    @Test
    fun testLimitEx() {
        val data = RichTextData("hello\nworld!", 16.0, DefaultTtfFont)
        assertEquals("hello\nworld!", data.text)
        assertEquals("hello\nworl\nd!", data.wordWrap(Size(32.0, 32.0)).text)
    }

    @Test
    fun testCombine() {
        val data = RichTextData("hello\nworld!", 16.0, DefaultTtfFont) + RichTextData("demo\ntest", 16.0, DefaultTtfFont)
        assertEquals("hello\nworld!demo\ntest", data.text)
    }
}
