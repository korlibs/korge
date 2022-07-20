package com.soywiz.korge.ext.swf

import com.soywiz.klock.microseconds
import com.soywiz.klock.milliseconds
import com.soywiz.korfl.as3swf.SWF
import com.soywiz.korfl.as3swf.TagDefineShape
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
import com.soywiz.korim.vector.ShapeRasterizerMethod
import com.soywiz.korim.vector.toSvg
import com.soywiz.korio.async.async
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.MemoryVfs
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.util.OS
import com.soywiz.korma.geom.RectangleInt
import com.soywiz.korma.geom.vector.VectorPath
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

    fun fastSwfExportConfig() = SWFExportConfig(
        debug = false, mipmaps = false,
        rasterizerMethod = ShapeRasterizerMethod.NONE,
        generateTextures = true
    )

    suspend fun VfsFile.readSWFDeserializing(views: Views, config: SWFExportConfig? = null): AnLibrary {
		val mem = MemoryVfs()

		val ani = this.readSWF(views, config ?: fastSwfExportConfig())
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
    fun dogShapes() = suspendTest {
        val swf = SWF()
        swf.loadBytes(resourcesVfs["dog.swf"].readBytes())
        val out = arrayListOf<String>()
        //println(swf.rootTimelineContainer.dictionary)
        for (charId in swf.getCharacterIds()) {
            val definition = swf.getCharacter(charId)
            when (definition) {
                is TagDefineShape -> {
                    val defStr = definition.getShapeExporter(swf, SWFExportConfig(roundDecimalPlaces = 2), 1.0).actualShape.toSvg(roundDecimalPlaces = 2)
                    out += "$charId:$defStr"
                }
            }
        }
        assertEquals(
            """
                1:<svg width="11.52px" height="20.42px" viewBox="0 0 11.52 20.42" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate(0.02, 0.02)"><path d="M8.2 11Q8.7 16.1, 8.1 16.8Q7.4 17.5, 4.2 15.2Q7 3.6, 7.5 6.6L8.2 11" transform="translate()" fill="rgba(102,33,0,1)"/><path d="M5.9 7.8Q6.6 5.6, 7.8 4L7.2 6.8Q6.3 10.6, 8.8 17.6Q10.2 19, 8.2 18.4L5.1 18.2Q4.2 18.7, 2.8 17L5 10.6L5.9 7.8" transform="translate()" fill="rgba(167,52,3,1)"/><path d="M6.6 0.1Q7.6 -0.6, 8.9 2.4Q10.2 5.4, 11 12L11.5 19.2L10.1 20.3Q8.9 20.8, 2.6 18.6L0.9 18Q-0.8 17.6, 0.6 15Q1.8 12.4, 3.6 6.6Q5.5 0.7, 6.6 0.1M8.2 11L7.5 6.6Q7 3.6, 4.2 15.2Q7.4 17.5, 8.1 16.8Q8.7 16.1, 8.2 11" transform="translate()" fill="rgba(34,10,0,1)"/></g></svg>
                2:<svg width="77.73px" height="63.7px" viewBox="0 0 77.73 63.7" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate(-0.02, -0.1)"><path d="M31.4 22L31.4 21.9L31.4 22" transform="translate()" fill="rgba(0,0,0,1)"/><path d="M31.4 22L31.4 22L30.8 23.6L30.2 25.8Q29.2 28.4, 28.8 29.1Q26.2 32.8, 22.6 35.4Q19.6 37.6, 16.8 38.2L12.2 39.3L9.6 39.3L6.8 39Q4.4 38.6, 3.6 37Q2.9 35.5, 2.8 33.5Q2.7 31.5, 4.1 26.2Q5.5 21, 8.7 15.4Q11.8 9.7, 15.4 8.6Q19 7.4, 27.6 12.4L29.9 13.4L30.8 13.6L34.8 14Q37.4 14, 39.7 12.8Q41.9 11.6, 44.2 9.6Q46.4 7.5, 49.6 5.6Q52.8 3.6, 57 3Q61.2 2.4, 65.4 4.2Q69.6 6, 72.3 11.4Q75 16.8, 75.6 23.6Q76.1 30.4, 76.1 39.6Q76.1 43.8, 74.2 46.6Q68.1 53, 53.8 57Q39.4 61, 29.6 61.2L18.4 61.5Q17.2 61.5, 16.6 51.4Q16.1 41.4, 17 40.7Q18 40, 18.6 39.8Q21.3 38.8, 24.5 36.6Q27.6 34.4, 30.1 30.4L30.6 32.6L30.6 32.4L30.8 27.8Q30.8 26.4, 31.2 24.4L31.4 22L31.4 21.9L31.4 22M30.6 32.6L30.5 32.6L30.6 32.6L30.6 32.6" transform="translate()" fill="rgba(153,51,0,1)"/><path d="M30.6 32.6L30.6 32.6L30.5 32.6L30.6 32.6" transform="translate()" fill="rgba(34,10,0,1)"/><path d="M69.5 7Q60.2 -0.6, 49.2 9.2Q38.2 19, 31 15.9Q23.6 12.8, 19 13.1Q14.4 13.4, 12.2 15.6Q10 17.6, 7 23.6Q4 29.5, 5.8 34.6Q12.6 42.2, 28.6 29.9Q21.4 41, 8.8 41Q3 40, 1 34.4Q1 28.1, 5 19.2Q9.2 10.2, 18 7.4Q20.6 7.6, 27.2 10.7Q29.8 12, 33.6 12.7Q37.3 13.4, 49.2 5Q61.2 -3.3, 72.8 8Q76.4 12.6, 77.1 17Q75.8 12, 69.5 7" transform="translate()" fill="rgba(202,62,2,1)"/><path d="M19.4 54Q18.6 61.4, 30.8 61.5Q28.1 61.8, 28.5 62.2L28.8 62.4L21.8 63.1Q15.6 63.6, 15.6 57L15.2 45.6Q14.8 40.7, 19.5 38.9Q24.2 37, 28.8 30.4Q26.4 36.8, 23.3 41.8Q20.2 46.7, 19.4 54" transform="translate()" fill="rgba(202,62,2,1)"/><path d="M77.7 26.2L77.8 44L77.6 48.2Q75.8 50.8, 61.6 56.8Q47.6 62.7, 31.5 63.4Q15.4 64.2, 15 63.4Q14.4 62.8, 14.3 58L13.8 41.8L12.8 42L5.5 41.2Q0.3 40.3, 0 36.4Q-0.2 32.4, 2 25.1Q4.1 17.7, 7.4 12.7Q10.8 7.6, 16.4 5.7Q19 6, 20.4 6.8Q21.6 7.6, 28.4 10.5Q35 13.4, 39.6 10.2Q44.3 7, 47 4.4Q49.6 1.8, 54.8 0.8L62.6 0.1Q65.2 0.4, 69 2.1Q72.6 3.7, 75.1 9.6Q77.6 15.4, 77.7 26.2M76.1 39.6Q76.1 30.4, 75.6 23.6Q75 16.8, 72.3 11.4Q69.6 6, 65.4 4.2Q61.2 2.4, 57 3Q52.8 3.6, 49.6 5.6Q46.4 7.5, 44.2 9.6Q41.9 11.6, 39.7 12.8Q37.4 14, 34.8 14L30.8 13.6L29.9 13.4L27.6 12.4Q19 7.4, 15.4 8.6Q11.8 9.7, 8.7 15.4Q5.5 21, 4.1 26.2Q2.7 31.5, 2.8 33.5Q2.9 35.5, 3.6 37Q4.4 38.6, 6.8 39L9.6 39.3L12.2 39.3L16.8 38.2Q19.6 37.6, 22.6 35.4Q26.2 32.8, 28.8 29.1Q29.2 28.4, 30.2 25.8L30.8 23.6L31.4 22L31.4 22L31.2 24.4Q30.8 26.4, 30.8 27.8L30.6 32.4L30.6 32.6L30.1 30.4Q27.6 34.4, 24.5 36.6Q21.3 38.8, 18.6 39.8Q18 40, 17 40.7Q16.1 41.4, 16.6 51.4Q17.2 61.5, 18.4 61.5L29.6 61.2Q39.4 61, 53.8 57Q68.1 53, 74.2 46.6Q76.1 43.8, 76.1 39.6" transform="translate()" fill="rgba(34,10,0,1)"/></g></svg>
                3:<svg width="13.47px" height="17.14px" viewBox="0 0 13.47 17.14" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate(-0.01, 0)"><path d="M6 3Q7.6 3.2, 9.2 4.3Q10.7 5.4, 10.2 6L8.6 8.2Q7.4 9.8, 5.6 11.3L3.3 13Q2.8 13.3, 2.6 12.8Q2.4 12.4, 2.6 10.9L3 7.6L3.4 4.6Q3.6 3.6, 3.9 3.3Q4.2 3, 4.4 3L6 3" transform="translate()" fill="rgba(26,26,26,1)"/><path d="M5.4 1.7L7.2 1.6Q9.1 1.6, 9.8 4.8Q7.5 1, 8.5 3.6Q4.6 4, 3.4 6.2Q2 8.4, 2.2 7.8L2.2 5Q2.1 2.8, 3.6 2.6Q5.2 2.3, 5.2 2L5.4 1.7" transform="translate()" fill="rgba(204,204,204,1)"/><path d="M13.3 5.2L10.4 9Q8.2 11.8, 5.2 14.4L3.3 15.8Q1.5 17.4, 0.6 17.1Q-0.2 16.3, 0 13.8Q0.2 11.2, 0.8 8L1.4 2.9Q1.7 1, 2.3 0.6L3 0L6 0.2Q8.6 0.4, 11.4 2.3Q14.1 4.2, 13.3 5.2M10.2 6Q10.7 5.4, 9.2 4.3Q7.6 3.2, 6 3L4.4 3Q4.2 3, 3.9 3.3Q3.6 3.6, 3.4 4.6L3 7.6L2.6 10.9Q2.4 12.4, 2.6 12.8Q2.8 13.3, 3.3 13L5.6 11.3Q7.4 9.8, 8.6 8.2L10.2 6" transform="translate()" fill="rgba(0,0,0,1)"/></g></svg>
                4:<svg width="24.87px" height="35.48px" viewBox="0 0 24.87 35.48" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate(0, -0.02)"><path d="M1.6 35.1L0.6 35.3L1.5 35L1.6 35.1" transform="translate()" fill="rgba(204,102,51,1)"/><path d="M18 34Q13.1 32.7, 1.6 35.1L1.5 35L3.2 33.8Q5.2 32.2, 6.8 29.4Q8.4 26.4, 9 22.2Q17.8 14.2, 21.6 5.6Q22.4 17.2, 20.2 22.9Q18 28.6, 17.8 30.7Q17.6 32.8, 18 34" transform="translate()" fill="rgba(153,51,0,1)"/><path d="M19.8 31.8L18 34M3.2 33.8L2.1 34" transform="translate()" fill="none" stroke-width="0.05" stroke="rgba(0,0,0,1)"/><path d="M19.1 15.6L15.4 21.1Q14.2 22.6, 12.3 26.9Q11 30.4, 8.8 32Q6.5 33.8, 4.8 34.4L0.6 35.2L3.3 33.2Q4.6 31.8, 4.8 27.4Q4.9 24.8, 6 21.5Q7.4 21.2, 8.8 20Q12.7 16.9, 15.2 14Q17.8 11.2, 18.7 9.4L20.6 5.8Q21.6 3.7, 23 2.8Q23.8 5.2, 22.5 8.8Q21.2 12.3, 19.1 15.6" transform="translate()" fill="rgba(202,62,2,1)"/><path d="M24.6 9.8Q25.4 18.2, 23.4 24.9L20.8 32.8L20.4 34L18 34Q17.6 32.8, 17.8 30.7Q18 28.6, 20.2 22.9Q22.4 17.2, 21.6 5.6Q17.8 14.2, 9 22.2Q8.4 26.4, 6.8 29.4Q5.2 32.2, 3.2 33.8L1.5 35L0.6 35.3L0 35.5Q1.8 33.6, 3.1 31.2Q4.3 28.8, 4.5 24L4.5 20.8L4.6 20.3Q4.8 20, 10.4 15.4Q16.1 10.9, 20.4 1.2Q21.8 -1.1, 23.4 1.2Q23.8 1.2, 24.6 9.8" transform="translate()" fill="rgba(34,10,0,1)"/></g></svg>
                5:<svg width="14.58px" height="10.55px" viewBox="0 0 14.58 10.55" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate(-0.02, 0)"><path d="M11.5 3.4Q9.4 5.3, 7 6.8Q4.6 8.4, 2.9 8.8L4.7 9.2L8.8 9.1L10.6 8.8L10.6 8.8Q9.5 10.2, 6.6 10.6L1.7 10.2Q-0.2 9.5, 0 8.2L4.5 6L9.1 3.2Q11.2 1.6, 12.4 0L14.6 0Q13.5 1.6, 11.5 3.4" transform="translate()" fill="rgba(34,10,0,1)"/></g></svg>
                6:<svg width="43.54px" height="40.33px" viewBox="0 0 43.54 40.33" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate()"><path d="M42.4 3.8Q44.2 5.1, 43.2 14.2Q42.4 21.3, 37 28L34.6 30.6L33.6 31.7L33.3 31.9Q39.8 22.2, 40.8 12.9Q41.7 3.8, 38.3 0.2L38.2 0L38.4 0.1L38.4 0.2Q41.5 2.3, 42.4 3.8M38.3 0.2L38.4 0.1L38.3 0.2M25.9 40.3Q7.9 41.2, 0.2 17.6L0 17.2L0.2 17.6L0.2 17.6L0.2 17.6L1.7 19Q3.6 21, 4.8 23Q9.4 38.2, 25.6 37.8L27.6 38L29 38.2L25.9 40.3M25.7 39.4L27.6 38L25.7 39.4" transform="translate()" fill="rgba(0,0,0,1)"/><path d="M1.9 17.6Q11.8 32.8, 31.2 34.7L27.6 38L25.6 37.8Q9.4 38.2, 4.8 23Q3.6 21, 1.7 19L0.2 17.6L1.9 17.6" transform="translate()" fill="rgba(167,52,3,1)"/><path d="M38.3 0.2Q41.7 3.8, 40.8 12.9Q39.8 22.2, 33.3 31.9L33.4 32.1L31.2 34.7Q11.8 32.8, 1.9 17.6Q20.5 17.8, 38.3 0.2" transform="translate()" fill="rgba(102,33,0,1)"/><path d="M33.6 31.7L34.6 30.6L37 28Q42.4 21.3, 43.2 14.2Q44.2 5.1, 42.4 3.8M38.4 0.1L38.3 0.2Q41.7 3.8, 40.8 12.9Q39.8 22.2, 33.3 31.9M38.2 0L38.3 0.2M25.9 40.3Q7.9 41.2, 0.2 17.6L0 17.2L0.2 17.6L0.2 17.6M1.7 19L0.2 17.6M34.6 30.6L33.7 31.8L33.4 32.1M25.6 37.8Q9.4 38.2, 4.8 23M27.6 38L25.7 39.4" transform="translate()" fill="none" stroke-width="0.05" stroke="rgba(0,0,0,1)"/></g></svg>
                7:<svg width="49.56px" height="39.2px" viewBox="0 0 49.56 39.2" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate(0.01, 0)"><path d="M44.7 37.4L43.4 37.1L40.5 35.8L40.2 35.6L37.6 33.9Q37 34, 34 31.6Q30.8 29.4, 24.7 25.8Q18.6 22.2, 11.2 20.4Q13.1 19.6, 15.2 9.9L15.6 4.8L15.6 6.2Q17.8 9.6, 29 15.2Q30.6 16, 38 20.3Q45.4 24.6, 47 26.2Q48 28, 47.6 28.8L47.4 30.6Q47.6 31.5, 46.4 34.5Q45.4 37.2, 44.7 37.4M11.4 22.6L16.9 24Q17.8 24.6, 9.2 29L6.4 30.6L5 31.4Q8.8 27.3, 11.4 22.6" transform="translate()" fill="rgba(102,33,0,1)"/><path d="M15.6 4.8L15.6 4.7L15.6 4.4L15.8 2.1Q16 1.2, 15.4 0L18.2 5.2Q19.4 7, 23.3 9.2Q27.2 11.8, 31.8 14.2Q36.4 16.8, 40.7 19Q44.7 21, 47.2 22.9Q49.6 24.8, 49.6 28.2Q49.5 31.6, 44 39.2L44.7 37.4Q45.4 37.2, 46.4 34.5Q47.6 31.5, 47.4 30.6L47.6 28.8Q48 28, 47 26.2Q45.4 24.6, 38 20.3Q30.6 16, 29 15.2Q17.8 9.6, 15.6 6.2L15.6 4.8" transform="translate()" fill="rgba(9,1,0,1)"/><path d="M47.2 22.9Q49.6 24.8, 49.6 28.2" transform="translate()" fill="none" stroke-width="0.05" stroke="rgba(0,0,0,1)"/><path d="M11.4 21.3Q5.4 20.4, 1.1 19Q-3.2 17.8, 9.2 17Q20.6 20.1, 28 25.7L38.3 33.6Q41.2 35.8, 40.4 35.6Q39.5 35.4, 35.4 32.9Q31.3 30.4, 24.4 26.3Q17.5 22.2, 11.4 21.3" transform="translate()" fill="rgba(167,52,3,1)"/><path d="M11.4 22.6Q7.7 22.4, 3 19.8L11.2 20.4Q18.6 22.2, 24.7 25.8Q30.8 29.4, 34 31.6Q37 34, 37.6 33.9L40.2 35.6L39.6 35.4L40.4 35.8L36.6 34Q34.2 33, 31.6 31.6L24 27.8L19.8 25.6L17.4 27Q15.2 28.3, 11.7 29.8L6.8 32L5.6 32.2L4.2 32L5 31.4L6.4 30.6L9.2 29Q17.8 24.6, 16.9 24L11.4 22.6" transform="translate()" fill="rgba(9,1,0,1)"/></g></svg>
                8:<svg width="34px" height="12.35px" viewBox="0 0 34 12.35" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate(2.6, 1.4)"><path d="M23.7 8.7L19.1 9L13.8 8.6Q7.2 8.4, -0.2 8.6Q0.1 7.4, 1.8 5.1Q3.5 2.9, 6 2Q8.4 1.2, 10.4 1L14 1.4L16.1 2L16.8 2.2L17.2 2.3Q21.1 3.5, 24.8 6.1Q25.4 6.5, 27.4 5.2L27.2 5.4L27.2 5.4L26.9 6L26.8 6.3L26.6 6.6L26.2 7.4Q25.3 8.4, 23.7 8.7" transform="translate()" fill="rgba(102,33,0,1)"/><path d="M9 4Q3.8 5.2, 3.8 9Q-0.6 10.4, -1.2 10.3Q-1.8 10, -1.7 8.8Q-1.6 7.6, -0.2 5.4Q1.2 3.2, 3.5 2.2L6.6 1L4.6 1.4Q4.6 1.2, 7.8 0.6L12.6 0.2Q19 0, 23.7 5.5Q22.3 4.3, 18.3 3.5Q14.2 2.6, 9 4" transform="translate()" fill="rgba(167,52,3,1)"/><path d="M19.2 1.2L20.8 2.5L24.2 5.4L24.8 6.1Q21 3.5, 17.1 2.3L16.8 2.2L16 2L14 1.4L10.4 1L6 2Q3.4 2.9, 1.8 5.1Q0 7.4, -0.2 8.6Q7.2 8.4, 13.8 8.6L19 9L23.6 8.7Q25.2 8.4, 26.1 7.4L26.5 6.6L26.8 6.3L26.8 6L27.2 5.4L27.2 5.4L27.3 5.2L27.4 4.8L31.4 -1.4L28.2 8.4Q27.6 9.7, 24 10.4L18.1 10.9Q10.4 10.4, 2.7 10.9L-0.6 11L-1.2 10.9Q-2.5 10.4, -2.6 9L-2.6 9Q-2.6 7.8, -1.9 6.4Q-1.2 4.8, 0.4 3.4Q1.8 2, 2.8 1.4Q8 -1.8, 13.8 -0.4L15.5 0Q16.8 0.2, 18 0.8L19.2 1.2" transform="translate()" fill="rgba(9,1,0,1)"/><path d="M19.2 1.2L18 0.8Q16.8 0.2, 15.5 0M31.4 -1.4L27.4 4.8" transform="translate()" fill="none" stroke-width="0.05" stroke="rgba(0,0,0,1)"/></g></svg>
                9:<svg width="24.92px" height="12.95px" viewBox="0 0 24.92 12.95" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate(2.1, 1.15)"><path d="M18.1 9.3L5.4 9.8L0.4 9.5Q0.4 8.4, 2 5.8Q3.6 3, 7.3 2Q10.9 0.8, 15.6 4.2Q14.4 2.8, 12.9 1.8Q15.8 3.1, 21.9 -1.2L21.6 -0.4Q22 5.2, 18.1 9.3" transform="translate()" fill="rgba(102,33,0,1)"/><path d="M1.7 10.2L0.6 10.2Q-0.6 8.2, 1.4 5.4Q3.4 2.6, 3.8 2.4Q5 1.1, 8.4 1Q12 0.7, 15.2 4L15 4Q13.5 2.8, 10.6 2.5Q7.8 2.2, 5 4.6Q2.9 6.2, 1.7 10.2" transform="translate()" fill="rgba(167,52,3,1)"/><path d="M18.1 9.3Q22 5.2, 21.6 -0.8Q24.4 -1.2, 20.6 11.3L3.2 11.8Q-1.6 11.6, -2.1 9.8L-2 9.3Q0.2 1.4, 7.7 0L8 0Q9.4 0, 10.4 0.6L11.2 0.9L12 1.2L12.2 1.4Q12.6 1.5, 12.9 1.8Q14.4 2.8, 15.6 4.2Q10.9 0.8, 7.3 2Q3.6 3, 2 5.8Q0.4 8.4, 0.4 9.5L5.4 9.8L18.1 9.3" transform="translate()" fill="rgba(9,1,0,1)"/></g></svg>
                10:<svg width="25.5px" height="23.6px" viewBox="0 0 25.5 23.6" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate(0.7, 3.15)"><path d="M4.5 19L1 14.6L8 6.2L7.7 6L9.3 4.4Q11.4 3.7, 16.4 3.6L16.7 3.5L17 3.5L20.8 6L21.4 8.6Q13.3 14.4, 4.5 19" transform="translate()" fill="rgba(102,33,0,1)"/><path d="M20.8 6L24.8 8.6L12.8 16.2Q7.4 19.3, 5.5 20.4L4.4 19.1L4.7 19.3L4.5 19Q13.3 14.4, 21.4 8.6L20.8 6M1 14.6L0 13.6L13 -3.2Q13.6 -1.6, 13.7 0Q13.1 1.4, 9.3 4.4L7.7 6L8 6.2L1 14.6M16.4 3.6L15.4 3.5L16.7 3.5L16.4 3.6" transform="translate()" fill="rgba(9,1,0,1)"/><path d="M18 3.8Q8.8 7.2, 4.8 12L-0.7 18.6Q-0.6 16.2, 0.5 14.2Q1.5 12, 5 8.4Q10.2 2.8, 13.6 0L13.6 -0.2L14.5 1.3L18 3.8" transform="translate()" fill="rgba(167,52,3,1)"/></g></svg>
                11:<svg width="120.55px" height="82.35px" viewBox="0 0 120.55 82.35" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate(-0.02, 0)"><path d="M37.4 13.7Q54 9.2, 70.6 5.6Q77.2 24.2, 87.2 29L88.7 29.8Q98 33.8, 106.4 34.1Q109.8 33, 115.2 42Q120.7 50.9, 109.7 62Q98.6 73.2, 88.8 76.2L85.7 77.2L78.2 78.2Q61 80, 47.8 75.4Q-23.9 34, 37.4 13.7" transform="translate()" fill="rgba(153,51,0,1)"/><path d="M117.1 39Q122.2 45.2, 119.8 52.8Q117.6 60.3, 106.2 69.2Q101 73.3, 95.8 76L95 76.4Q89.6 79, 84.4 80L79.3 81L63.5 82.4Q51.6 82, 40.3 79.2Q29 76.3, 19.7 70.7Q10.4 65, 3.2 54Q-4.6 40, 6.8 18.4Q47.2 9.3, 72.5 0Q77.2 18.6, 83 22.6Q88.7 26.4, 91.8 28.1Q95 29.7, 110 32.2Q112 32.8, 117.1 39M85.7 77.2L88.8 76.2Q98.6 73.2, 109.7 62Q120.7 50.9, 115.2 42Q109.8 33, 106.4 34.1Q98 33.8, 88.7 29.8L87.2 29Q77.2 24.2, 70.6 5.6Q54 9.2, 37.4 13.7L37.4 13.7L33 14.9L16.4 19.8L9 22Q-12.4 71.1, 58.4 80.5L72.4 79.9L78.4 79L85.7 77.2M79.3 81L78.4 79L79.3 81" transform="translate()" fill="rgba(34,10,0,1)"/><path d="M37.4 13.7Q-23.9 34, 47.8 75.4Q61 80, 78.2 78.2L85.7 77.2L78.4 79L72.4 79.9L58.4 80.5Q-12.4 71.1, 9 22L16.4 19.8L33 14.9L37.4 13.7L37.4 13.7M78.2 78.2L78.4 79L78.2 78.2" transform="translate()" fill="rgba(202,62,2,1)"/><path d="M78.4 79L79.3 81M78.4 79L78.2 78.2" transform="translate()" fill="none" stroke-width="1" stroke="rgba(51,51,51,1)"/></g></svg>
                12:<svg width="49.78px" height="62.15px" viewBox="0 0 49.78 62.15" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate(-71.7, -20.75)"><path d="M83.5 28.8Q98.3 33.7, 108.1 32.1Q113.2 32.2, 116.8 42Q120.4 51.8, 111 61.2Q101.6 70.5, 96.2 73.4Q90.7 76.2, 85.6 78.5Q82.6 80.1, 80.2 80.6L79.2 81.8L78.9 81Q65.9 47.6, 82.4 28.4L83.4 28.8L83.5 28.8" transform="translate()" fill="rgba(153,51,0,1)"/><path d="M108.1 32.1Q98.3 33.7, 83.5 28.8L83.4 28.8Q82.4 28.9, 71.7 20.8Q92.9 30.6, 110.4 29Q111.8 29, 115.4 31.4Q116.4 32.1, 119.7 37.6Q123 43, 120.2 51.6Q117.6 60.3, 109.3 66.8Q101 73.3, 99.4 74.9Q97.9 76.5, 94.5 78.2Q91 80, 86.8 81.2Q82.6 82.4, 78.8 82.9L78.8 81L78.9 81L80 80.8L80.2 80.6Q82.6 80.1, 85.6 78.5Q90.7 76.2, 96.2 73.4Q101.6 70.5, 111 61.2Q120.4 51.8, 116.8 42Q113.2 32.2, 108.1 32.1" transform="translate()" fill="rgba(34,10,0,1)"/></g></svg>
                13:<svg width="67.1px" height="24.9px" viewBox="0 0 67.1 24.9" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate(0, 0)"><path d="M62.6 3.8Q63.6 3, 63.6 3.2Q63.9 3, 64.5 4.2L65.4 6.1L65.4 6.2L65.4 6.1Q65 7, 61.8 9Q58.6 10.8, 49.7 14.8Q40.7 18.7, 27.9 21.2Q15 23.6, 3.6 23.1Q3 21, 3.4 19.9L4.2 18L7 17.8L13.4 17.6L23.4 16.8Q29.5 16.2, 38.4 14.1Q47.2 12, 54.2 8.4Q61.4 4.8, 62.6 3.8" transform="translate()" fill="rgba(255,102,0,1)"/><path d="M3.2 16.9Q12.8 17.9, 20.2 16.7L36.9 13.2Q44.2 11.7, 51 9Q57.2 6.5, 63.4 3Q60.9 5.4, 58.4 7.2Q56 9, 52.4 10.6Q48.8 12.2, 42.8 14.2Q36.6 16.1, 20.8 18.4Q5 20.6, 2.4 23Q3.2 19.3, 3.2 16.9" transform="translate()" fill="rgba(254,158,95,1)"/><path d="M64.5 4.2Q63.9 3, 63.6 3.2Q63.6 3, 62.6 3.8Q61.4 4.8, 54.2 8.4Q47.2 12, 38.4 14.1Q29.5 16.2, 23.4 16.8L13.4 17.6L7 17.8L4.2 18L3.4 19.9Q3 21, 3.6 23.1Q15 23.6, 27.9 21.2Q40.7 18.7, 49.7 14.8Q58.6 10.8, 61.8 9Q65 7, 65.4 6.1L65.4 6.2L65.4 6.1L64.5 4.2M61.8 10.8Q58.7 12.7, 48.8 16.8Q38.9 20.8, 30.1 22.4L13.6 24.4L3 24.9Q0 24.9, 0 23.7Q0 22.4, 1 20.4L2.8 15.6Q5.2 15.6, 9.2 15.6L21.8 14.4Q30.5 13.2, 40.9 10.4Q51.3 7.5, 56.6 4.4L63.3 0Q65.4 2.4, 65.9 3.7L67 6Q67.4 7, 66.2 8L61.8 10.8" transform="translate()" fill="rgba(34,10,0,1)"/></g></svg>
                14:<svg width="96.21px" height="55.05px" viewBox="0 0 96.21 55.05" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate(0.01, 0.02)"><path d="M42.2 43Q51 46.5, 61 47Q68.8 47.3, 76.6 47Q86 45.9, 89.2 47.2Q90.4 49.5, 79 51.8Q67.5 54, 55.6 53.6Q46 52.8, 36.6 49.2Q26.4 45.2, 17.3 39.2Q11 35, 6.7 28.6Q2.4 22.2, 2.4 15.4Q2.4 8.4, 3.2 6.4Q8 -2.4, 13.5 9.6Q19 21.5, 21 25Q23 28.4, 28.2 34Q33.4 39.6, 42.2 43" transform="translate()" fill="rgba(255,153,102,1)"/><path d="M11 2.9Q5.5 7.4, 5.7 14.4Q6.6 28.1, 23.4 39.6Q40.3 51.2, 56.3 52.3Q72.3 53.4, 80.2 51.4Q88.1 49.5, 88.6 47.9Q89.3 46.2, 81.2 46.8Q95.2 44, 92.6 47.8Q90 51.4, 65.8 53.8Q41.6 56.2, 21 41.6Q17.2 39, 14.1 37.6Q0.5 25, 1.8 15.5Q3.2 3.8, 5 2.9L6 2.6L7.4 2.2L11 2.9" transform="translate()" fill="rgba(250,196,116,1)"/><path d="M42.2 43Q33.4 39.6, 28.2 34Q23 28.4, 21 25Q19 21.5, 13.5 9.6Q8 -2.4, 3.2 6.4Q2.4 8.4, 2.4 15.4Q2.4 22.2, 6.7 28.6Q11 35, 17.3 39.2Q26.4 45.2, 36.6 49.2Q46 52.8, 55.6 53.6Q67.5 54, 79 51.8Q90.4 49.5, 92.5 46.8Q89.6 45.6, 76.6 47Q68.8 47.3, 61 47Q51 46.5, 42.2 43M36.8 50.8Q28.3 47.9, 20.6 43Q13 38.4, 7.6 31.6Q1.4 23.9, 0.2 14.2Q-1 4.4, 5 0.3Q11.6 -1.5, 15.6 6.8Q19.7 15, 23 22.2Q26.2 29.4, 32.1 34.4Q38 39.2, 44.7 41.7Q51.3 44.2, 59 45.2Q70.9 46.5, 82.8 45.2L96.2 45.6Q91.7 52, 78.4 53.8Q65 55.6, 55.4 54.8Q45.8 53.9, 36.8 50.8" transform="translate()" fill="rgba(34,10,0,1)"/></g></svg>
                15:<svg width="14.92px" height="32.35px" viewBox="0 0 14.92 32.35" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate()"><path d="M7.2 16.4L10.6 10L11.6 8.2L12 12.3L11.8 18.5Q11.4 23.4, 8 26.2L7.5 25.5Q4.6 21.4, 7.2 16.4" transform="translate()" fill="rgba(153,51,0,1)"/><path d="M13.6 10.8Q13.2 13, 12 13.7Q10.8 14.3, 8.2 19.2Q6.2 23.2, 6.8 24.6L5.2 22.4Q4.3 21.2, 4.8 19.8Q5.2 18.2, 8.6 12.5Q11.8 6.8, 11.8 5.6Q11.7 4.5, 12.1 6.8Q14.1 8.5, 13.6 10.8" transform="translate()" fill="rgba(202,62,2,1)"/><path d="M14.6 11.4Q15.4 19.6, 13.9 26.6Q12.3 33.6, 11.8 32.1L10.7 29.8L8 26.2Q11.4 23.4, 11.8 18.5L12 12.3L11.6 8.2L10.6 10L7.2 16.4Q4.6 21.4, 7.5 25.5Q2.5 19.3, 0 19.8Q1.8 18.2, 3.1 16.8Q4.3 15.2, 5.6 12.7Q6.8 10.1, 8 8.2Q9.1 6.4, 11.4 0Q13.8 3.1, 14.6 11.4" transform="translate()" fill="rgba(34,10,0,1)"/></g></svg>
                16:<svg width="45.95px" height="60.8px" viewBox="0 0 45.95 60.8" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate(-4.9, 0)"><path d="M44 25L44 24.8L45.4 25.3L44 25" transform="translate()" fill="rgba(204,102,51,1)"/><path d="M44 24.8L44 25Q39.7 26.5, 30.6 38.4L22.1 48.8Q15 57.3, 13 56.6Q11.8 55.8, 9.6 51.4Q7.2 47.1, 7.4 46.4Q7.7 45.7, 8.4 45.4Q9.1 45.1, 9.8 43.4Q10.5 41.7, 11.6 40.4L18.9 31.7Q25.1 24.2, 29.2 15Q29.8 17.6, 39.9 23.2L44 24.8" transform="translate()" fill="rgba(153,51,0,1)"/><path d="M15 60.4L13.8 60.8" transform="translate()" fill="none" stroke-width="0.05" stroke="rgba(0,0,0,1)"/><path d="M34.4 0Q34.8 3.7, 32.8 12.2Q30.8 20.8, 21.8 32.8Q12.8 44.8, 11.6 47.2Q10.6 49.7, 11.2 51.4Q6.4 50.3, 5.6 48.6L5.6 47.5L21.6 25Q29.8 13.1, 34.4 0" transform="translate()" fill="rgba(202,62,2,1)"/><path d="M24.2 14.7Q24.3 18.3, 24.7 18.8L24.8 18.8L26.9 15L32.1 5.2L29.2 15Q25.1 24.2, 18.8 31.7L11.6 40.4L9.6 43L8.1 45Q7.4 45.7, 7.3 46.4Q7.2 47.1, 9.6 51.4Q11.8 55.8, 13 56.6Q15 57.3, 22.1 48.8L30.6 38.4Q39.6 26.5, 44 25L45.4 25.3L45.6 25.4L46 25.5Q47.4 25.9, 48.5 26.4L50.8 26.6L44.4 28.4Q42.2 29.4, 38.6 33.4Q35 37.4, 31 42.2Q26.9 47, 23.5 51.7Q20.3 55.9, 15 60.4L13.8 60.8Q11 60, 9 57Q7 54, 6.2 51.8Q5.4 49.6, 4.9 45.8L6 44.4L7.7 42L11.6 36.5L17.9 28.6L20.8 25.1Q21.8 23.8, 22.4 21.2Q23 18.6, 22.6 13.4Q22.2 8.3, 22.4 4.2Q22.6 0, 23 2.6L23.8 8.2L24.2 14.7" transform="translate()" fill="rgba(34,10,0,1)"/></g></svg>
                17:<svg width="41.99px" height="23.96px" viewBox="0 0 41.99 23.96" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate(-0.01, 0)"><path d="M35.6 17.4L35.8 17.3L36.2 18.5L35.6 17.4" transform="translate()" fill="rgba(0,0,0,1)"/><path d="M38.9 18.9L39.6 18.8L38.9 19.6L38.9 18.9" transform="translate()" fill="rgba(204,102,51,1)"/><path d="M38.9 19.6L37.6 20.2L37.4 20.2L37.4 20.2L37 20.2Q34.8 21.1, 33.4 20.8Q30 20.2, 27.4 18.8L21 15.4Q12.8 11.5, 3.5 7.6Q4.5 6.2, 7.8 4.4Q11.2 2.4, 14.8 2.8Q18.4 3, 21 4Q23.4 4.9, 25.3 6.4L27.6 8.4L28.4 9.1L28.8 9.4Q32.5 12.8, 35.6 17.4L36.2 18.5Q36.5 19, 37.6 19L38.9 18.9L38.9 19.6" transform="translate()" fill="rgba(153,51,0,1)"/><path d="M2.1 9.6Q1 10.2, 0.6 9.2Q-0.2 6.7, 2 4.9L4 3Q5.7 1.4, 7.8 1.2L16 0.8Q18.6 1, 21.8 2.6Q25 4, 28.2 7.4L32.9 12.8Q34.4 14.8, 36 18.2Q35.1 16.8, 31.2 13Q27.4 9.2, 22.8 7.8Q18.2 6.4, 16.4 6.3Q14.4 6, 11 6.4Q7.6 6.7, 5.2 10.2Q3.2 8.8, 2.1 9.6" transform="translate()" fill="rgba(202,62,2,1)"/><path d="M41.2 21.4L41.2 21.4L39.2 23.2Q37.6 24.6, 32.8 23.5Q28.8 22.6, 25.1 20.8Q15.6 15.8, 5.8 12.2L1.6 10.4L0.8 9.8Q-0.4 8.6, 0.2 6.8L0.2 6.7Q0.8 5.3, 2.6 3.8Q4.4 2.2, 7.1 1.3L11.2 0.2Q19.5 -0.8, 26 4.1L28 5.6L30.6 8L31.9 9.2L33.2 11.6Q34.5 14, 35.8 17.3L35.6 17.4Q32.5 12.8, 28.8 9.4L28.4 9.1L27.6 8.4L25.3 6.4Q23.4 4.9, 21 4Q18.4 3, 14.8 2.8Q11.2 2.4, 7.8 4.4Q4.5 6.2, 3.5 7.6Q12.8 11.5, 21 15.4L27.4 18.8Q30 20.2, 34.2 20.6Q38.2 20.8, 39.4 19.2L39.4 19.2L39.6 19L39.6 18.9L40.2 18.3L42 20.6L41.6 21L41.2 21.4" transform="translate()" fill="rgba(34,10,0,1)"/></g></svg>
                18:<svg width="24.9px" height="36.35px" viewBox="0 0 24.9 36.35" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate(1.7, 0)"><path d="M10.6 28.9L10.3 28.8L10.6 28.7L10.6 28.9" transform="translate()" fill="rgba(0,0,0,1)"/><path d="M21.8 29.7Q20.6 37.2, 10.6 28.9L10.6 28.7L5.3 20L4.8 20.3Q1.8 18.6, 6.8 8.9L15.2 4.2Q19 17, 20.8 29.6L21.8 29.4L21.8 29.7" transform="translate()" fill="rgba(153,51,0,1)"/><path d="M6.8 8.9L5.4 9.6L6.2 7.6L17 0L21.2 18.4L23.2 29.4L21.8 29.7L21.8 29.4L20.8 29.6Q19 17, 15.2 4.2L6.8 8.9" transform="translate()" fill="rgba(34,10,0,1)"/><path d="M21.4 31.4Q20 35.2, 17.9 35.8Q15.8 36.5, 14.2 34Q12.8 31.4, 9.4 28.2L4.4 23.2Q2.6 21.4, 9.2 17.4Q8.2 22.4, 12.8 28.7Q17.4 35, 21.4 31.4" transform="translate()" fill="rgba(202,62,2,1)"/><path d="M16.5 36.4Q7.7 30, -1.7 20.3Q1.2 21.4, 3 20.7L5.3 20L10.6 28.7L16.5 36.4" transform="translate()" fill="rgba(34,10,0,1)"/></g></svg>
                19:<svg width="39.25px" height="54.85px" viewBox="0 0 39.25 54.85" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate(0, 10.45)"><path d="M36.4 30.4L36.6 30.4L36.4 30.8L36.4 30.4M0.4 32.2L0.3 32.4L0 32L0.4 32.2" transform="translate()" fill="rgba(0,0,0,1)"/><path d="M29 -2.8L31.2 -0.1Q32.7 2, 35.3 10.6Q37.9 19, 36.4 30.4L36.4 30.8L36.6 30.9Q35.6 35.2, 33.9 38.8L32 39.6Q17.6 46.8, 6.9 35.2Q5 34, 2.3 33L0.4 32.2Q8 3.3, 28.7 -0.6L28.4 0L28.6 0.1L29 0.2L29 -2.8" transform="translate()" fill="rgba(153,51,0,1)"/><path d="M29 -2.8L30.7 -10.4Q34.6 -3.2, 37.3 7Q39.9 17.2, 38 25.6L37.1 29L36.6 30.4L36.4 30.4Q37.9 19, 35.3 10.6Q32.7 2, 31.2 -0.1L29 -2.8" transform="translate()" fill="rgba(34,10,0,1)"/><path d="M0.3 32.4L0 32L0.4 32.2" transform="translate()" fill="none" stroke-width="0.05" stroke="rgba(0,0,0,1)"/><path d="M39.2 36.8Q38.1 38.4, 35.7 40.2Q18.9 51.6, 0.3 32.4" transform="translate()" fill="none" stroke-width="0.05" stroke="rgba(0,0,0,1)"/><path d="M2.3 33L0.4 32.2" transform="translate()" fill="none" stroke-width="0.05" stroke="rgba(0,0,0,1)"/><path d="M35.2 38.6Q37.2 37.4, 38.7 36.8L33.2 41.2Q31.6 42.6, 27.3 43.2Q23 43.9, 19.8 43.2Q16.4 42.4, 13.8 41.4Q11.2 40.3, 8 38.2L3.6 35L0.3 32.2L1.2 32L4.7 32.2Q6.6 32.6, 13.2 37Q19.6 41.2, 26.4 40.5Q33.2 39.8, 35.2 38.6" transform="translate()" fill="rgba(202,62,2,1)"/><path d="M39.2 36.8Q38.1 38.4, 35.7 40.2Q18.9 51.6, 0.3 32.4L0.4 32.2L2.3 33Q5 34, 6.9 35.2Q17.6 46.8, 32 39.6L39.2 36.8" transform="translate()" fill="rgba(34,10,0,1)"/></g></svg>
                20:<svg width="26.05px" height="21.31px" viewBox="0 0 26.05 21.31" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate()"><path d="M17.2 8.5Q18.7 4, 22.4 0.7Q23.6 9, 22.4 17Q14.7 18.8, 8.4 18.6Q4.1 19, 2.4 17.4L3.4 13.6Q4.2 10, 9 8.2Q13.6 6.4, 19.1 10.6L17.2 8.5" transform="translate()" fill="rgba(153,51,0,1)"/><path d="M16.9 9.6L14.4 9Q13.1 8.8, 10.9 9.6Q8.7 10.2, 7.8 11.2Q6.8 12, 6 14Q5.3 15.8, 5.7 18.8L1.1 18.4Q0.4 18, 1.2 17.8Q1.8 17.5, 1.8 17.1Q1.8 14.6, 3.2 12.4Q3.6 11.6, 4.2 11.2Q5.7 9.8, 7.3 8.7Q9.2 7.3, 11.6 7Q15.6 7.2, 18.5 10L18.6 10.1L16.9 9.6" transform="translate()" fill="rgba(202,62,2,1)"/><path d="M22.4 0.7L24.3 0Q27.2 8.4, 25.4 19.4Q14.6 21.4, 6.1 21.3Q0.7 21.4, 0 19.2L0.1 18.5Q1.8 7.9, 10 5.5L10.4 5.4Q11.8 5.6, 13.1 6L13.3 6L14.8 6.8L15 6.9L17.2 8.5L19.1 10.6Q13.6 6.4, 9 8.2Q4.2 10, 3.4 13.6L2.4 17.4Q4.1 19, 8.4 18.6Q14.7 18.8, 22.4 17Q23.6 9, 22.4 0.7" transform="translate()" fill="rgba(34,10,0,1)"/></g></svg>
                21:<svg width="1350px" height="900px" viewBox="0 0 1350 900" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate(450, 300)"><path d="M900 -300L900 600L-450 600L-450 -300L900 -300M450 300L450 0L0 0L0 300L450 300" transform="translate()" fill="rgba(0,0,0,1)"/><path d="M900 -300L900 600L-450 600L-450 -300L900 -300ZM450 300L0 300L0 0L450 0L450 300Z" transform="translate()" fill="none" stroke-width="0.05" stroke="rgba(0,0,0,1)"/></g></svg>
            """.trimIndent(),
            out.joinToString("\n")
        )
    }

    @Test
    fun dog() = swfTest {
        if (OS.isAndroid) return@swfTest
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
		val lib = resourcesVfs["test4.swf"].readSWFDeserializing(views, fastSwfExportConfig())
		println(lib)
	}

	@Test
	fun name6() = swfTest {
		val lib = resourcesVfs["as3test.swf"].readSWFDeserializing(views, fastSwfExportConfig())
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
		val lib = resourcesVfs["soundtest.swf"].readSWFDeserializing(views, fastSwfExportConfig())
		println(lib)
	}

	@Test
	fun name8() = swfTest {
		val lib = resourcesVfs["progressbar.swf"].readSWFDeserializing(views, fastSwfExportConfig())
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
		val lib = resourcesVfs["exports.swf"].readSWFDeserializing(views, fastSwfExportConfig())
		assertEquals(listOf("Graphic1Export", "MC1Export", "MainTimeLine"), lib.symbolsByName.keys.sorted())
		val sh = lib.createMovieClip("Graphic1Export")
		val mc = lib.createMovieClip("MC1Export")

		//AnimateSerializer.gen(lib).writeToFile("c:/temp/file.ani")

		//AnimateDeserializer.read(AnimateSerializer.gen(lib), views)
	}

	@Test
	fun props() = swfTest {
		val lib = resourcesVfs["props.swf"].readSWFDeserializing(views, fastSwfExportConfig())
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
		val lib = resourcesVfs["shapes.swf"].readSWFDeserializing(views, fastSwfExportConfig())
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
        if (OS.isAndroid) return@swfTest
		val lib = resourcesVfs["morph.swf"].readSWFDeserializing(views, fastSwfExportConfig())
		//val lib = ResourcesVfs["shapes.swf"].readSWFDeserializing(views, debug = false)
		//lib.writeTo(LocalVfs("c:/temp")["morph.ani"])
	}

	@Test
	fun ninepatch() = swfTest {
		val lib = resourcesVfs["ninepatch.swf"].readSWFDeserializing(views, fastSwfExportConfig())
		//val lib = ResourcesVfs["shapes.swf"].readSWFDeserializing(views, debug = false)
		//lib.writeTo(LocalVfs("c:/temp")["ninepatch.ani"])
	}

	@Test
	fun stopattheend() = swfTest {
		val lib = resourcesVfs["stop_at_the_end.swf"].readSWFDeserializing(views, fastSwfExportConfig())
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
		val lib = resourcesVfs["cameras.swf"].readSWFDeserializing(views, fastSwfExportConfig())
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
		val lib = resourcesVfs["events.swf"].readSWFDeserializing(views, fastSwfExportConfig())
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
		val lib = localVfs("c:/temp/test29.swf").readSWFDeserializing(views, fastSwfExportConfig())
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
