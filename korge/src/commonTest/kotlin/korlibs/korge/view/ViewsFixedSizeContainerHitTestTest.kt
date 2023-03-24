import korlibs.korge.tests.ViewsForTesting
import korlibs.korge.view.SolidRect
import korlibs.korge.view.fixedSizeContainer
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.image.color.Colors
import korlibs.math.geom.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ViewsFixedSizeContainerHitTestTest : ViewsForTesting() {
    @Test
    fun testClipContainerHitTest() = viewsTest {
        lateinit var rect: SolidRect
        val clip = fixedSizeContainer(100, 100, clip = true) {
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
