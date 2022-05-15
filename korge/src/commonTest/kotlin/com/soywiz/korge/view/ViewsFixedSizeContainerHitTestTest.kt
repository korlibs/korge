import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korge.view.SolidRect
import com.soywiz.korge.view.fixedSizeContainer
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
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

        log += stage.mouseHitTest(100.0, 200.0)
        log += stage.mouseHitTest(100.0, 100.0)
        log += stage.mouseHitTest(2000.0, 2000.0)
        clip.clip = false
        log += stage.mouseHitTest(100.0, 200.0)
        assertEquals(
            listOf<Any?>(null, rect, null, rect),
            log
        )
    }
}
