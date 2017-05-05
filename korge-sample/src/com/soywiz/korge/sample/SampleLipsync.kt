package com.soywiz.korge.sample

import com.soywiz.korge.Korge
import com.soywiz.korge.animate.AnLibrary
import com.soywiz.korge.event.addEventListener
import com.soywiz.korge.ext.lipsync.LipSyncEvent
import com.soywiz.korge.ext.lipsync.Voice
import com.soywiz.korge.ext.lipsync.lipSync
import com.soywiz.korge.ext.lipsync.readVoice
import com.soywiz.korge.resources.Path
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tween.color
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.Container
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.go

object SampleLipsync : Module() {
	@JvmStatic fun main(args: Array<String>) = Korge(Module(), sceneClass = MainScene::class.java)

	override val mainScene: Class<out Scene> = MainScene::class.java

	class MainScene(
		@Path("lipsync/lipsynctest.swf") val lipsynctest: AnLibrary,
		@Path("lipsync/to-be.voice.mp3") val myvoice: Voice
	) : Scene() {
		suspend override fun sceneInit(sceneView: Container) {
			go {
				val voice = resourcesRoot["lipsync/to-be.voice.mp3"].readVoice(views)
				//voice.play("Carlos")
				myvoice.play("Tamara")
			}

			sceneView += lipsynctest.createMainTimeLine()

			go {
				//sceneView.tween(sceneView::colorMul[Colors.WHITE, Colors.RED].color(), time = 1000)
			}
		}
	}
}
