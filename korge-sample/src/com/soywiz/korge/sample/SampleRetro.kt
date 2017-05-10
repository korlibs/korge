package com.soywiz.korge.sample

import com.soywiz.korge.Korge
import com.soywiz.korge.input.onOut
import com.soywiz.korge.input.onOver
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.ScaleView
import com.soywiz.korge.view.scaleView
import com.soywiz.korim.color.Colors

object SampleRetro : Module() {
	@JvmStatic fun main(args: Array<String>) = Korge(this@SampleRetro, debug = false)
	//@JvmStatic fun main(args: Array<String>) = Korge(this@SampleRetro, debug = true)

	override val width: Int = 640
	override val height: Int = 480
	override val mainScene: Class<out Scene> = MainScene::class.java

	class MainScene(
	) : Scene() {
		suspend override fun sceneInit(sceneView: Container) {
			val filterView = views.scaleView(160, 120, scale = 4.0, filtering = false)
			val solidRect = views.solidRect(100, 100, Colors.RED).apply {
				x = 50.0
				y = 50.0
				rotationDegrees = 45.0
				addUpdatable {
					rotationDegrees++
				}

				onOver {
					alpha = 0.5
				}
				onOut {
					alpha = 1.0
				}
			}


			filterView += solidRect
			sceneView += filterView
		}
	}
}
