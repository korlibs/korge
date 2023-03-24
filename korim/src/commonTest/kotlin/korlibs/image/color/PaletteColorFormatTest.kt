package korlibs.image.color

import kotlin.test.Test
import kotlin.test.assertEquals

class PaletteColorFormatTest {
    @Test
    fun test() {
        val fmt = PaletteColorFormat(RgbaArray(Colors.RED, Colors.GREEN, Colors.BLUE))
        assertEquals(Colors.RED, fmt.toRGBA(0))
        assertEquals(Colors.GREEN, fmt.toRGBA(1))
        assertEquals(Colors.BLUE, fmt.toRGBA(2))
    }
}
