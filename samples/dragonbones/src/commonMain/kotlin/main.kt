import com.dragonbones.event.*
import com.dragonbones.util.*
import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.*
import com.soywiz.korge.dragonbones.*
import com.soywiz.korge.input.*
import com.soywiz.korge.input.MouseEvents
import com.soywiz.korge.input.mouse
import com.soywiz.korge.render.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.time.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.json.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.interpolation.*
import com.soywiz.korma.random.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.math.*
import kotlin.random.*
import kotlin.reflect.*

suspend fun main(): Unit {
	//Logger.defaultLevel = Logger.Level.TRACE
	//Logger("Views").level = Logger.Level.TRACE
	//Logger("Korge").level = Logger.Level.TRACE
	//Logger("RenderContext").level = Logger.Level.TRACE
	//Logger("BatchBuilder2D").level = Logger.Level.TRACE
	//Logger("DefaultShaders").level = Logger.Level.TRACE
	//Logger("RenderContext2D").level = Logger.Level.TRACE
	//Korge(MyModule, debug = true)
	println("V0")
	println("KorioNative.ResourcesVfs.absolutePath: " + resourcesVfs.absolutePath)

	com.soywiz.korge.Korge(Korge.Config(object : MyModule() {
		//override val mainScene: KClass<out Scene> = HelloScene::class
		override val quality: GameWindow.Quality = GameWindow.Quality.QUALITY
	}, debug = false))

	//Korge {
	//    hello()
	//}
}

suspend fun Stage.hello() {
	println("HelloScene.sceneInit[0]")
	val bmp = resourcesVfs["atlas2/atlas2.png"].readBitmap()
	//val bmp = Bitmap32(100, 100).apply {
	//    context2d {
	//        fillStyle = Context2d.Color(Colors.RED)
	//        fillRoundRect(0.0, 0.0, 100.0, 100.0, 16.0, 16.0)
	//    }
	//}
	//solidRect(100, 100, Colors.RED) {
	image(bmp) {
		position(100, 100)
		alpha(0.5)
		mouse {
			over { alpha(1.0) }
			out { alpha(0.5) }
			click { println("clicked box!") }
		}
		launchImmediately {
			while (true) {
				println("step0")
				tween(this::x[100, 200], time = 1.seconds, easing = Easing.EASE_OUT_ELASTIC)
				println("step1")
				tween(this::x[200, 100], time = 1.seconds, easing = Easing.EASE_OUT_ELASTIC)
				println("step2")
			}
		}
	}
	println("HelloScene.sceneInit[1]")
}

class HelloScene : Scene() {
	override suspend fun Container.sceneInit() {
		println("HelloScene.sceneInit[0]")
		solidRect(100, 100, Colors.RED) {
			position(100, 100)
			alpha(0.5)
			mouse {
				over {
					alpha(1.0)
				}
				out {
					alpha(0.5)
				}
			}
		}
		println("HelloScene.sceneInit[1]")
	}
}

open class MyModule : Module() {
	override val title: String = "KorGE - DragonBones"
	override val mainScene = MyScene::class
	//override val quality: LightQuality = LightQuality.QUALITY
	//override val quality: LightQuality = LightQuality.PERFORMANCE
	//override val quality: LightQuality = LightQuality.AUTO
	override val quality: GameWindow.Quality = GameWindow.Quality.QUALITY

	override suspend fun AsyncInjector.configure() {
		println("init[0]")
		mapPrototype { HelloScene() }
		mapPrototype { MyScene() }
		mapPrototype { ClassicDragonScene() }
		mapPrototype { EyeTrackingScene() }
		mapPrototype { HelloWorldScene() }
		mapPrototype { SkinChangingScene() }
		println("init[1]")
	}

	override val size: SizeInt = SizeInt(1280, 720)
	override val windowSize: SizeInt = SizeInt(1280, 720)
}

abstract class MyBaseScene : Scene() {
}

class MyScene : MyBaseScene() {
	lateinit var buttonContainer: Container

	override suspend fun Container.sceneInit() {
		//addEventListener<MouseEvent> {
		//	println("MouseEvent: ${views.nativeWidth},${views.nativeHeight} :: ${views.virtualWidth},${views.virtualHeight} :: $it")
		//}

		val mySceneContainer = sceneContainer(views) {
			this.x = views.virtualWidth.toDouble() * 0.5
			this.y = views.virtualHeight.toDouble() * 0.5
		}
		buttonContainer = this
		this += Button("Hello") {
			println("Hello")
			mySceneContainer.changeToDisablingButtons<HelloWorldScene>()
		}.position(8, views.virtualHeight - 48)
		//this += Button("Classic") { mySceneContainer.changeToDisablingButtons<ClassicDragonScene>() }.position(108, views.virtualHeight - 48)
		this += Button("Eye Tracking") {
			println("Eye Tracking")
			mySceneContainer.changeToDisablingButtons<EyeTrackingScene>()
		}.position(200, views.virtualHeight - 48)
		this += Button("Skin Changing") {
			println("Skin Changing")
			mySceneContainer.changeToDisablingButtons<SkinChangingScene>()
		}.position(600, views.virtualHeight - 48)
		mySceneContainer.changeToDisablingButtons<HelloWorldScene>()
	}

