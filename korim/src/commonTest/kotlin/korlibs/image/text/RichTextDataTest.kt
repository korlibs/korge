package korlibs.image.text

import korlibs.image.font.*
import kotlin.test.*

class RichTextDataTest {
    //val data = RichTextData(RichTextData.Node("hello, world", DefaultTtfFont, 16.0))
    val style = RichTextData.Style(DefaultTtfFont, 16f)

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
        val data = RichTextData(RichTextData.Line(RichTextData.TextNode("hello, world")))
        assertEquals("hello, world", data.text)
        assertEquals("hello, \nworld", data.wordWrap(40f).text)
    }

    @Test
    fun testConstructSingle() {
        val data2 = RichTextData("hello\nworld!", DefaultTtfFont, 16f)
        assertEquals("hello\nworld!", data2.text)
    }

    @Test
    fun testLimitEx() {
        val data = RichTextData("hello\nworld!", DefaultTtfFont, 16f)
        assertEquals("hello\nworld!", data.text)
        assertEquals("hello\nworl\nd!", data.wordWrap(32f).text)
    }

    @Test
    fun testCombine() {
        val data = RichTextData("hello\nworld!", DefaultTtfFont, 16f) + RichTextData("demo\ntest", DefaultTtfFont, 16f)
        assertEquals("hello\nworld!demo\ntest", data.text)
    }

    @Test
    fun testLimitHeight() {
        val data = RichTextData("hello\nworld!", DefaultTtfFont, 16f)
        assertEquals("hello", data.limitHeight(16f).text)
        assertEquals("hello\nworld!", data.limitHeight(24f, includePartialLines = true).text)
        assertEquals("hello", data.limitHeight(24f, includePartialLines = false).text)
    }

    @Test
    fun testLimitAll() {
        val data = RichTextData("hello\nworld!", DefaultTtfFont, 16f)
        assertEquals("hello...", data.limit(64f, 16f, ellipsis = "...").text)
        assertEquals("hell...", data.limit(32f, 16f, ellipsis = "...").text)
        assertEquals("h...", data.limit(24f, 16f, ellipsis = "...").text)
        assertEquals("...", data.limit(16f, 16f, ellipsis = "...").text)
        assertEquals("hello\nworld!", data.limit(64f, 32f, ellipsis = "...").text)
        assertEquals("hello\nwo...", data.limit(32f, 32f, ellipsis = "...").text)
        assertEquals("hello\nworl\nd!", data.limit(32f, 64f, ellipsis = "...").text)
    }

    @Test
    fun testFitEllipsis() {
        val line = RichTextData.Line(RichTextData.TextNode("hello", style))
        assertEquals("hello...", RichTextData.fitEllipsis(40f, line).text)
        assertEquals("hell...", RichTextData.fitEllipsis(32f, line).text)
        assertEquals("hel...", RichTextData.fitEllipsis(30f, line).text)
        assertEquals("he...", RichTextData.fitEllipsis(28f, line).text)
        assertEquals("h...", RichTextData.fitEllipsis(24f, line).text)
        assertEquals("...", RichTextData.fitEllipsis(16f, line).text)
    }
}
