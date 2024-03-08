package korlibs.image.format

import korlibs.image.bitmap.matchContentsDistinctCount
import korlibs.io.async.suspendTestNoBrowser
import korlibs.io.file.std.resourcesVfs
import kotlin.test.Test
import kotlin.test.assertEquals

class KRATest {
    val formats = ImageFormats(KRA, PNG)

    @Test
    fun kraTest() = suspendTestNoBrowser {
        val output = resourcesVfs["krita1.kra"].readBitmap(formats)
        val expected = resourcesVfs["krita1.kra.png"].readBitmapNoNative(formats)
        //output.showImageAndWait()
        assertEquals(0, output.matchContentsDistinctCount(expected))
    }
}
