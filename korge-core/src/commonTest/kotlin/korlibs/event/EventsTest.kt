package korlibs.event

import kotlin.test.*

class EventsTest {
    @Test
    fun testMouseEvents() {
        MouseEvent(scrollDeltaX = 1f, scrollDeltaMode = MouseEvent.ScrollDeltaMode.LINE).also {
            assertEquals(10.0f, it.scrollDeltaXPixels)
            assertEquals(1.0f, it.scrollDeltaXLines)
            assertEquals(0.1f, it.scrollDeltaXPages)
        }
        MouseEvent(scrollDeltaX = 100f, scrollDeltaMode = MouseEvent.ScrollDeltaMode.PIXEL).also {
            assertEquals(100.0f, it.scrollDeltaXPixels)
            assertEquals(10.0f, it.scrollDeltaXLines)
            assertEquals(1.0f, it.scrollDeltaXPages)
        }
        MouseEvent(scrollDeltaX = 1f, scrollDeltaMode = MouseEvent.ScrollDeltaMode.PAGE).also {
            assertEquals(100.0f, it.scrollDeltaXPixels)
            assertEquals(10.0f, it.scrollDeltaXLines)
            assertEquals(1.0f, it.scrollDeltaXPages)
        }
    }
}
