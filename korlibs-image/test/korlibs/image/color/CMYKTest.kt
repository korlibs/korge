package korlibs.image.color

import kotlin.test.Test
import kotlin.test.assertEquals

class CMYKTest {
    data class ColorMap(val name: String, val cmyk: CMYK, val rgba: RGBA)

    val colors = listOf(
        ColorMap("BLACK", CMYK.float(0f, 0f, 0f, 1f), Colors.BLACK),
        ColorMap("WHITE", CMYK.float(0f, 0f, 0f, 0f), Colors.WHITE),
        ColorMap("RED", CMYK.float(0f, 1f, 1f, 0f), Colors.RED),
        ColorMap("GREEN", CMYK.float(1f, 0f, 1f, 0f), Colors.GREEN),
        ColorMap("BLUE", CMYK.float(1f, 1f, 0f, 0f), Colors.BLUE),
        ColorMap("YELLOW", CMYK.float(0f, 0f, 1f, 0f), Colors.YELLOW),
        ColorMap("CYAN", CMYK.float(1f, 0f, 0f, 0f), Colors.CYAN),
        ColorMap("MAGENTA", CMYK.float(0f, 1f, 0f, 0f), Colors.MAGENTA)
    )

    @Test
    fun testRgbaToCmyk() {
        for (color in colors) {
            assertEquals(color.cmyk, color.rgba.toCMYK(), color.name)
        }
    }

    @Test
    fun testCmykToRgba() {
        for (color in colors) {
            assertEquals(color.rgba, color.cmyk.toRGBA(), color.name)
        }
    }
}
