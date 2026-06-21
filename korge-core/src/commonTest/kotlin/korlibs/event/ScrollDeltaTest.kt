package korlibs.event

import kotlin.test.Test
import kotlin.test.assertEquals

class ScrollDeltaTest {
    @Test
    fun test() {
        MouseEvent().apply {
            scrollDeltaMode = MouseEvent.ScrollDeltaMode.LINE
            scrollDeltaY = 10f
            assertEquals(100f, scrollDeltaYPixels)
            assertEquals(10f, scrollDeltaYLines)
            assertEquals(1f, scrollDeltaYPages)
        }
        MouseEvent().apply {
            scrollDeltaMode = MouseEvent.ScrollDeltaMode.PIXEL
            scrollDeltaY = 100f
            assertEquals(100f, scrollDeltaYPixels)
            assertEquals(10f, scrollDeltaYLines)
            assertEquals(1f, scrollDeltaYPages)
        }
        MouseEvent().apply {
            scrollDeltaMode = MouseEvent.ScrollDeltaMode.PAGE
            scrollDeltaY = 1f
            assertEquals(100f, scrollDeltaYPixels)
            assertEquals(10f, scrollDeltaYLines)
            assertEquals(1f, scrollDeltaYPages)
        }
    }
}
