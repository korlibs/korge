package com.soywiz.korge.sample

import com.soywiz.korge.Korge
import com.soywiz.korge.input.onClick
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tween.Easings
import com.soywiz.korge.tween.color
import com.soywiz.korge.tween.rangeTo
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.SolidRect
import com.soywiz.korge.view.Text
import com.soywiz.korge.view.text
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.go

object Sample2 : Module() {
	@JvmStatic fun main(args: Array<String>) = Korge(Sample2)

	override val mainScene: Class<out Scene> = MainScene::class.java

	class MainScene : Scene() {
		suspend override fun sceneInit(sceneView: Container) {
			val rect = views.solidRect(100, 100, Colors.RED)
			val text = views.text("HELLO", color = Colors.RED)
			sceneView += rect
			sceneView += text

			go {
				class TextHolder(val text: Text) {
					var value: Int
						get() = this.text.text.toIntOrNull() ?: 0
						set(value) {
							this.text.text = "$value"
						}
				}

				val textHolder = TextHolder(text)
				text.tween(textHolder, TextHolder::value..1000, time = 1000)
			}

			rect.onClick {
				println("click!")
			}

			go {
				//rect.tween((SolidRect::color..Colors.BLUE).color(), time = 1000)
				rect.tween(
					(SolidRect::color..Colors.BLUE).color(),
					SolidRect::x..200.0,
					SolidRect::y..100.0,
					SolidRect::width..200.0,
					SolidRect::height..90.0,
					SolidRect::rotationDegrees..90.0,
					time = 1000,
					easing = Easings.EASE_IN_OUT_QUAD
				)
				//println(rect.color)
			}
		}
	}
}
