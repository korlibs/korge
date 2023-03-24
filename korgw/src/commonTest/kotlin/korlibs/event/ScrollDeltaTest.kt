package korlibs.event

import kotlin.test.Test
import kotlin.test.assertEquals

class ScrollDeltaTest {
    @Test
    fun test() {
        MouseEvent().apply {
            scrollDeltaMode = MouseEvent.ScrollDeltaMode.LINE
            scrollDeltaY = 10.0
            assertEquals(100.0, scrollDeltaYPixels)
            assertEquals(10.0, scrollDeltaYLines)
            assertEquals(1.0, scrollDeltaYPages)
        }
        MouseEvent().apply {
            scrollDeltaMode = MouseEvent.ScrollDeltaMode.PIXEL
            scrollDeltaY = 100.0
            assertEquals(100.0, scrollDeltaYPixels)
            assertEquals(10.0, scrollDeltaYLines)
            assertEquals(1.0, scrollDeltaYPages)
        }
        MouseEvent().apply {
            scrollDeltaMode = MouseEvent.ScrollDeltaMode.PAGE
            scrollDeltaY = 1.0
            assertEquals(100.0, scrollDeltaYPixels)
            assertEquals(10.0, scrollDeltaYLines)
            assertEquals(1.0, scrollDeltaYPages)
        }
    }
}