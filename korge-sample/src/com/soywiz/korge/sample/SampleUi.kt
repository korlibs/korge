package com.soywiz.korge.sample

import com.soywiz.korge.Korge
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.UIFactory
import com.soywiz.korge.ui.button
import com.soywiz.korge.view.Container
import com.soywiz.korim.color.RGBA

object SampleUi : Module() {
	@JvmStatic fun main(args: Array<String>) = Korge(this@SampleUi)

	override val width: Int = 560
	override val height: Int = 380

	//override val bgcolor: Int = Colors.WHITE
	override val bgcolor: Int = RGBA(0x70, 0x70, 0x70, 0xFF)
	override val mainScene: Class<out Scene> = MainScene::class.java

	class MainScene(
		val ui: UIFactory
	) : Scene() {
		suspend override fun sceneInit(sceneView: Container) {
			sceneView += ui.button().apply {
				width = 400.0
				height = 64.0
			}
		}
	}
}
