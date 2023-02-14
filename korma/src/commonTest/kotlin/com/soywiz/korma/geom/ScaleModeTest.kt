package com.soywiz.korma.geom

import kotlin.test.Test
import kotlin.test.assertEquals

class ScaleModeTest {
    @Test
    fun size() {
        assertEquals(SizeInt(300, 300), SizeInt(100, 100).fitTo(SizeInt(600, 300)))
        assertEquals(SizeInt(300, 300), SizeInt(100, 100).applyScaleMode(SizeInt(600, 300), ScaleMode.SHOW_ALL))
        assertEquals(SizeInt(600, 600), SizeInt(100, 100).applyScaleMode(SizeInt(600, 300), ScaleMode.COVER))
        assertEquals(SizeInt(600, 300), SizeInt(100, 100).applyScaleMode(SizeInt(600, 300), ScaleMode.EXACT))
        assertEquals(SizeInt(100, 100), SizeInt(100, 100).applyScaleMode(SizeInt(600, 300), ScaleMode.NO_SCALE))
    }

    @Test
    fun rectangle() {
        assertEquals(MRectangle(150, 0, 300, 300), MRectangle(0, 0, 100, 100).applyScaleMode(MRectangle(0, 0, 600, 300), ScaleMode.SHOW_ALL, Anchor.MIDDLE_CENTER))
        assertEquals(MRectangle(0, 0, 300, 300), MRectangle(0, 0, 100, 100).applyScaleMode(MRectangle(0, 0, 600, 300), ScaleMode.SHOW_ALL, Anchor.TOP_LEFT))
        assertEquals(MRectangle(300, 0, 300, 300), MRectangle(0, 0, 100, 100).applyScaleMode(MRectangle(0, 0, 600, 300), ScaleMode.SHOW_ALL, Anchor.BOTTOM_RIGHT))

        assertEquals(MRectangle(0, 0, 600, 300), MRectangle(0, 0, 100, 100).applyScaleMode(MRectangle(0, 0, 600, 300), ScaleMode.EXACT, Anchor.MIDDLE_CENTER))

        assertEquals(MRectangle(0, 0, 100, 100), MRectangle(0, 0, 100, 100).applyScaleMode(MRectangle(0, 0, 600, 300), ScaleMode.NO_SCALE, Anchor.TOP_LEFT))
        assertEquals(MRectangle(250, 100, 100, 100), MRectangle(0, 0, 100, 100).applyScaleMode(MRectangle(0, 0, 600, 300), ScaleMode.NO_SCALE, Anchor.MIDDLE_CENTER))
        assertEquals(MRectangle(500, 200, 100, 100), MRectangle(0, 0, 100, 100).applyScaleMode(MRectangle(0, 0, 600, 300), ScaleMode.NO_SCALE, Anchor.BOTTOM_RIGHT))

        assertEquals(MRectangle(0, 0, 100, 100), MSize(100, 100).applyScaleMode(MRectangle(-100, -100, 200, 200), ScaleMode.NO_SCALE, Anchor.BOTTOM_RIGHT))

        assertEquals(MRectangle(0, 0, 600, 600), MSize(100, 100).applyScaleMode(MRectangle(0, 0, 600, 300), ScaleMode.COVER, Anchor.TOP_LEFT))
        assertEquals(MRectangle(0, -150, 600, 600), MSize(100, 100).applyScaleMode(MRectangle(0, 0, 600, 300), ScaleMode.COVER, Anchor.MIDDLE_CENTER))
        assertEquals(MRectangle(0, -300, 600, 600), MSize(100, 100).applyScaleMode(MRectangle(0, 0, 600, 300), ScaleMode.COVER, Anchor.BOTTOM_RIGHT))
    }
}
