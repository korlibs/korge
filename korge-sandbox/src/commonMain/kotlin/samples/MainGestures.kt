package samples

import com.soywiz.klock.seconds
import com.soywiz.klock.timesPerSecond
import com.soywiz.korge.input.mouse
import com.soywiz.korge.input.rotationRecognizer
import com.soywiz.korge.input.scaleRecognizer
import com.soywiz.korge.input.touch
import com.soywiz.korge.scene.ScaledScene
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.ui.uiButton
import com.soywiz.korge.view.Image
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.addFixedUpdater
import com.soywiz.korge.view.anchor
import com.soywiz.korge.view.container
import com.soywiz.korge.view.image
import com.soywiz.korge.view.position
import com.soywiz.korge.view.scale
import com.soywiz.korge.view.text
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.plus
import com.soywiz.korma.interpolation.Easing

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

        touch {
            var startImageRatio = 1.0
            var startRotation = 0.degrees

            scaleRecognizer(start = {
                startImageRatio = image.scale
            }) {
                image.scale = startImageRatio * this.ratio
            }

            rotationRecognizer(start = {
                startRotation = container.rotation
            }) {
                container.rotation = startRotation + this.delta
            }
        }

        image.mouse {
            click {
                image.alpha = if (image.alpha > 0.5) 0.5 else 1.0
            }
        }

        addFixedUpdater(2.timesPerSecond) {
            println(views.input.activeTouches)
        }

        uiButton(text = "1") {
            position(10, 380)
            onPress { println("TAPPED ON 1") }
        }
        uiButton(text = "2") {
            position(150, 380)
            onPress { println("TAPPED ON 2") }
        }

        uiButton(text = "3") {
            position(300, 380)
            onPress { println("TAPPED ON 3") }
        }

        while (true) {
            image.tween(image::rotation[minDegrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)
            image.tween(image::rotation[maxDegrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)
        }
    }
}
