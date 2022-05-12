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
