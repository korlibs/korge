package korlibs.image.text

import kotlin.test.*

class RichTextDataHTMLTest {
    @Test
    fun test() {
        assertEquals("hello <b>wo<i>r</i>ld</b>", RichTextData.fromHTML("hello <strong>wo<em>r</em>ld</strong>").toHTML())
        assertEquals("hello <font size=20><b>wo<i>r</i>ld</b></font>", RichTextData.fromHTML("hello <font size=20><strong>wo<em>r</em>ld</strong></font>").toHTML())
        assertEquals("hello <font color=\"red\">world</font>", RichTextData.fromHTML("hello <font color=red>world</font>").toHTML())
    }
}
