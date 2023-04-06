
import korlibs.image.color.*
import korlibs.korge.tests.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlin.test.*

class ViewsFixedSizeContainerHitTestTest : ViewsForTesting() {
    @Test
    fun testClipContainerHitTest() = viewsTest {
        lateinit var rect: SolidRect
        val clip = fixedSizeContainer(Size(100, 100), clip = true) {
            xy(50, 50)
            rect = solidRect(1000, 1000, Colors.RED)
            rect.mouseEnabled = true
        }

        val log = arrayListOf<Any?>()

        log += stage.mouseHitTest(Point(100, 200))
        log += stage.mouseHitTest(Point(100, 100))
        log += stage.mouseHitTest(Point(2000, 2000))
        clip.clip = false
        log += stage.mouseHitTest(Point(100, 200))
        assertEquals(
            listOf<Any?>(null, rect, null, rect),
            log
        )
    }
}
