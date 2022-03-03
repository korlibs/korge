package com.soywiz.korim.format

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

// https://zpl.fi/exif-orientation-in-different-formats/
class EXIFInfoTest {
    //val formats = ImageFormats(AVIFInfo, JPEGInfo, PNG, WEBPInfo)
    val formats = ImageFormats(AVIFInfo, JPEGInfo, PNG)

    suspend fun testBase(file: String) {
        val header = resourcesVfs[file].readImageInfo(formats)
        assertNotNull(header)
        assertEquals(256, header.width)
        assertEquals(256, header.height)
        assertEquals(ImageOrientation.MIRROR_HORIZONTAL_ROTATE_270, header.orientation)
    }

    @Test
    fun testAvif() = suspendTest { testBase("Exif5-2x.avif") }
    @Test
    fun testJpeg() = suspendTest { testBase("Exif5-2x.jpeg") }
    @Test
    fun testPNG() = suspendTest { testBase("Exif5-2x.png") }
    //@Test
    //fun testWebp() = suspendTest { testBase("Exif5-2x.webp") }
}
