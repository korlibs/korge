import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

suspend fun main() = Korge(quality = GameWindow.Quality.PERFORMANCE, title = "KorGE Shapes!") {
	setupCircle()
	setupRects()

	solidRect(300, 200, Colors.DARKCYAN)
	sgraphics {
		fill(Colors.DARKCYAN) {
			rect(-1.0, -1.0, 3.0, 2.0)
		}
		fill(Colors.AQUAMARINE) {
			circle(0.0, 0.0, 1.0)
		}
		fill(Colors.AQUAMARINE) {
			circle(1.0, 0.0, 1.0)
		}
		position(100, 100)
	}.scale(100.0, 100.0).interactive()
}

fun Stage.setupCircle() {
	val circle = Circle(radius = 32.0)
	addChild(circle)
	circle.position(512, 256)
	var growing = true
	launch {
		while (true) {
			when {
				circle.radius > 128.0 -> {
					growing = false
					circle.radius--
				}
				circle.radius < 32.0 -> {
					growing = true
					circle.radius++
				}
				else -> if (growing) circle.radius++ else circle.radius--
			}
			delay(16.milliseconds)
		}
	}
}

fun Stage.setupRects() {
	val rect1 = roundRect(80.0, 100.0, 5.0, color = Colors.GREEN).position(820, 128)
	val rect2 = roundRect(80.0, 100.0, 5.0, color = Colors.GREEN).position(1020, 128).anchor(0.5, 0.5)
	addFixedUpdater(60.timesPerSecond) {
		rect1.rotation += 1.degrees
		rect2.rotation += 1.degrees
		//no need for delay
		//delay(16.milliseconds)
	}
}

fun <T : View> T.interactive(): T = apply {
	alpha = 0.5
	onOver { alpha = 1.0 }
	onOut { alpha = 0.5 }
}
