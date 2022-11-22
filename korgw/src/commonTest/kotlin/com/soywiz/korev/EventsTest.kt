package com.soywiz.korev

import kotlin.test.*

class EventsTest {
    @Test
    fun testMouseEvents() {
        MouseEvent(scrollDeltaX = 1.0, scrollDeltaMode = MouseEvent.ScrollDeltaMode.LINE).also {
            assertEquals(10.0, it.scrollDeltaXPixels)
            assertEquals(1.0, it.scrollDeltaXLines)
            assertEquals(0.1, it.scrollDeltaXPages)
        }
        MouseEvent(scrollDeltaX = 100.0, scrollDeltaMode = MouseEvent.ScrollDeltaMode.PIXEL).also {
            assertEquals(100.0, it.scrollDeltaXPixels)
            assertEquals(10.0, it.scrollDeltaXLines)
            assertEquals(1.0, it.scrollDeltaXPages)
        }
        MouseEvent(scrollDeltaX = 1.0, scrollDeltaMode = MouseEvent.ScrollDeltaMode.PAGE).also {
            assertEquals(100.0, it.scrollDeltaXPixels)
            assertEquals(10.0, it.scrollDeltaXLines)
            assertEquals(1.0, it.scrollDeltaXPages)
        }
    }
}