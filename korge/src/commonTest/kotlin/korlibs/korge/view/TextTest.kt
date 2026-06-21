package korlibs.korge.view

import assertEqualsFloat
import korlibs.image.font.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.math.geom.*
import kotlin.test.*

class TextTest {
    @Test
    fun testTextBounds() {
        val text = Text("", font = DefaultTtfFont)
        assertEqualsFloat(
            Rectangle.fromBounds(Point(0, 0), Point(0, 18.4)),
            text.getLocalBounds().roundDecimalPlaces(1)
        )
        text.text = "hello"
        assertEqualsFloat(
            Rectangle.fromBounds(Point(-0.8, 0), Point(28.9, 18.4)),
            text.getLocalBounds().roundDecimalPlaces(1)
        )
    }

    @Test
    fun testTextRange() = suspendTest {
        val text = Text("Hello", font = resourcesVfs["font/font.fnt"].readBitmapFont())
        text.textRangeStart = 1
        text.textRangeEnd = 3
        text._renderInternal(null)
        val sliceContainer = text.firstChild as Container
        assertEquals(5, sliceContainer.numChildren)
        assertEquals(".XX..", sliceContainer.children.joinToString("") { if (it.visible) "X" else "." })
    }
}
