package com.soywiz.korge.sample

import com.soywiz.korge.Korge
import com.soywiz.korge.animate.AnLibrary
import com.soywiz.korge.resources.Path
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korim.color.RGBA
import java.util.*

object Sample3 : Module() {
	@JvmStatic fun main(args: Array<String>) = Korge(Sample3)

	//override val bgcolor: Int = Colors.WHITE
	override val bgcolor: Int = RGBA(0x70, 0x70, 0x70, 0xFF)
	override val mainScene: Class<out Scene> = MainScene::class.java

	class MainScene(
		//@Path("semilla/semilla.swf") val semillaLibrary: AnLibrary
		//@Path("shape1.swf") val lib: AnLibrary
		//@Path("shape2.swf") val lib: AnLibrary
		//@Path("shape2.swf") val lib: AnLibrary
		//@Path("mask.swf") val lib: AnLibrary
		//@Path("morph.swf") val lib: AnLibrary
		//@Path("color.swf") val lib: AnLibrary
		//@Path("ninepatch.swf") val lib: AnLibrary
		//@Path("complexflow.swf") val lib: AnLibrary
		@Path("loop.swf") val lib: AnLibrary
		//@Path("eyes.swf") val eyesLibrary: AnLibrary
		//@Path("eyes2.swf") val eyesLibrary: AnLibrary
		//@Path("radialgradient.swf") val library: AnLibrary
		//@Path("radialgradient2.swf") val library: AnLibrary
		//@Path("gradient1.swf") val library: AnLibrary
	) : Scene() {
		val random = Random()

		suspend override fun sceneInit(sceneView: Container) {
			//SwfLoader.load(views)

			val mt = lib.createMainTimeLine()
			for (state in mt.symbol.states.values) {
				val sstate = state.subTimeline
				//println("name=${state.name}: startTime=${state.startTime}, totalTime=${sstate.totalTimeSeconds} : ${sstate.actions}")
			}
			println(mt.symbol.states.keys)

			//sceneView += lib.createMainTimeLine().apply { scale = 4.0 }
			sceneView += mt.apply { scale = 1.0 }
			//sceneView += semillaLibrary.createMainTimeLine().apply { this["semilla"].play("anim2") }
			//sceneView += eyesLibrary.createMainTimeLine().apply { scale = 3.0 }
			//sceneView += library.createMainTimeLine().apply { scale = 3.0 }
		}
	}
}
