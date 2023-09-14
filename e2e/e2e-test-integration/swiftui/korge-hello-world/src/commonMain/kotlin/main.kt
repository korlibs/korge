import korlibs.time.*
import korlibs.korge.*
import korlibs.korge.scene.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*

suspend fun main() = Korge(windowSize = Size(512, 512), backgroundColor = Colors["#2b2b2b"]) {
	val sceneContainer = sceneContainer()
	sceneContainer.changeTo({ MyScene() })
}

class MyScene : Scene() {
	override suspend fun SContainer.sceneMain() {
		val minDegrees = (-16).degrees
		val maxDegrees = (+16).degrees

		val image = image(resourcesVfs["korge.png"].readBitmap()) {
        //val image = solidRect(50, 50, Colors.RED) {
			rotation = maxDegrees
			anchor(.5, .5)
			scale(0.8)
		}.position((size / 2).toPoint())
        println("size=$size")
        println("width=$width, height=$height")
        println("width=$scaledWidth, height=$scaledHeight")
        println("width=$unscaledWidth, height=$unscaledHeight")

        println("image.pos=${image.pos}")

		while (true) {
			image.tween(image::rotation[minDegrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)
			image.tween(image::rotation[maxDegrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)
		}
	}
}
