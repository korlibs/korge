package com.soywiz.korim.text

import kotlin.test.*

class RichTextDataHTMLTest {
    @Test
    fun test() {
        assertEquals("hello <b>wo<i>r</i>ld</b>", RichTextData.fromHTML("hello <strong>wo<em>r</em>ld</strong>").toHTML())
        assertEquals("hello <font size=\"16\"><b>wo<i>r</i>ld</b></font>", RichTextData.fromHTML("hello <font size=20><strong>wo<em>r</em>ld</strong></font>").toHTML())
    }
}
