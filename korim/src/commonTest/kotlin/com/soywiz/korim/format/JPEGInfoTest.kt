package com.soywiz.korim.format

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class JPEGInfoTest {
    @Test
    fun test() = suspendTest {
        val header = resourcesVfs["Portrait_3.jpg"].readImageInfo(JPEGInfo)
        assertNotNull(header)
        assertEquals(1800, header.width)
        assertEquals(1200, header.height)
        assertEquals(ImageOrientation.ROTATE_180, header.orientation)
    }
}
