package korlibs.image.format

import korlibs.io.async.*
import korlibs.io.file.std.*
import kotlin.test.*

class WEBPTest {
    val WEBPDecoder = ImageDecodingProps(format = WEBP)

    @Test
    fun testPremultiplied() = suspendTest {
        WEBP.initOnce(coroutineContext)
        val bmp = resourcesVfs["Exif5-2x.webp"].readBitmap(WEBPDecoder)
        assertEquals("256x256", "${bmp.size}")
    }
}
