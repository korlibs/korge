package com.soywiz.korim.format

import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.MSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JPEGInfoTest {
    @Test
    fun test() = suspendTest {
        val header = resourcesVfs["Portrait_3.jpg"].readImageInfo(JPEGInfo)
        assertNotNull(header)
        assertEquals(MSize(1800, 1200), header.size)
        assertEquals(ImageOrientation.ROTATE_180, header.orientation)
    }

    @Test
    fun test2() = suspendTest {
        val header = resourcesVfs["exif1.jpeg"].readImageInfo(JPEGInfo)
        assertNotNull(header)
        assertEquals(MSize(3024, 4032), header.size)
        assertEquals(ImageOrientation.ROTATE_180, header.orientation)
    }
}
