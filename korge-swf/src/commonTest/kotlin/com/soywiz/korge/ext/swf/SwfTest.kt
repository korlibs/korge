package com.soywiz.korge.ext.swf

import com.soywiz.klock.microseconds
import com.soywiz.klock.milliseconds
import com.soywiz.korge.animate.AnLibrary
import com.soywiz.korge.animate.AnMovieClip
import com.soywiz.korge.animate.AnSymbolMovieClip
import com.soywiz.korge.animate.AnSymbolShape
import com.soywiz.korge.animate.serialization.readAni
import com.soywiz.korge.animate.serialization.writeTo
import com.soywiz.korge.tests.TestCoroutineDispatcher
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.ViewsLog
import com.soywiz.korge.view.allDescendantNames
import com.soywiz.korge.view.descendantsWithProp
import com.soywiz.korge.view.descendantsWithPropDouble
import com.soywiz.korge.view.dumpToString
import com.soywiz.korge.view.updateSingleView
import com.soywiz.korio.async.async
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.MemoryVfs
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.RectangleInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlin.coroutines.CoroutineContext
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SwfTest {
    open class EventLoopTest(val dispatcher: TestCoroutineDispatcher) : CoroutineScope {
        override val coroutineContext: CoroutineContext get() = dispatcher
        suspend fun step(ms: Int) {
            //dispatcher.frameTime
            dispatcher.step(ms.milliseconds)
        }
    }

    val dispatcher = TestCoroutineDispatcher()
    val eventLoopTest = EventLoopTest(dispatcher)
	val viewsLog = ViewsLog(eventLoopTest.dispatcher)
	val views = viewsLog.views

	suspend fun VfsFile.readSWFDeserializing(views: Views, config: SWFExportConfig? = null): AnLibrary {
		val mem = MemoryVfs()

		val ani = if (config != null) this.readSWF(views, config) else this.readSWF(views)
		ani.writeTo(mem["file.ani"], ani.swfExportConfig.toAnLibrarySerializerConfig(compression = 0.0))
		//println("ANI size:" + mem["file.ani"].size())
		return mem["file.ani"].readAni(AnLibrary.Context(views))
	}

	fun swfTest(callback: suspend EventLoopTest.() -> Unit) = suspendTest {
		viewsLog.init()
		callback(eventLoopTest)
	}

	@Test
	fun name3() = swfTest {
		val lib = resourcesVfs["simple.swf"].readSWFDeserializing(views)
		assertEquals("550x400", "${lib.width}x${lib.height}")
		val mc = lib.createMainTimeLine()

		println(lib.fps)
		println(lib.msPerFrame)
		for (n in 0 until 10) {
			println(mc.dumpToString())
            mc.update(41)
		}
	}

    @Test
    fun dog() = swfTest {
        val lib = resourcesVfs["dog.swf"].readSWFDeserializing(views)
        assertEquals("450x300", "${lib.width}x${lib.height}")
        val mc = lib.createMainTimeLine()

        println(lib.fps)
        println(lib.msPerFrame)
        for (n in 0 until 10) {
            println(mc.dumpToString())
            mc.update(41)
        }
    }

	@Test
	fun name5() = swfTest {
		//val lib = ResourcesVfs["test1.swf"].readSWF(views)
		//val lib = ResourcesVfs["test2.swf"].readSWF(views)
		val lib = resourcesVfs["test4.swf"].readSWFDeserializing(views, SWFExportConfig(debug = true))
		println(lib)
	}

	@Test
	fun name6() = swfTest {
		val lib = resourcesVfs["as3test.swf"].readSWFDeserializing(views, SWFExportConfig(debug = false))
		assertEquals(6, lib.symbolsById.size)
		println(lib.symbolsById)

		val s0 = lib.symbolsById[0] as AnSymbolMovieClip
		val s1 = lib.symbolsById[1] as AnSymbolShape
		val s2 = lib.symbolsById[2] as AnSymbolMovieClip
		val s3 = lib.symbolsById[3] as AnSymbolMovieClip
		val s4 = lib.symbolsById[4] as AnSymbolShape
		val s5 = lib.symbolsById[5] as AnSymbolMovieClip

		//assertEquals(2, s0.actions.size)
		//assertEquals("[(0, AnActions(actions=[AnFlowAction(gotoTime=41, stop=true)])), (41, AnActions(actions=[AnFlowAction(gotoTime=41, stop=true)]))]", s0.actions.entries.toString())
		//assertEquals(0, s2.actions.size)
		//assertEquals(1, s3.actions.size)
		//assertEquals(1, s5.actions.size)

		println(lib)
	}

	@Test
    @Ignore
	fun name7() = swfTest {
		val lib = resourcesVfs["soundtest.swf"].readSWFDeserializing(views, SWFExportConfig(debug = false))
		println(lib)
	}

	@Test
	fun name8() = swfTest {
		val lib = resourcesVfs["progressbar.swf"].readSWFDeserializing(views, SWFExportConfig(debug = false))
		val mc = lib.symbolsById[0] as AnSymbolMovieClip
		assertEquals("[default, frame0, progressbar]", mc.states.keys.toList().sorted().toString())
		val progressbarState = mc.states["progressbar"]!!
		assertEquals(0.microseconds, progressbarState.startTime)
		//assertEquals("default", progressbarState.state.name)
		//assertEquals(41000, progressbarState.state.loopStartTime)
		assertEquals(83000.microseconds, progressbarState.subTimeline.totalTime)

		println(lib)
	}

	@Test
	fun exports() = swfTest {
		val lib = resourcesVfs["exports.swf"].readSWFDeserializing(views, SWFExportConfig(debug = false))
		assertEquals(listOf("MainTimeLine", "Graphic1Export", "MC1Export"), lib.symbolsByName.keys.toList())
		val sh = lib.createMovieClip("Graphic1Export")
		val mc = lib.createMovieClip("MC1Export")

		//AnimateSerializer.gen(lib).writeToFile("c:/temp/file.ani")

		//AnimateDeserializer.read(AnimateSerializer.gen(lib), views)
	}

	@Test
	fun props() = swfTest {
		val lib = resourcesVfs["props.swf"].readSWFDeserializing(views, SWFExportConfig(debug = false))
		//val lib = ResourcesVfs["props.swf"].readSWF(views, debug = false)
		val mt = lib.createMainTimeLine()
		views.stage.addChild(mt)
		assertEquals(mapOf("gravity" to "9.8"), mt.children.first().props)
		assertEquals(1, views.stage.descendantsWithProp("gravity").count())
		assertEquals(1, views.stage.descendantsWithProp("gravity", "9.8").count())
		assertEquals(0, views.stage.descendantsWithProp("gravity", "9.0").count())

		assertEquals(1, views.stage.descendantsWithPropDouble("gravity").count())
		assertEquals(1, views.stage.descendantsWithPropDouble("gravity", 9.8).count())
		assertEquals(0, views.stage.descendantsWithPropDouble("gravity", 9.0).count())
	}

	@Test
	fun shapes() = swfTest {
		val lib = resourcesVfs["shapes.swf"].readSWFDeserializing(views, SWFExportConfig(debug = false))
		//val lib = ResourcesVfs["shapes.swf"].readSWF(views, debug = false)
		val mt = lib.createMainTimeLine()
		views.stage += mt
		val shape = mt.getChildByName("shape") as AnMovieClip
		assertNotNull(shape)

		val allItems = listOf("f12", "f23", "f34", "square", "circle")

		fun assertExists(vararg exists: String) {
			val exists2 = exists.toList()
			val notExists = allItems - exists2

			val availableNames = (shape as Container).children.map { it.name }.filterNotNull()

			for (v in exists) assertNotNull(shape.getChildByName(v), "Missing elements: $exists2 in $availableNames")
			for (v in notExists) assertNull(
				shape.getChildByName(v),
				"Elements that should not exists: $notExists in $availableNames"
			)
		}

		assertExists("f12")
		shape.play("circle")
		assertExists("f12", "f23", "circle")
		shape.play("square")
		assertExists("f23", "f34", "square")
		shape.play("empty2")
		assertExists("f34")
	}

	@Test
	fun morph() = swfTest {
		val lib = resourcesVfs["morph.swf"].readSWFDeserializing(views, SWFExportConfig(debug = false))
		//val lib = ResourcesVfs["shapes.swf"].readSWFDeserializing(views, debug = false)
		//lib.writeTo(LocalVfs("c:/temp")["morph.ani"])
	}

	@Test
	fun ninepatch() = swfTest {
		val lib = resourcesVfs["ninepatch.swf"].readSWFDeserializing(views, SWFExportConfig(debug = false))
		//val lib = ResourcesVfs["shapes.swf"].readSWFDeserializing(views, debug = false)
		//lib.writeTo(LocalVfs("c:/temp")["ninepatch.ani"])
	}

	@Test
	fun stopattheend() = swfTest {
		val lib = resourcesVfs["stop_at_the_end.swf"].readSWFDeserializing(views, SWFExportConfig(debug = false))
		val cmt = lib.createMainTimeLine()
		assertEquals(listOf("box"), cmt.allDescendantNames)
		for (n in 0 until 10) cmt.update(10)
		assertEquals(listOf("circle"), cmt.allDescendantNames)
		cmt["circle"]?.x = 900.0
		assertEquals(900.0, cmt["circle"]?.x)
		cmt.update(10)
		cmt.update(40)
		assertEquals(900.0, cmt["circle"]?.x)
		assertEquals(
			RectangleInt(x=899, y=96, width=161, height=161),
			cmt["circle"]!!.getGlobalBounds().toInt()
		)
		//val lib = ResourcesVfs["shapes.swf"].readSWFDeserializing(views, debug = false)
		//lib.writeTo(LocalVfs("c:/temp")["ninepatch.ani"])
	}

    @Test
	fun cameraBounds() = swfTest {
		val lib = resourcesVfs["cameras.swf"].readSWFDeserializing(views, SWFExportConfig(debug = false))
		val root = views.stage
		root += lib.createMainTimeLine()
		assertEquals(
			RectangleInt(-1, -2, 721, 1282),
			root["showCamera"]!!.getGlobalBounds().toInt()
		)
		assertEquals(
            RectangleInt(x=137, y=0, width=444, height=790),
			root["menuCamera"]!!.getGlobalBounds().toInt()
		)
		assertEquals(
            RectangleInt(x=-359, y=0, width=1439, height=2559),
			root["ingameCamera"]!!.getGlobalBounds().toInt()
		)

		//val lib = ResourcesVfs["shapes.swf"].readSWFDeserializing(views, debug = false)
		//lib.writeTo(LocalVfs("c:/temp")["ninepatch.ani"])
	}

	@Test
	//("Fix order")
	fun events() = swfTest {
		//val lib = ResourcesVfs["cameras.swf"].readSWFDeserializing(views, SWFExportConfig(debug = false))
		val lib = resourcesVfs["events.swf"].readSWFDeserializing(views, SWFExportConfig(debug = false))
		val root = views.stage
		val mtl = lib.createMainTimeLine()
		root += mtl
        val state = go {
			println("a")
			val result = mtl.playAndWaitEvent("box", "box_back")
			println("--------------")
			assertEquals("box_back", result)
			//assertEquals(0.5, mtl["box"]!!.alpha, 0.001)
			assertEquals(0.5, mtl["box"]!!.alpha, 0.01)
			println("b")
		}
		for (n in 0 until 200) {
			views.update(42)
			step(42)
		}
		state.await()
	}

	@Test
    @Ignore
	fun bigexternal1() = swfTest {
		val lib = localVfs("c:/temp/test29.swf").readSWFDeserializing(views, SWFExportConfig(debug = false))
		//val lib = ResourcesVfs["shapes.swf"].readSWFDeserializing(views, debug = false)
		lib.writeTo(localVfs("c:/temp")["test29.ani"])
	}

	@Test
    @Ignore
	fun bigexternal2() = swfTest {
		val lib = localVfs("c:/temp/ui.swf").readSWFDeserializing(views)
		val mc = lib.createMainTimeLine()

		println(lib.fps)
		println(lib.msPerFrame)
		for (n in 0 until 10) {
			println(mc.dumpToString())
			mc.update(41)
		}
	}

    fun Views.update(ms: Int) {
        this.update(ms.milliseconds)
    }
    fun View.update(ms: Int) {
        this.updateSingleView(ms.milliseconds)
    }

    operator fun View.get(name: String): View? {
        when {
            this.name == name -> return this
            this is Container -> {
                this.getChildByName(name)?.let { return it }
                this.fastForEachChild {
                    it[name]?.let { return it }
                }
            }
        }
        return null
    }
    suspend fun <T> CoroutineScope.go(block: suspend () -> T): Deferred<T> = async { block() }

    //suspend fun <T> Deferred<T>.await(): T = this.
}