	suspend inline fun <reified T : Scene> SceneContainer.changeToDisablingButtons() {
		for (child in buttonContainer.children.filterIsInstance<Button>()) {
			//println("DISABLE BUTTON: $child")
			child.enabledButton = false
		}
		try {
			changeTo<T>()
		} finally {
			for (child in buttonContainer.children.filterIsInstance<Button>()) {
				//println("ENABLE BUTTON: $child")
				child.enabledButton = true
			}
		}
	}
}

class Button(text: String, handler: suspend () -> Unit) : Container() {
	val textField = Text(text, textSize = 32.0).apply { filtering = false }
	private val bounds = textField.textBounds
	val g = Graphics().apply {
		fill(Colors.DARKGREY, 0.7) {
			roundRect(bounds.x, bounds.y, bounds.width + 16, bounds.height + 16, 8.0, 8.0)
		}
	}
	var enabledButton = true
		set(value) {
			field = value
			updateState()
		}
	private var overButton = false
		set(value) {
			field = value
			updateState()
		}

	fun updateState() {
		when {
			!enabledButton -> alpha = 0.3
			overButton -> alpha = 1.0
			else -> alpha = 0.8
		}
	}

	init {
		//this += this.solidRect(bounds.width, bounds.height, Colors.TRANSPARENT_BLACK)
		this += g.apply {
			mouseEnabled = true
		}
		this += textField.position(8, 8)

		mouse {
			over { overButton = true }
			out { overButton = false }
		}
		onClick {
			if (enabledButton) handler()
		}
		updateState()
	}
}

class HelloWorldScene : BaseDbScene() {
	val SCALE = 1.6
	override suspend fun Container.sceneInit() {
		val skeDeferred = asyncImmediately { Json.parse(resources["mecha_1002_101d_show/mecha_1002_101d_show_ske.json"].readString())!! }
		//val skeDeferred = asyncImmediately { MemBufferWrap(resources["mecha_1002_101d_show/mecha_1002_101d_show_ske.dbbin"].readBytes()) }
		val texDeferred = asyncImmediately { resources["mecha_1002_101d_show/mecha_1002_101d_show_tex.json"].readString() }
		val imgDeferred = asyncImmediately { resources["mecha_1002_101d_show/mecha_1002_101d_show_tex.png"].readBitmap().mipmaps() }

		val data = factory.parseDragonBonesData(skeDeferred.await())
		val atlas = factory.parseTextureAtlasData(Json.parse(texDeferred.await())!!, imgDeferred.await())

		val armatureDisplay = factory.buildArmatureDisplay("mecha_1002_101d")!!.position(0, 300).scale(SCALE)

		//armatureDisplay.animation.play("walk")
		println(armatureDisplay.animation.animationNames)
		//armatureDisplay.animation.play("jump")
		armatureDisplay.animation.play("idle")
		//scaleView(512, 512) {
		this += armatureDisplay
		//}
	}
}

class ClassicDragonScene : BaseDbScene() {
	override suspend fun Container.sceneInit() {
		//val scale = 0.3
		val scale = 0.8
		val ske = asyncImmediately { resources["Dragon/Dragon_ske.json"].readString() }
		val tex = asyncImmediately { resources["Dragon/Dragon_tex.json"].readString() }
		val img = asyncImmediately { resources["Dragon/Dragon_tex.png"].readBitmap() }

		val data = factory.parseDragonBonesData(Json.parse(ske.await())!!)

		val atlas = factory.parseTextureAtlasData(
			Json.parse(tex.await())!!,
			img.await()
		)
		val armatureDisplay = factory.buildArmatureDisplay("Dragon", "Dragon")!!.position(0, 200).scale(scale)
		armatureDisplay.animation.play("walk")
		println(armatureDisplay.animation.animationNames)
		//armatureDisplay.animation.play("jump")
		//armatureDisplay.animation.play("fall")
		this += armatureDisplay
	}
}

// @TODO: Remove in next KorGE version
val MouseEvents.exit by WeakPropertyThis<MouseEvents, Signal<MouseEvents>> {
	Signal()
}

class EyeTrackingScene : BaseDbScene() {
	val scale = 0.46
	var totalTime = 0.0

