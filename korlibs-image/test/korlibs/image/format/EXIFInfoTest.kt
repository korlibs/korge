package korlibs.image.format

import korlibs.memory.*
import korlibs.io.async.suspendTest
import korlibs.io.file.std.resourcesVfs
import korlibs.platform.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

// https://zpl.fi/exif-orientation-in-different-formats/
class EXIFInfoTest {
    //val formats = ImageFormats(AVIFInfo, JPEGInfo, PNG, WEBPInfo)
    val formats = ImageDecodingProps.DEFAULT.copy(format = ImageFormats(AVIFInfo, JPEGInfo, PNG))

    suspend fun testBase(file: String) {
        val header = resourcesVfs[file].readImageInfo(formats)
        assertNotNull(header)
        assertEquals(256, header.width)
        assertEquals(256, header.height)
        assertEquals(ImageOrientation.MIRROR_HORIZONTAL_ROTATE_270, header.orientation)
    }

    @Test
    fun testAvif() = suspendTest({ !Platform.isJsBrowser }) { testBase("Exif5-2x.avif") }
    @Test
    fun testJpeg() = suspendTest({ !Platform.isJsBrowser }) { testBase("Exif5-2x.jpeg") }
    @Test
    fun testPNG() = suspendTest({ !Platform.isJsBrowser }) { testBase("Exif5-2x.png") }
    //@Test
    //fun testWebp() = suspendTest { testBase("Exif5-2x.webp") }
}
