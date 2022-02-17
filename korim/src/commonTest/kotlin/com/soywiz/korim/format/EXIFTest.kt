package com.soywiz.korim.format

import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.test.Test
import kotlin.test.assertEquals

class EXIFTest {
    @Test
    fun test() = suspendTest {
        val exif = EXIF.readExifFromJpeg(resourcesVfs["Portrait_3.jpg"])
        assertEquals(ImageOrientation.MIRROR_VERTICAL, exif.orientation)
    }
}
