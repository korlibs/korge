import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korge.view.SolidRect
import com.soywiz.korge.view.fixedSizeContainer
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.*
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
