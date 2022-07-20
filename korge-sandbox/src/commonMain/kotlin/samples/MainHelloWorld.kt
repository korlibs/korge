package samples

import com.soywiz.klock.seconds
import com.soywiz.korge.resources.resourceBitmap
import com.soywiz.korge.scene.ScaledScene
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.anchor
import com.soywiz.korge.view.image
import com.soywiz.korge.view.position
import com.soywiz.korge.view.scale
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.interpolation.Easing

class MainHelloWorld : ScaledScene(512, 512) {
    // @TODO: We could autogenerate this via gradle
    val com.soywiz.korio.resources.ResourcesContainer.korge_png by resourceBitmap("korge.png")

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
