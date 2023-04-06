package samples

import korlibs.time.seconds
import korlibs.time.timesPerSecond
import korlibs.korge.input.*
import korlibs.korge.scene.ScaledScene
import korlibs.korge.tween.get
import korlibs.korge.tween.tween
import korlibs.korge.ui.uiButton
import korlibs.korge.view.Image
import korlibs.korge.view.SContainer
import korlibs.korge.view.addFixedUpdater
import korlibs.korge.view.anchor
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.position
import korlibs.korge.view.scale
import korlibs.korge.view.text
import korlibs.image.format.readBitmap
import korlibs.io.file.std.resourcesVfs
import korlibs.math.geom.*
import korlibs.math.interpolation.Easing

class MainGestures : ScaledScene(512, 512) {
//class MainGestures : Scene() {
    override suspend fun SContainer.sceneMain() {
        val minDegrees = (-16).degrees
        val maxDegrees = (+16).degrees
        lateinit var image: Image

        val container = container {
            position(256, 256)
            image = image(resourcesVfs["korge.png"].readBitmap()) {
                rotation = maxDegrees
                anchor(.5, .5)
                scale(.8)
            }
        }

        text("Zoom and rotate with two fingers")

        gestures {
            onMagnify {
                image.scaleD += it.amount * image.scaleD
            }
            onRotate {
                image.rotation += 5.degrees * it.amount
            }
        }

        touch {
            var startImageRatio = 1.0
            var startRotation = 0.degrees

            scaleRecognizer(start = {
                startImageRatio = image.scaleD
            }) {
                image.scaleD = startImageRatio * this.ratio
            }

            rotationRecognizer(start = {
                startRotation = container.rotation
            }) {
                container.rotation = startRotation + this.delta
            }
        }

        image.mouse {
            click {
                image.alphaF = if (image.alphaF > 0.5f) 0.5f else 1.0f
            }
        }

        addFixedUpdater(2.timesPerSecond) {
            println(views.input.activeTouches)
        }

        uiButton(label = "1") {
            position(10, 380)
            onPress { println("TAPPED ON 1") }
        }
        uiButton(label = "2") {
            position(150, 380)
            onPress { println("TAPPED ON 2") }
        }

        uiButton(label = "3") {
            position(300, 380)
            onPress { println("TAPPED ON 3") }
        }

        while (true) {
            image.tween(image::rotation[minDegrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)
            image.tween(image::rotation[maxDegrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)
        }
    }
}