	override suspend fun Container.sceneInit() {
		try {
			println("EyeTrackingScene[0]")

			val _animationNames = listOf(
				"PARAM_ANGLE_X", "PARAM_ANGLE_Y", "PARAM_ANGLE_Z",
				"PARAM_EYE_BALL_X", "PARAM_EYE_BALL_Y",
				"PARAM_BODY_X", "PARAM_BODY_Y", "PARAM_BODY_Z",
				"PARAM_BODY_ANGLE_X", "PARAM_BODY_ANGLE_Y", "PARAM_BODY_ANGLE_Z",
				"PARAM_BREATH"
			)

			val skeDeferred = asyncImmediately { resources["shizuku/shizuku_ske.json"].readString() }
			val tex00Deferred = asyncImmediately { resources["shizuku/shizuku.1024/texture_00.png"].readBitmap().mipmaps() }
			val tex01Deferred = asyncImmediately { resources["shizuku/shizuku.1024/texture_01.png"].readBitmap().mipmaps() }
			val tex02Deferred = asyncImmediately { resources["shizuku/shizuku.1024/texture_02.png"].readBitmap().mipmaps() }
			val tex03Deferred = asyncImmediately { resources["shizuku/shizuku.1024/texture_03.png"].readBitmap().mipmaps() }

			println("EyeTrackingScene[1]")

			factory.parseDragonBonesData(
				Json.parse(skeDeferred.await())!!,
				"shizuku"
			)
			println("EyeTrackingScene[2]")
			factory.updateTextureAtlases(
				arrayOf(
					tex00Deferred.await(),
					tex01Deferred.await(),
					tex02Deferred.await(),
					tex03Deferred.await()
				), "shizuku"
			)
			println("EyeTrackingScene[3]")
			val armatureDisplay = factory.buildArmatureDisplay("shizuku", "shizuku")!!
				.position(0, 300).scale(this@EyeTrackingScene.scale)
			this += armatureDisplay

			println(armatureDisplay.animation.animationNames)
			println("EyeTrackingScene[4]")
			//armatureDisplay.play("idle_00")
			armatureDisplay.animation.play("idle_00")

			val target = Point()
			val ftarget = Point()

			mouse {
				moveAnywhere {
					ftarget.x = (localMouseX(views) - armatureDisplay.x) / this@EyeTrackingScene.scale
					ftarget.y = (localMouseY(views) - armatureDisplay.y) / this@EyeTrackingScene.scale
					//println(":" + localMouseXY(views) + ", " + target + " :: ${armatureDisplay.x}, ${armatureDisplay.y} :: ${this@EyeTrackingScene.scale}")
				}
				exit {
					ftarget.x = armatureDisplay.x / this@EyeTrackingScene.scale
					ftarget.y = (armatureDisplay.y - 650) / this@EyeTrackingScene.scale
					//println(":onExit:" + " :: $target :: ${armatureDisplay.x}, ${armatureDisplay.y} :: ${this@EyeTrackingScene.scale}")
				}
			}

			// This job will be automatically destroyed by the SceneContainer
			launchImmediately {
				val bendRatio = 0.75
				val ibendRatio = 1.0 - bendRatio
				while (true) {
					target.x = (target.x * bendRatio + ftarget.x * ibendRatio)
					target.y = (target.y * bendRatio + ftarget.y * ibendRatio)
					delay(16.milliseconds)
				}
			}

			addUpdater {
				totalTime += it.milliseconds

				val armature = armatureDisplay.armature
				val animation = armatureDisplay.animation
				val canvas = armature.armatureData.canvas!!

				var p = 0.0
				val pX = max(min((target.x - canvas.x) / (canvas.width * 0.5), 1.0), -1.0)
				val pY = -max(min((target.y - canvas.y) / (canvas.height * 0.5), 1.0), -1.0)
				for (animationName in _animationNames) {
					if (!animation.hasAnimation(animationName)) {
						continue
					}

					var animationState = animation.getState(animationName, 1)
					if (animationState == null) {
						animationState = animation.fadeIn(animationName, 0.1, 1, 1, animationName)
						if (animationState != null) {
							animationState.resetToPose = false
							animationState.stop()
						}
					}

					if (animationState == null) {
						continue
					}

					when (animationName) {
						"PARAM_ANGLE_X", "PARAM_EYE_BALL_X" -> p = (pX + 1.0) * 0.5
						"PARAM_ANGLE_Y", "PARAM_EYE_BALL_Y" -> p = (pY + 1.0) * 0.5
						"PARAM_ANGLE_Z" -> p = (-pX * pY + 1.0) * 0.5
						"PARAM_BODY_X", "PARAM_BODY_ANGLE_X" -> p = (pX + 1.0) * 0.5
						"PARAM_BODY_Y", "PARAM_BODY_ANGLE_Y" -> p = (-pX * pY + 1.0) * 0.5
						"PARAM_BODY_Z", "PARAM_BODY_ANGLE_Z" -> p = (-pX * pY + 1.0) * 0.5
						"PARAM_BREATH" -> p = (sin(totalTime / 1000.0) + 1.0) * 0.5
					}

					animationState.currentTime = p * animationState.totalTime
				}
			}
		} catch (e: Throwable) {
			e.printStackTrace()
		}
	}
}

