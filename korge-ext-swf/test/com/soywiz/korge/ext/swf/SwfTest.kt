package com.soywiz.korge.ext.swf

import com.soywiz.korge.animate.AnSymbolMovieClip
import com.soywiz.korge.animate.AnSymbolShape
import com.soywiz.korge.animate.serialization.AnimateDeserializer
import com.soywiz.korge.animate.serialization.AnimateSerializer
import com.soywiz.korge.view.ViewsLog
import com.soywiz.korge.view.dumpToString
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.LocalVfs
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korio.vfs.writeToFile
import org.junit.Assert
import org.junit.Test

class SwfTest {
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

	@Test
	fun name6() = syncTest {
		val lib = ResourcesVfs["as3test.swf"].readSWF(views, debug = false)
		Assert.assertEquals(6, lib.symbolsById.size)
		println(lib.symbolsById)

		val s0 = lib.symbolsById[0] as AnSymbolMovieClip
		val s1 = lib.symbolsById[1] as AnSymbolShape
		val s2 = lib.symbolsById[2] as AnSymbolMovieClip
		val s3 = lib.symbolsById[3] as AnSymbolMovieClip
		val s4 = lib.symbolsById[4] as AnSymbolShape
		val s5 = lib.symbolsById[5] as AnSymbolMovieClip

		//Assert.assertEquals(2, s0.actions.size)
		//Assert.assertEquals("[(0, AnActions(actions=[AnFlowAction(gotoTime=41, stop=true)])), (41, AnActions(actions=[AnFlowAction(gotoTime=41, stop=true)]))]", s0.actions.entries.toString())
		//Assert.assertEquals(0, s2.actions.size)
		//Assert.assertEquals(1, s3.actions.size)
		//Assert.assertEquals(1, s5.actions.size)

		println(lib)
	}

	@Test
	fun name7() = syncTest {
		val lib = ResourcesVfs["soundtest.swf"].readSWF(views, debug = false)
		println(lib)
	}

	@Test
	fun name8() = syncTest {
		val lib = ResourcesVfs["progressbar.swf"].readSWF(views, debug = false)
		val mc = lib.symbolsById[0] as AnSymbolMovieClip
		Assert.assertEquals("[frame0, default, progressbar]", mc.states.keys.toList().toString())
		val progressbarState = mc.states["progressbar"]!!
		Assert.assertEquals(0, progressbarState.startTime)
		Assert.assertEquals("default", progressbarState.state.name)
		Assert.assertEquals(41000, progressbarState.state.loopStartTime)
		Assert.assertEquals(41000, progressbarState.state.totalTime)

		println(lib)
	}


	@Test
	fun exports() = syncTest {
		val lib = AnimateDeserializer.read(AnimateSerializer.gen(ResourcesVfs["exports.swf"].readSWF(views, debug = false), compression = 0.0), views)
		Assert.assertEquals(listOf("MainTimeLine", "Graphic1Export", "MC1Export"), lib.symbolsByName.keys.toList())
		val sh = lib.createMovieClip("Graphic1Export")
		val mc = lib.createMovieClip("MC1Export")

		//AnimateSerializer.gen(lib).writeToFile("c:/temp/file.ani")

		//AnimateDeserializer.read(AnimateSerializer.gen(lib), views)

	}
}
