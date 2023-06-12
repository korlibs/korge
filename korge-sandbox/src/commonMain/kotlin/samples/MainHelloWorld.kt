package samples

import korlibs.time.seconds
import korlibs.korge.resources.resourceBitmap
import korlibs.korge.scene.ScaledScene
import korlibs.korge.tween.get
import korlibs.korge.tween.tween
import korlibs.korge.view.SContainer
import korlibs.korge.view.anchor
import korlibs.korge.view.image
import korlibs.korge.view.position
import korlibs.korge.view.scale
import korlibs.image.bitmap.*
import korlibs.math.geom.degrees
import korlibs.math.interpolation.Easing

// @TODO: We could autogenerate this via gradle
private val korlibs.io.resources.ResourcesContainer.korge_png by resourceBitmap("korge.png")

class MainHelloWorld : ScaledScene(512, 512) {
    override suspend fun SContainer.sceneMain() {
        gameWindow.icon = korge_png.get().bmp.toBMP32().scaled(32, 32)

        val minDegrees = (-16).degrees
        val maxDegrees = (+16).degrees

        val image = image(korge_png) {
            //val image = image(resourcesVfs["korge.png"].readbitmapslice) {
            rotation = maxDegrees
            anchor(.5, .5)
            scale(.8)
            position(256, 256)
        }

        //bindLength(image::scaledWidth) { 100.vw }
        //bindLength(image::scaledHeight) { 100.vh }

        while (true) {
            image.tween(image::rotation[minDegrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)
            image.tween(image::rotation[maxDegrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)
        }
    }
}
