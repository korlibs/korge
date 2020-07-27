import com.soywiz.klock.*
import com.soywiz.korge.input.*
import com.soywiz.korge.tests.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class MyTest : ViewsForTesting() {
	@Test
	fun test() = viewsTest {
		val log = arrayListOf<String>()
		val rect = solidRect(100, 100, Colors.RED)
		rect.onClick {
			log += "clicked"
		}
		assertEquals(1, views.stage.numChildren)
		rect.simulateClick()
		assertEquals(true, rect.isVisibleToUser())
		tween(rect::x[-102], time = 10.seconds)
		assertEquals(Rectangle(x=-102, y=0, width=100, height=100), rect.globalBounds)
		assertEquals(false, rect.isVisibleToUser())
		assertEquals(listOf("clicked"), log)
	}
}