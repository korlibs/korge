package com.soywiz.korim.format

import kotlin.test.*

class ImageOrientationTest {
    @Test
    fun test() {
        assertEquals(listOf(0, 1, 2, 3), ImageOrientation.ORIGINAL.indices.toList())
        assertEquals(listOf(3, 0, 1, 2), ImageOrientation.ROTATE_90.indices.toList())
        assertEquals(listOf(2, 3, 0, 1), ImageOrientation.ROTATE_180.indices.toList())
        assertEquals(listOf(1, 2, 3, 0), ImageOrientation.ROTATE_270.indices.toList())
    }
}
