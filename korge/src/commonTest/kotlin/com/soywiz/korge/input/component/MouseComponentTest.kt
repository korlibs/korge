package com.soywiz.korge.input.component

import com.soywiz.korge.input.*
import com.soywiz.korge.tests.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.util.*
import kotlin.test.*

class MouseComponentTest : ViewsForTesting() {
	@Test
	@Ignore // @TODO: Re-enable this
	fun name() = viewsTest {
		if (OS.isJs) return@viewsTest

		val log = arrayListOf<String>()
		val tex = Bitmap32(16, 16)
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
}
