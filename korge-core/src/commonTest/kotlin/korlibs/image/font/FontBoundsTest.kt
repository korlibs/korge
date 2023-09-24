package korlibs.image.font

import korlibs.image.text.*
import korlibs.logger.*
import kotlin.test.*

class FontBoundsTest {
    val logger = Logger("FontBoundsTest")

    @Test
    fun test() {
        val bounds = DefaultTtfFont.getTextBounds(100.0, "Hello\nhi!", align = TextAlignment.TOP_LEFT)
        logger.debug { bounds }
        logger.debug { bounds.lineBounds }
        val bounds2 = DefaultTtfFont.getTextBoundsWithGlyphs(100.0, "Hello\nhi!", align = TextAlignment.TOP_LEFT)
        logger.debug { bounds2.metrics }
        //println(bounds2.glyphs)
        //buildVectorPath {
        //    circle()
        //}.transformPoints()
    }
}
