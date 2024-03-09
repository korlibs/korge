package korlibs.image.bitmap

import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.platform.*
import kotlin.test.*

class Bitmap32RgbaTest {
    @Test
    fun testNative() = suspendTestNoBrowser {
        if (Platform.isNative) return@suspendTestNoBrowser
        //if (OS.isMac) return@suspendTestNoBrowser // kotlin.AssertionError: Expected <#ff0000ff>, actual <#fb0007ff>.
        //if (OS.isTvos) return@suspendTestNoBrowser

        val bmp = resourcesVfs["rgba.png"].readBitmap(ImageDecodingProps.DEFAULT_PREMULT.copy(format = PNG))
        val i = bmp.toBMP32()
        assertEquals(Colors.RED, i[0, 0])
        assertEquals(Colors.GREEN, i[1, 0])
        assertEquals(Colors.BLUE, i[2, 0])
        assertEquals(Colors.TRANSPARENT, i[3, 0])
    }

    @Test
    fun testNormal() = suspendTestNoBrowser {
        val bmp = resourcesVfs["rgba.png"].readBitmapNoNative(ImageDecodingProps.DEFAULT.copy(format = PNG))
        val i = bmp.toBMP32()
        assertEquals(Colors.RED, i[0, 0])
        assertEquals(Colors.GREEN, i[1, 0])
        assertEquals(Colors.BLUE, i[2, 0])
        assertEquals(Colors.TRANSPARENT, i[3, 0])
    }
}
