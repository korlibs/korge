package korlibs.image.font

import korlibs.io.async.*
import korlibs.io.file.std.*
import kotlin.test.*

class WoffTest {
    /*
    @Test
    @Ignore //     java.lang.ArrayIndexOutOfBoundsException: Index 1297 out of bounds for length 1296
    fun test() = suspendTest {
        val font = resourcesVfs["font.woff"].readWoffFont()
        assertEquals(
            listOf("OS/2", "PCLT", "cmap", "cvt ", "fpgm", "glyf", "hdmx", "head", "hhea", "hmtx", "kern", "loca", "maxp", "name", "post", "prep"),
            font.getTableNames().toList()
        )
        //NativeImage(500, 500).context2d { drawText("HELLO WORLD", 100.0, 100.0, font = font, size = 64.0, fill = true) }.showImageAndWait()
    }
    */
}
