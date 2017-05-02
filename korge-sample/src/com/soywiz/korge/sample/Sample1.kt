package com.soywiz.korge.sample

import com.soywiz.korge.Korge
import com.soywiz.korge.animate.AnLibrary
import com.soywiz.korge.animate.AnTextField
import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korge.bitmapfont.FontDescriptor
import com.soywiz.korge.component.Component
import com.soywiz.korge.component.docking.dockedTo
import com.soywiz.korge.ext.spriter.SpriterLibrary
import com.soywiz.korge.input.*
import com.soywiz.korge.render.Texture
import com.soywiz.korge.resources.Path
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.time.timers
import com.soywiz.korge.tween.Easing
import com.soywiz.korge.tween.Easings
import com.soywiz.korge.tween.rangeTo
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.*
import com.soywiz.korge.view.tiles.TileSet
import com.soywiz.korge.view.tiles.tileMap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korio.async.async
import com.soywiz.korio.async.go
import com.soywiz.korio.async.sleep
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.util.clamp
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korma.geom.Anchor

object Sample1 {
	@JvmStatic fun main(args: Array<String>) = Korge(Sample1Module, args, sceneClass = Sample1Scene::class.java)
	//@JvmStatic fun main(args: Array<String>) = Korge(Sample1Module, args, sceneClass = Sample2Scene::class.java)
}

object Sample1Module : Module() {
	override val title = "Sample1"
	override val icon = "kotlin8.png"
	override var mainScene = Sample1Scene::class.java
	//override var mainScene = Sample2Scene::class.java

	suspend override fun init(injector: AsyncInjector) {
		injector.get<ResourcesRoot>().mount("/", ResourcesVfs)
		injector.get<Views>().registerPropertyTriggerSuspend("gravity") { view, key, value ->
			val gravity = value.toDouble()
			go {
				var speed = 0.0
				val stepMs = 16
				while (true) {
					speed += (gravity / 1000.0) * stepMs
					view.y += speed
					view.timers.waitMilliseconds(stepMs)
				}
			}
			//println(child)
		}
	}
}

class MouseSampleController(view: View) : Component(view) {
	override fun update(dtMs: Int) {
		//view.globalToLocal(views.input.mouse, temp)
		view.x = view.parent?.localMouseX ?: 0.0
		view.y = view.parent?.localMouseY ?: 0.0
		view.rotationDegrees = (view.rotationDegrees + 1) % 360
	}
}

fun View.mouseSampleController() = this.apply { MouseSampleController(this).attach() }

class Sample2Scene(
	@Path("test4.swf") val test4Library: AnLibrary
) : Scene() {
	suspend override fun sceneInit(sceneView: Container) {
		super.init()

		this.sceneView += test4Library.createMainTimeLine()
	}
}

class JellyButton(val view: View) {
	val initialScale = view.scale

	//val thread = AsyncThread()

	init {
		view.onOver { view.views.eventLoop.async { view.tween(view::scale..initialScale * 1.5, time = 200, easing = Easings.EASE_OUT_ELASTIC) } }
		view.onOut { view.views.eventLoop.async { view.tween(view::scale..initialScale, time = 400, easing = Easings.EASE_OUT_ELASTIC) } }
	}

	fun onClick(callback: suspend () -> Unit) {
		view.onClick { view.views.eventLoop.async { callback() } }
	}
}

