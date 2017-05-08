package com.soywiz.korge.sample

import com.soywiz.korge.Korge
import com.soywiz.korge.animate.AnLibrary
import com.soywiz.korge.animate.play
import com.soywiz.korge.input.onClick
import com.soywiz.korge.resources.Path
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.get
import com.soywiz.korge.view.setText
import com.soywiz.korim.color.RGBA

object SampleMasks : Module() {
	@JvmStatic fun main(args: Array<String>) = Korge(this@SampleMasks, debug = false)

	override val width: Int = 560
	override val height: Int = 380
	override val mainScene: Class<out Scene> = MainScene::class.java

	class MainScene(
		@Path("mask.swf") val lib: AnLibrary
	) : Scene() {
		suspend override fun sceneInit(sceneView: Container) {
			sceneView += lib.createMainTimeLine()
			sceneView["action"].play("drop")
		}
	}
}
