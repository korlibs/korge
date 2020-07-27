import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.render.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korge3d.*
import com.soywiz.korge3d.animation.*
import com.soywiz.korge3d.format.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*

//suspend fun main() = Demo3.main(args)
suspend fun main() = Korge(Korge.Config(module = Korge3DSampleModule()))

@Korge3DExperimental
class Korge3DSampleModule : KorgeModule(RootScene::class) {
	override val size: SizeInt = SizeInt(1280, 720)
	override val title: String = "KorGE 3D"
	override val bgcolor: RGBA = RGBA.float(.25f, .25f, .25f, 1f)

	override suspend fun AsyncInjector.configure() {
		mapPrototype { RootScene() }
		mapPrototype { CratesScene() }
		mapPrototype { MonkeyScene() }
		mapPrototype { SkinningScene() }
	}
}

class RootScene : Scene() {
	lateinit var contentSceneContainer: SceneContainer

	override suspend fun Container.sceneInit() {
		contentSceneContainer = sceneContainer(views)

		sceneButton<CratesScene>("Crates", 0)
		sceneButton<MonkeyScene>("Monkey", 1)
		sceneButton<SkinningScene>("Skinning", 2)

		contentSceneContainer.changeToDisablingButtons<CratesScene>(this)
		//contentSceneContainer.changeToDisablingButtons<SkinningScene>(this)
	}

	inline fun <reified T : Scene> Container.sceneButton(title: String, x: Int) {
		this += Button(title) { contentSceneContainer.changeToDisablingButtons<T>(this) }
			.position(8 + x * 200, views.virtualHeight - 48)
	}
}

@Korge3DExperimental
class CratesScene : Scene() {
	override suspend fun Container.sceneInit() {
		val korgeTex = resourcesVfs["korge.png"].readNativeImage().mipmaps(false)
		val crateTex = resourcesVfs["crate.jpg"].readNativeImage().mipmaps(true)
		val crateMaterial = Material3D(diffuse = Material3D.LightTexture(crateTex))

		image(korgeTex).alpha(0.5)

		scene3D {
			//camera.set(fov = 60.degrees, near = 0.3, far = 1000.0)

			light().position(0, 0, -3)

			val cube1 = box().material(crateMaterial)
			val cube2 = box().position(0, 2, 0).scale(1, 2, 1).rotation(0.degrees, 0.degrees, 45.degrees).material(crateMaterial)
			val cube3 = box().position(-5, 0, 0).material(crateMaterial)
			val cube4 = box().position(+5, 0, 0).material(crateMaterial)
			val cube5 = box().position(0, -5, 0).material(crateMaterial)
			val cube6 = box().position(0, +5, 0).material(crateMaterial)
			val cube7 = box().position(0, 0, -5).material(crateMaterial)
			val cube8 = box().position(0, 0, +5).material(crateMaterial)

			var tick = 0
			addUpdatable {
				val angle = (tick / 4.0).degrees
				camera.positionLookingAt(
					cos(angle * 2) * 4, cos(angle * 3) * 4, -sin(angle) * 4, // Orbiting camera
					0, 1, 0
				)
				tick++
			}

			launchImmediately {
				while (true) {
					tween(time = 16.seconds) {
						cube1.modelMat.identity().rotate((it * 360).degrees, 0.degrees, 0.degrees)
						cube2.modelMat.identity().rotate(0.degrees, (it * 360).degrees, 0.degrees)
					}
				}
			}
		}

		image(korgeTex).position(views.virtualWidth, 0).anchor(1, 0).alpha(0.5)
	}
}

@Korge3DExperimental
class MonkeyScene : Scene() {
	override suspend fun Container.sceneInit() {
		//delay(10.seconds)
		//println("delay")
		scene3D {
			val light1 = light().position(0, 10, +10).setTo(Colors.RED)
			val light2 = light().position(10, 0, +10).setTo(Colors.BLUE)

			launchImmediately {
				while (true) {
					tween(light1::y[-20], light2::x[-20], time = 1.seconds, easing = Easing.SMOOTH)
					tween(light1::y[+20], light2::x[+20], time = 1.seconds, easing = Easing.SMOOTH)
				}
			}

			val library = resourcesVfs["monkey-smooth.dae"].readColladaLibrary()
			val model = library.geometryDefs.values.first()
			val view = mesh(model.mesh).rotation(-90.degrees, 0.degrees, 0.degrees)

			var tick = 0
			addUpdatable {
				val angle = (tick / 1.0).degrees
				camera.positionLookingAt(
					cos(angle * 1) * 4, 0.0, -sin(angle * 1) * 4, // Orbiting camera
					0, 0, 0
				)
				tick++
			}
		}
	}

}

@Korge3DExperimental
class SkinningScene : Scene() {
	override suspend fun Container.sceneInit() {
		scene3D {
			//val library = resourcesVfs["model.dae"].readColladaLibrary()
			//val library = resourcesVfs["ball.dae"].readColladaLibrary()
			val library = resourcesVfs["skinning.dae"].readColladaLibrary()
			//val library = resourcesVfs["model_skinned_animated.dae"].readColladaLibrary()
			//val library = resourcesVfs["Fallera.dae"].readColladaLibrary()

			val mainSceneView = library.mainScene.instantiate()
			val cameras = mainSceneView.findByType<Camera3D>()

			val animator = Animator3D(library.animationDefs.values, mainSceneView)
			addUpdatable { animator.update(it) }
			val model = mainSceneView.findByType<ViewWithMesh3D>().first()
				//.rotation(-90.degrees, 90.degrees, 0.degrees)

			val camera1 = cameras.firstOrNull() ?: camera
			val camera2 = cameras.lastOrNull() ?: camera

			camera = camera1.clone()

			this += mainSceneView
			addUpdatable {
				//val mainSceneView = mainSceneView
				//println(mainSceneView)

				//println("Camera: ${camera.transform}")
				//println("Model: ${model.transform}")
				//println("Skeleton: ${model.skeleton}")
			}
		}
	}

}
