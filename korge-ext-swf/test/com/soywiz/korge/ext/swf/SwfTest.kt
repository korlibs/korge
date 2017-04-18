package com.soywiz.korge.ext.swf

import com.soywiz.korge.animate.AnLibrary
import com.soywiz.korge.animate.AnSymbolMovieClip
import com.soywiz.korge.animate.AnSymbolShape
import com.soywiz.korge.animate.serialization.AnLibraryDeserializer
import com.soywiz.korge.animate.serialization.AnLibrarySerializer
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.ViewsLog
import com.soywiz.korge.view.dumpToString
import com.soywiz.korge.view.findDescendantsWithProp
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.LocalVfs
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korio.vfs.writeToFile
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SwfTest {
	val viewsLog = ViewsLog()
	val views = viewsLog.views

	suspend fun VfsFile.readSWFDeserializing(views: Views, debug: Boolean = false): AnLibrary = AnLibraryDeserializer.read(AnLibrarySerializer.gen(this.readSWF(views, debug = debug), compression = 0.0), views)

	@Before
	fun init() = syncTest {
		viewsLog.init()
	}

	@Test
	fun name3() = syncTest {
		val lib = ResourcesVfs["simple.swf"].readSWFDeserializing(views)
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
		val lib = ResourcesVfs["test4.swf"].readSWFDeserializing(views, debug = true)
		println(lib)
	}

	@Test
	fun name6() = syncTest {
		val lib = ResourcesVfs["as3test.swf"].readSWFDeserializing(views, debug = false)
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
		val lib = ResourcesVfs["soundtest.swf"].readSWFDeserializing(views, debug = false)
		println(lib)
	}

	@Test
	fun name8() = syncTest {
		val lib = ResourcesVfs["progressbar.swf"].readSWFDeserializing(views, debug = false)
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
		val lib = ResourcesVfs["exports.swf"].readSWFDeserializing(views, debug = false)
		Assert.assertEquals(listOf("MainTimeLine", "Graphic1Export", "MC1Export"), lib.symbolsByName.keys.toList())
		val sh = lib.createMovieClip("Graphic1Export")
		val mc = lib.createMovieClip("MC1Export")

		//AnimateSerializer.gen(lib).writeToFile("c:/temp/file.ani")

		//AnimateDeserializer.read(AnimateSerializer.gen(lib), views)
	}

	@Test
	fun props() = syncTest {
		val lib = ResourcesVfs["props.swf"].readSWFDeserializing(views, debug = false)
		//val lib = ResourcesVfs["props.swf"].readSWF(views, debug = false)
		val mt = lib.createMainTimeLine()
		views.stage += mt
		Assert.assertEquals(mapOf("gravity" to "9.8"), mt.children.first().props)
		Assert.assertEquals(1, views.stage.findDescendantsWithProp("gravity").count())
		Assert.assertEquals(1, views.stage.findDescendantsWithProp("gravity", "9.8").count())
		Assert.assertEquals(0, views.stage.findDescendantsWithProp("gravity", "9.0").count())
	}
}
