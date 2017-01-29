package com.soywiz.korge.sample

import com.soywiz.korge.Korge
import com.soywiz.korge.TextureResource
import com.soywiz.korge.resources.Path
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.View
import com.soywiz.korio.async.go
import com.soywiz.korio.async.sleep
import com.soywiz.korio.async.sleepNextFrame
import com.soywiz.korio.async.tween.Easing
import com.soywiz.korio.async.tween.rangeTo
import com.soywiz.korio.async.tween.tween
import com.soywiz.korio.inject.AsyncInjector

object Sample1 {
	@JvmStatic fun main(args: Array<String>) = Korge(Sample1Module, args)
}

object Sample1Module : Module() {
	override var mainScene = Sample1Scene::class.java
}

class Sample1Scene(
	@Path("korge.png") val korgeTex: TextureResource,
	injector: AsyncInjector
) : Scene(injector) {
	suspend override fun init() {
		super.init()
		val image = views.image(korgeTex.tex, 0.5).apply {
			scale = 0.2
			rotation = Math.toRadians(-90.0)
			//smoothing = false
		}
		root += image

		go {
			while (true) {
				image.alpha = if (image.hitTest(views.mouse) != null) 1.0 else 0.7
				sleepNextFrame()
			}
		}

		go {
			image.tween(
				View::x..200.0, View::y..200.0,
				View::rotation..Math.toRadians(0.0), View::scale..2.0,
				time = 2000, easing = Easing.EASE_IN_OUT_QUAD
			)
			for (delta in listOf(+200.0, -200.0, +100.0)) {
				image.tween(View::x..image.x + delta, time = 1000, easing = Easing.EASE_IN_OUT_QUAD)
			}
			views.dump()
		}
	}
}