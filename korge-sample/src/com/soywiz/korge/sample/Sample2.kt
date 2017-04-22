package com.soywiz.korge.sample

import com.soywiz.korge.Korge
import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korge.ext.particle.ParticleEmitter
import com.soywiz.korge.ext.particle.attachParticleAndWait
import com.soywiz.korge.input.onClick
import com.soywiz.korge.resources.Path
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.time.sleep
import com.soywiz.korge.time.waitFrame
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Text
import com.soywiz.korge.view.text
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.go
import com.soywiz.korio.async.sleep
import com.soywiz.korma.random.get
import java.util.*

object Sample2 : Module() {
	@JvmStatic fun main(args: Array<String>) = Korge(Sample2)

	override val mainScene: Class<out Scene> = MainScene::class.java

	class MainScene(
		@Path("font/font.fnt") val font: BitmapFont,
		@Path("particle/particle.pex") val emitter: ParticleEmitter
	) : Scene() {
		val random = Random()

		suspend override fun sceneInit(sceneView: Container) {
			val rect = views.solidRect(100, 100, Colors.RED)
			//val text = views.text("HELLO", color = Colors.RED, font = font)
			val text = views.text("HELLO", color = Colors.RED)
			sceneView += rect
			sceneView += text

			val particles = emitter.create(200.0, 200.0)
			sceneView += particles
			particles.speed = 2.0

			for (n in 0 until 10) {
				go {
					sleep(random[100, 400])
					while (true) {
						sceneView.attachParticleAndWait(
							emitter,
							random[100.0, views.virtualWidth.toDouble()],
							random[100.0, views.virtualHeight.toDouble()],
							time = random[300, 500], speed = random[1.0, 2.0]
						)
						sleep(random[0, 50])
						//println("done!")
					}
				}
			}

			go {
				while (true) {
					//println(views.nativeMouseX)
					particles.emitterPos.x = particles.parent?.localMouseX ?: 0.0
					particles.emitterPos.y = particles.parent?.localMouseY ?: 0.0
					particles.emitting = (views.input.mouseButtons == 0)
					particles.waitFrame()
					//println(":::")
				}
			}

			go {
				particles.waitComplete()
				println("No more particles!")
			}

			//particles.y = views.nativeMouseY

			go {
				class TextHolder(val text: Text) {
					var value: Int
						get() = this.text.text.toIntOrNull() ?: 0
						set(value) {
							this.text.text = "$value"
						}
				}

				val textHolder = TextHolder(text)
				text.tween(textHolder::value..1000, time = 1000)
			}

			rect.onClick {
				println("click!")
			}

			go {
				//rect.tween((SolidRect::color..Colors.BLUE).color(), time = 1000)
				rect.tween(
					(rect::color..Colors.BLUE).color(),
					(rect::x..200.0).delay(200).duration(600),
					rect::y..100.0,
					rect::width..200.0,
					rect::height..90.0,
					rect::rotationDegrees..90.0,
					time = 1000,
					easing = Easings.EASE_IN_OUT_QUAD
				)
				//println(rect.color)
			}
		}
	}
}
