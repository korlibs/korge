package korlibs.korge.input.component

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.korge.input.*
import korlibs.korge.tests.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlin.test.*

class MouseComponentTest : ViewsForTesting() {
	@Test
	fun name() = viewsTest {
		val log = arrayListOf<String>()
		val tex = Bitmap32(16, 16, premultiplied = true)
		val image = Image(tex)
		views.stage += image

		image.onOver { log += "onOver" }
		image.onOut { log += "onOut" }
		image.onClick { log += "onClick" }
		image.onDown { log += "onDown" }
		image.onUp { log += "onUp" }
		image.onUpOutside { log += "onUpOutside" }
		image.onMove { log += "onMove" }

		mouseMoveTo(8.0, 8.0)
		assertEquals("onOver", log.joinToString(","))
		mouseMoveTo(20.0, 20.0)
		assertEquals("onOver,onOut", log.joinToString(","))
		mouseMoveTo(8.0, 8.0)
		mouseDown()
		assertEquals("onOver,onOut,onOver,onDown", log.joinToString(","))
		mouseMoveTo(10.0, 10.0)
		assertEquals("onOver,onOut,onOver,onDown,onMove", log.joinToString(","))
		mouseMoveTo(50.0, 50.0)
		assertEquals("onOver,onOut,onOver,onDown,onMove,onOut", log.joinToString(","))
		mouseUp()
		assertEquals("onOver,onOut,onOver,onDown,onMove,onOut,onUpOutside", log.joinToString(","))
	}

    @Test
    fun test2() = viewsTest {
        val log = arrayListOf<String>()
        fixedSizeContainer(Size(100, 100)) {
            solidRect(50, 50, Colors.RED) {
                onClick { log += "rect" }
            }
            onClick { log += "container" }
        }
        mouseMoveAndClickTo(80, 80)
        assertEquals("container", log.joinToString(","))
        mouseMoveAndClickTo(45, 45)
        assertEquals("container,rect", log.joinToString(","))
    }
}
