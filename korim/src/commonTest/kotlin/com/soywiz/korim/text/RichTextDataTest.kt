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
        val data = RichTextData(RichTextData.Line(RichTextData.TextNode("hello, world", 16.0, DefaultTtfFont)))
        assertEquals("hello, world", data.text)
        assertEquals("hello, \nworld", data.wordWrap(40.0).text)
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
        assertEquals("hello\nworl\nd!", data.wordWrap(32.0).text)
    }

    @Test
    fun testCombine() {
        val data = RichTextData("hello\nworld!", 16.0, DefaultTtfFont) + RichTextData("demo\ntest", 16.0, DefaultTtfFont)
        assertEquals("hello\nworld!demo\ntest", data.text)
    }

    @Test
    fun testLimitHeight() {
        val data = RichTextData("hello\nworld!", 16.0, DefaultTtfFont)
        assertEquals("hello", data.limitHeight(16.0,).text)
        assertEquals("hello\nworld!", data.limitHeight(24.0, includePartialLines = true).text)
        assertEquals("hello", data.limitHeight(24.0, includePartialLines = false).text)
    }

    @Test
    fun testLimitAll() {
        val data = RichTextData("hello\nworld!", 16.0, DefaultTtfFont)
        assertEquals("hello...", data.limit(64.0, 16.0, ellipsis = "...").text)
        assertEquals("hell...", data.limit(32.0, 16.0, ellipsis = "...").text)
        assertEquals("h...", data.limit(24.0, 16.0, ellipsis = "...").text)
        assertEquals("...", data.limit(16.0, 16.0, ellipsis = "...").text)
        assertEquals("hello\nworld!", data.limit(64.0, 32.0, ellipsis = "...").text)
        assertEquals("hello\nwo...", data.limit(32.0, 32.0, ellipsis = "...").text)
        assertEquals("hello\nworl\nd!", data.limit(32.0, 64.0, ellipsis = "...").text)
    }

    @Test
    fun testFitEllipsis() {
        val line = RichTextData.Line(RichTextData.TextNode("hello", 16.0, DefaultTtfFont))
        assertEquals("hello...", RichTextData.fitEllipsis(40.0, line).text)
        assertEquals("hell...", RichTextData.fitEllipsis(32.0, line).text)
        assertEquals("hel...", RichTextData.fitEllipsis(30.0, line).text)
        assertEquals("he...", RichTextData.fitEllipsis(28.0, line).text)
        assertEquals("h...", RichTextData.fitEllipsis(24.0, line).text)
        assertEquals("...", RichTextData.fitEllipsis(16.0, line).text)
    }
}
