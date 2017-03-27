package com.soywiz.korge.sample

import com.soywiz.korfl.SwfLibrary
import com.soywiz.korge.Korge
import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korge.bitmapfont.FontDescriptor
import com.soywiz.korge.component.Component
import com.soywiz.korge.input.component.onOut
import com.soywiz.korge.input.component.onOver
import com.soywiz.korge.render.Texture
import com.soywiz.korge.resources.Path
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tween.Easing
import com.soywiz.korge.tween.rangeTo
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.*
import com.soywiz.korge.view.tiles.TileSet
import com.soywiz.korge.view.tiles.tileMap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.geom.Point2d
import com.soywiz.korio.async.go

object Sample1 {
	@JvmStatic fun main(args: Array<String>) = Korge(Sample1Module, args)
}

object Sample1Module : Module() {
	override val title = "Sample1"
	override val icon = "kotlin8.png"
	override var mainScene = Sample1Scene::class.java
}

class MouseSampleController(view: View) : Component(view) {
	val temp = Point2d()
	override fun update(dtMs: Int) {
		//view.globalToLocal(views.input.mouse, temp)
		view.x = views.input.mouse.x
		view.y = views.input.mouse.y
		view.rotationDegrees = (view.rotationDegrees + 1) % 360
	}
}

fun View.mouseSampleController() = this.apply { MouseSampleController(this).attach() }

class Sample1Scene(
	@Path("korge.png") val korgeTex: Texture,
	@Path("simple.swf") val swfLibrary: SwfLibrary,
	@Path("tiles.png") val tilesetTex: Texture,
	@Path("font/font.fnt") val font: BitmapFont,
	@FontDescriptor(face = "Arial", size = 40) val font2: BitmapFont
) : Scene() {
	suspend override fun init() {
		super.init()

		val tileset = TileSet(tilesetTex, 32, 32)

		root.container() {
			//this.text(font, "hello")
			this.tileMap(Bitmap32(8, 8), tileset) {
				this.x = -128.0
				this.y = -128.0
				alpha = 0.8
			}
		}.mouseSampleController()

		root.container {
			val mc = swfLibrary.an.createMainTimeLine().apply {
				//speed = 0.1
			}
			this += mc
			//mc.addUpdatable { println(mc.dumpToString()) }
		}

		val image = root.image(korgeTex, 0.5).apply {
			scale = 0.2
			rotation = Math.toRadians(-90.0)
			alpha = 0.7
			//smoothing = false
			onOver { alpha = 1.0 }
			onOut { alpha = 0.7 }
			//onDown { scale = 0.3 }
			//onUp { scale = 0.2 }
		}

		val tilemap = root.tileMap(Bitmap32(8, 8), tileset) {
			alpha = 0.8
		}

		root.text(font, "Hello world! F,", textSize = 72.0).apply {
			x = 100.0
			y = 100.0
		}

		root.text(font2, "2017", textSize = 40.0).apply {
			x = 0.0
			y = 0.0
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
