package com.soywiz.korge.sample

import com.soywiz.korge.Korge
import com.soywiz.korge.animate.AnLibrary
import com.soywiz.korge.resources.Path
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korim.color.RGBA

object Sample4 : Module() {
	@JvmStatic fun main(args: Array<String>) = Korge(this@Sample4)

	override val width: Int = 560
	override val height: Int = 380
	override val mainScene: Class<out Scene> = MainScene::class.java

	class MainScene(
		@Path("texts.swf") val lib: AnLibrary
	) : Scene() {
		suspend override fun sceneInit(sceneView: Container) {
			sceneView += lib.createMainTimeLine()
		}
	}
}