class Sample1Scene(
	@Path("korge.png") val korgeTex: Texture,
	@Path("simple.swf") val swfLibrary: AnLibrary,
	@Path("test1.swf") val test1Library: AnLibrary,
	@Path("test4.swf") val test4Library: AnLibrary,
	@Path("as3test.swf") val as3testLibrary: AnLibrary,
	@Path("soundtest.swf") val soundtestLibrary: AnLibrary,
	@Path("progressbar.swf") val progressbarLibrary: AnLibrary,
	@Path("buttons.swf") val buttonsLibrary: AnLibrary,
	@Path("props.swf") val propsLibrary: AnLibrary,
	@Path("tiles.png") val tilesetTex: Texture,
	@Path("font/font.fnt") val font: BitmapFont,
	@Path("spriter-sample1/demo.scml") val demoSpriterLibrary: SpriterLibrary,
	@FontDescriptor(face = "Arial", size = 40) val font2: BitmapFont
) : Scene() {
	lateinit var tileset: TileSet
	lateinit var image: Image
	lateinit var percent: AnTextField

	suspend override fun sceneInit(sceneView: Container) {
		tileset = TileSet(views, tilesetTex, 32, 32)

		sceneView.container {
			//this.text(font, "hello")
			this.tileMap(Bitmap32(8, 8), tileset) {
				//blendMode = BlendMode.ADD
				this.x = -128.0
				this.y = -128.0
				alpha = 0.8
			}
		}.mouseSampleController()

		sceneView.container {
			this += swfLibrary.createMainTimeLine()
		}


		image = sceneView.image(korgeTex, 0.5).apply {
			scale = 0.2
			rotation = Math.toRadians(-90.0)
			alpha = 0.7
			//smoothing = false
			mouse.hitTestType = View.HitTestType.SHAPE
			onOver { alpha = 1.0 }
			onOut { alpha = 0.7 }
			//onDown { scale = 0.3 }
			//onUp { scale = 0.2 }
		}

		val tilemap = sceneView.tileMap(Bitmap32(8, 8), tileset) {
			alpha = 0.8
		}


		sceneView.container {
			val mc = test1Library.createMainTimeLine().apply {
				//speed = 0.1
			}
			this += mc
			//mc.addUpdatable { println(mc.dumpToString()) }
		}

		sceneView.container {
			//JekllyButton()
			val mc = test4Library.createMainTimeLine().apply {
				x = 320.0
				y = 320.0
				//speed = 0.1
			}
			this += mc
			//mc.addUpdatable { println(mc.dumpToString()) }
		}

		sceneView.container {
			val mc = as3testLibrary.createMainTimeLine().apply {
				//x = 320.0
				//y = 320.0
				//speed = 0.1
			}
			this += mc
			//mc.addUpdatable { println(mc.dumpToString()) }
		}

		sceneView.container {
			val mc = soundtestLibrary.createMainTimeLine().apply {
				//x = 320.0
				//y = 320.0
				//speed = 0.1
			}
			this += mc
			//mc.addUpdatable { println(mc.dumpToString()) }
		}

		sceneView += progressbarLibrary.createMainTimeLine().apply {
			this@apply.dockedTo(Anchor.TOP_LEFT)
			go {
				percent = (this@apply["percent"] as AnTextField?)!!
				percent.onClick {
					percent.alpha = 0.5
					println(percent.alpha)
				}
				sceneView.tween(time = 2000, easing = Easing.EASE_IN_OUT_QUAD) { ratio ->
					this@apply.seekStill("progressbar", ratio)
					//println(this.findFirstWithName("percent"))
					percent.setText("%d%%".format((ratio * 100).toInt()))
				}
			}
			Unit
		}

		sceneView.text("Hello world! F,", textSize = 72.0, font = font).apply {
			blendMode = BlendMode.ADD
			x = 100.0
			y = 100.0
		}

		sceneView.text("2017", textSize = 40.0, font = font2).apply {
			x = 0.0
			y = 0.0
		}

		go {
			image.tween(
				image::x..200.0, image::y..200.0,
				image::rotation..Math.toRadians(0.0), image::scale..2.0,
				time = 2000, easing = Easing.EASE_IN_OUT_QUAD
			)
			for (delta in listOf(+200.0, -200.0, +100.0)) {
				image.tween(image::x..image.x + delta, time = 1000, easing = Easing.EASE_IN_OUT_QUAD)
			}
			//views.dump()
		}

		val player = demoSpriterLibrary.create("Player", "idle").apply {
			//val player = demoSpriterLibrary.create("Player", "hurt_idle").apply {
			x = 400.0
			y = 200.0
			scale = 0.7
		}.moveWithKeys()

		go {
			player.tween(
				player::rotationDegrees..360.0,
				player::scale..1.0,
				time = 1000, easing = Easing.EASE_IN_OUT_QUAD
			)
			player.changeTo("hurt_idle", time = 300, easing = Easing.EASE_IN)
			sleep(400)
			player.changeTo("walk", time = 1000, easing = Easing.LINEAR)
			sleep(400)
			player.changeTo("sword_swing_0", time = 1000, easing = Easing.LINEAR)
			sleep(500)
			player.changeTo("throw_axe", time = 500, easing = Easing.LINEAR)
			player.waitCompleted()
			println("completed")
			//player.speed = 0.1
			player.tween(player::speed..0.3, time = 2000)

			//println("${player.animation1}:${player.animation2}:${player.prominentAnimation}")
		}
		//sceneView += ShaderView(views).apply { this += player }
		sceneView += player

		sceneView.container {
			val mc = buttonsLibrary.createMainTimeLine()
			mc.scale = 0.3
			mc.setXY(400, 300)
			//this += ShaderView(views).apply { this += mc }
			this += mc
			for (n in 1..4) {
				for (m in 1..4) {
					val buttonView = mc["buttonsRow$n"]?.get("button$m")
					if (buttonView != null) {
						JellyButton(buttonView)
					}
				}
			}
		}

		sceneView.addUpdatable {
			if (views.input.keysJustPressed[Keys.UP]) println("Just pressed UP!")
			if (views.input.keysJustReleased[Keys.UP]) println("Just released UP!")
			if (views.input.keys[Keys.UP]) println("Pressing UP!")
			//if (views.input.keysJustPressed[Keys.UP]) println("Just pressed UP!")
		}

		sceneView += propsLibrary.createMainTimeLine()
	}
}

class MoveWithKeysComponent(view: View) : Component(view) {
	override fun update(dtMs: Int) {
		val upTime = (views.input.keysPressingTime[Keys.UP] / 20).clamp(0, 5)
		val downTime = (views.input.keysPressingTime[Keys.DOWN] / 20).clamp(0, 5)
		val leftTime = (views.input.keysPressingTime[Keys.LEFT] / 20).clamp(0, 5)
		val rightTime = (views.input.keysPressingTime[Keys.RIGHT] / 20).clamp(0, 5)
		view.y -= upTime
		view.y += downTime
		view.x -= leftTime
		view.x += rightTime
	}
}

fun <T : View> T.moveWithKeys(): T = this.apply { addComponent(MoveWithKeysComponent(this)) }
