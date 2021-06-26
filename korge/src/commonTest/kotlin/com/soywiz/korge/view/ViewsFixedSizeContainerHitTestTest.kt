import com.soywiz.korag.log.*
import com.soywiz.korge.render.*
import com.soywiz.korge.tests.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class ViewsFixedSizeContainerHitTestTest : ViewsForTesting() {
    @Test
    fun testClipContainerHitTest() = viewsTest {
        lateinit var rect: SolidRect
        val clip = fixedSizeContainer(100, 100, clip = true) {
            xy(50, 50)
            rect = solidRect(1000, 1000, Colors.RED)
            rect.mouseEnabled = true
        }

        assertEquals(rect, stage.mouseHitTest(100.0, 100.0))
        assertEquals(null, stage.mouseHitTest(100.0, 200.0))
        clip.clip = false
        assertEquals(rect, stage.mouseHitTest(100.0, 200.0))
    }
}
