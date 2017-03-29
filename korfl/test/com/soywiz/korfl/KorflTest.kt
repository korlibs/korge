package com.soywiz.korfl

import com.soywiz.korge.view.ViewsLog
import com.soywiz.korge.view.dumpToString
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Test
import java.util.*

class KorflTest {
	val viewsLog = ViewsLog()
	val views = viewsLog.views

	@Test
	fun name3() = syncTest {
		val lib = ResourcesVfs["simple.swf"].readSWF(views)
		val mc = lib.createMainTimeLine()
		println(lib.fps)
		println(lib.msPerFrame)
		for (n in 0 until 10) {
			println(mc.dumpToString())
			mc.update(41)
		}
	}

	@Test
	fun name5() = syncTest {
		//val lib = ResourcesVfs["test1.swf"].readSWF(views)
		//val lib = ResourcesVfs["test2.swf"].readSWF(views)
		val lib = ResourcesVfs["test4.swf"].readSWF(views, debug = true)
		println(lib)
	}
}
