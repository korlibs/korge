/*
import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.render.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.interpolation.*

suspend fun main() = Korge(width = 512, height = 512, bgcolor = Colors["#2b2b2b"]) {
	val minDegrees = (-16).degrees
	val maxDegrees = (+16).degrees

    views.addDebugRenderer { ctx ->
        ctx.debugLineRenderContext.apply {
            color(Colors.RED) {
                drawVector {
                    circle(100.0, 100.0, 60.0)
                }
            }
        }
        //ctx.debugLineRenderContext.line(0.0, 0.0, 100.0, 100.0)
    }

	val image = image(resourcesVfs["korge.png"].readBitmap()) {
		rotation = maxDegrees
		anchor(.5, .5)
		scale(.8)
		position(256, 256)
	}

	while (true) {
		image.tween(image::rotation[minDegrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)
		image.tween(image::rotation[maxDegrees], time = 1.seconds, easing = Easing.EASE_IN_OUT)
	}
}
*/