class SkinChangingScene : BaseDbScene() {
	val SCALE = 0.42
	val random = Random(0)

	override suspend fun Container.sceneInit() {
		val suitConfigs = listOf(
			listOf(
				"2010600a", "2010600a_1",
				"20208003", "20208003_1", "20208003_2", "20208003_3",
				"20405006",
				"20509005",
				"20703016", "20703016_1",
				"2080100c",
				"2080100e", "2080100e_1",
				"20803005",
				"2080500b", "2080500b_1"
			),
			listOf(
				"20106010",
				"20106010_1",
				"20208006",
				"20208006_1",
				"20208006_2",
				"20208006_3",
				"2040600b",
				"2040600b_1",
				"20509007",
				"20703020",
				"20703020_1",
				"2080b003",
				"20801015"
			)
		)

		val deferreds = arrayListOf<Deferred<*>>()

		deferreds += asyncImmediately {
			factory.parseDragonBonesData(
				Json.parse(resources["you_xin/body/body_ske.json"].readString())!!
			)
		}
		deferreds += asyncImmediately {
			val atlas = factory.parseTextureAtlasData(
				Json.parse(resources["you_xin/body/body_tex.json"].readString())!!,
				resources["you_xin/body/body_tex.png"].readBitmap().mipmaps()
			)
		}

		for ((i, suitConfig) in suitConfigs.withIndex()) {
			for (partArmatureName in suitConfig) {
				// resource/you_xin/suit1/2010600a/xxxxxx
				val path = "you_xin/" + "suit" + (i + 1) + "/" + partArmatureName + "/" + partArmatureName
				val dragonBonesJSONPath = path + "_ske.json"
				val textureAtlasJSONPath = path + "_tex.json"
				val textureAtlasPath = path + "_tex.png"
				//
				deferreds += asyncImmediately {
					factory.parseDragonBonesData(Json.parse(resources[dragonBonesJSONPath].readString())!!)
					factory.parseTextureAtlasData(
						Json.parse(resources[textureAtlasJSONPath].readString())!!,
						resources[textureAtlasPath].readBitmap().mipmaps()
					)
				}
			}
		}

		deferreds.awaitAll()

		val armatureDisplay = factory.buildArmatureDisplay("body")!!
			.position(0, 360).scale(SCALE)
		this += armatureDisplay

		println(armatureDisplay.animation.animationNames)
		//armatureDisplay.animation.play("idle_00")
		armatureDisplay.on(EventObject.LOOP_COMPLETE) {
			//println("LOOP!")
			// Random animation index.
			val nextAnimationName = random[armatureDisplay.animation.animationNames]
			armatureDisplay.animation.fadeIn(nextAnimationName, 0.3, 0)
		}
		armatureDisplay.animation.play("idle", 0)
		//armatureDisplay.animation.play("speak")

		for (part in suitConfigs[0]) {
			val partArmatureData = factory.getArmatureData(part)
			factory.replaceSkin(armatureDisplay.armature, partArmatureData!!.defaultSkin!!)
		}
		val _replaceSuitParts = arrayListOf<String>()
		var _replaceSuitIndex = 0

		mouse {
			onUpAnywhere {
				// This suit has been replaced, next suit.
				if (_replaceSuitParts.size == 0) {
					_replaceSuitIndex++

					if (_replaceSuitIndex >= suitConfigs.size) {
						_replaceSuitIndex = 0
					}

					// Refill the unset parits.
					for (partArmatureName in suitConfigs[_replaceSuitIndex]) {
						_replaceSuitParts.add(partArmatureName)
					}
				}

				// Random one part in this suit.
				val partIndex: Int = floor(random.nextDouble() * _replaceSuitParts.size).toInt()
				val partArmatureName = _replaceSuitParts[partIndex]
				val partArmatureData = factory.getArmatureData(partArmatureName)
				// Replace skin.
				factory.replaceSkin(armatureDisplay.armature, partArmatureData!!.defaultSkin!!)
				// Remove has been replaced
				_replaceSuitParts.splice(partIndex, 1)
			}
		}
	}
}

abstract class BaseDbScene : MyBaseScene() {
	val resources get() = resourcesVfs
	val factory = KorgeDbFactory()
}
