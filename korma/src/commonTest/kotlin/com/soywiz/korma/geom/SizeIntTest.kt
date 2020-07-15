package com.soywiz.korma.geom

import kotlin.test.Test
import kotlin.test.assertEquals

class SizeIntTest {
    @Test
    fun cover() {
        assertEquals(
            SizeInt(100, 400),
            SizeInt(50, 200).applyScaleMode(container = SizeInt(100, 100), mode = ScaleMode.COVER)
        )
        assertEquals(
            SizeInt(25, 100),
            SizeInt(50, 200).applyScaleMode(container = SizeInt(25, 25), mode = ScaleMode.COVER)
        )
    }
}
