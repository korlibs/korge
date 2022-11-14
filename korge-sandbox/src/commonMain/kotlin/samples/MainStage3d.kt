package samples

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kds.values
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.seconds
import com.soywiz.korge.input.mouse
import com.soywiz.korge.input.onClick
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.scene.SceneContainer
import com.soywiz.korge.scene.sceneContainer
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korge3d.Camera3D
import com.soywiz.korge3d.Korge3DExperimental
import com.soywiz.korge3d.Material3D
import com.soywiz.korge3d.Stage3D
import com.soywiz.korge3d.View3D
import com.soywiz.korge3d.ViewWithMesh3D
import com.soywiz.korge3d.animation.Animator3D
import com.soywiz.korge3d.cube
import com.soywiz.korge3d.findByType
import com.soywiz.korge3d.format.readColladaLibrary
import com.soywiz.korge3d.instantiate
import com.soywiz.korge3d.light
import com.soywiz.korge3d.material
import com.soywiz.korge3d.mesh
import com.soywiz.korge3d.position
import com.soywiz.korge3d.positionLookingAt
import com.soywiz.korge3d.rotation
import com.soywiz.korge3d.scale
import com.soywiz.korge3d.scene3D
import com.soywiz.korim.bitmap.mipmaps
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readNativeImage
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.cos
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.rotate
import com.soywiz.korma.geom.sin
import com.soywiz.korma.geom.times
import com.soywiz.korma.geom.unaryMinus
import com.soywiz.korma.geom.vector.roundRect
import com.soywiz.korma.interpolation.Easing

class MainStage3d : Scene() {
    lateinit var contentSceneContainer: SceneContainer

    override suspend fun SContainer.sceneInit() {
        views.injector
            .mapPrototype { MainStage3d() }
            .mapPrototype { CratesScene() }
            .mapPrototype { MonkeyScene() }
            .mapPrototype { SkinningScene() }

        contentSceneContainer = sceneContainer(views)

        sceneButton<CratesScene>("Crates", 0)
        sceneButton<MonkeyScene>("Monkey", 1)
        sceneButton<SkinningScene>("Skinning", 2)

        contentSceneContainer.changeToDisablingButtons<CratesScene>(this)
        //contentSceneContainer.changeToDisablingButtons<SkinningScene>(this)
    }

    inline fun <reified T : Scene> Container.sceneButton(title: String, x: Int) {
        uiButton(title)
            .xy(8 + x * 200, views.virtualHeight - 120)
            .onClick { contentSceneContainer.changeToDisablingButtons<T>(this) }

        //this += Button(title) { contentSceneContainer.changeToDisablingButtons<T>(this) }
        //    .position(8 + x * 200, views.virtualHeight - 120)
    }

    @Korge3DExperimental
    class CratesScene : Scene() {
        override suspend fun SContainer.sceneInit() {
            val korgeTex = resourcesVfs["korge.png"].readNativeImage().mipmaps(false)
            val crateTex = resourcesVfs["crate.jpg"].readNativeImage().mipmaps(true)
            val crateMaterial = Material3D(diffuse = Material3D.LightTexture(crateTex))

            image(korgeTex).alpha(0.5)

            scene3D {
                //camera.set(fov = 60.degrees, near = 0.3, far = 1000.0)

                light().position(0, 0, -3)

                val cube1 = cube().material(crateMaterial)
                val cube2 = cube().position(0, 2, 0).scale(1, 2, 1).rotation(0.degrees, 0.degrees, 45.degrees).material(crateMaterial)
                val cube3 = cube().position(-5, 0, 0).material(crateMaterial)
                val cube4 = cube().position(+5, 0, 0).material(crateMaterial)
                val cube5 = cube().position(0, -5, 0).material(crateMaterial)
                val cube6 = cube().position(0, +5, 0).material(crateMaterial)
                val cube7 = cube().position(0, 0, -5).material(crateMaterial)
                val cube8 = cube().position(0, 0, +5).material(crateMaterial)

                var tick = 0
                addUpdater {
                    val angle = (tick / 4.0).degrees
                    camera.positionLookingAt(
                        cos(angle * 2) * 4, cos(angle * 3) * 4, -sin(angle) * 4, // Orbiting camera
                        0.0, 1.0, 0.0
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
        override suspend fun SContainer.sceneInit() {
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
                addUpdater {
                    val angle = (tick / 1.0).degrees
                    camera.positionLookingAt(
                        cos(angle * 1) * 4, 0.0, -sin(angle * 1) * 4, // Orbiting camera
                        0.0, 0.0, 0.0
                    )
                    tick++
                }
            }
        }

    }

    @Korge3DExperimental
    class SkinningScene : Scene() {
        override suspend fun SContainer.sceneInit() {
            scene3D {
                //val library = resourcesVfs["model.dae"].readColladaLibrary()
                //val library = resourcesVfs["ball.dae"].readColladaLibrary()
                val library = resourcesVfs["skinning.dae"].readColladaLibrary()
                //val library = resourcesVfs["model_skinned_animated.dae"].readColladaLibrary()
                //val library = resourcesVfs["Fallera.dae"].readColladaLibrary()

                val mainSceneView = library.mainScene.instantiate()
                val cameras = mainSceneView.findByType<Camera3D>()

                val animators = library.animationDefs.values.map { Animator3D(it, mainSceneView) }
                addUpdater { animators.fastForEach { animator -> animator.update(it) } }
                val model = mainSceneView.findByType<ViewWithMesh3D>().first()
                //.rotation(-90.degrees, 90.degrees, 0.degrees)

                val camera1 = cameras.firstOrNull() ?: camera
                val camera2 = cameras.lastOrNull() ?: camera

                camera = camera1.clone()

                this += mainSceneView
                addUpdater {
                    //val mainSceneView = mainSceneView
                    //println(mainSceneView)

                    //println("Camera: ${camera.transform}")
                    //println("Model: ${model.transform}")
                    //println("Skeleton: ${model.skeleton}")
                }
            }
        }

    }


    @Korge3DExperimental
    private suspend fun Stage3D.orbit(v: View3D, distance: Double, time: TimeSpan) {
        view.tween(time = time) { ratio ->
            val angle = 360.degrees * ratio
            camera.positionLookingAt(
                cos(angle) * distance, 0.0, sin(angle) * distance, // Orbiting camera
                v.transform.translation.x.toDouble(), v.transform.translation.y.toDouble(), v.transform.translation.z.toDouble()
            )
        }
    }

    /*
    class Button(text: String, handler: suspend () -> Unit) : Container() {
        val textField = Text(text, textSize = 32.0).apply { smoothing = false }
        private val bounds = textField.textBounds
        val g = CpuGraphics().updateShape {
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
    */

    suspend inline fun <reified T : Scene> SceneContainer.changeToDisablingButtons(buttonContainer: Container) {
        for (child in buttonContainer.children.filterIsInstance<UIButton>()) {
            //println("DISABLE BUTTON: $child")
            child.enabled = false
        }
        try {
            changeTo<T>()
        } finally {
            for (child in buttonContainer.children.filterIsInstance<UIButton>()) {
                //println("ENABLE BUTTON: $child")
                child.enabled = true
            }
        }
    }

}
