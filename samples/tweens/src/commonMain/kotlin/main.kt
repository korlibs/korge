import com.soywiz.klock.seconds
import com.soywiz.korge.Korge
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.position
import com.soywiz.korge.view.solidRect
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.delay

suspend fun main() = Korge(width = 512, height = 512, virtualWidth = 512, virtualHeight = 512, title = "Tweens") {
    val rect1 = solidRect(100, 100, Colors.RED)
    val rect2 = solidRect(100, 100, Colors.BLUE)

    while (true) {
        tween(
            rect1::x[width - 100],
            rect2::y[height - 200],
            time = 1.seconds
        )

        tween(
            rect1::y[height - 100],
            rect2::x[width - 100],
            rect2::y[height - 100],
            time = 1.seconds,
        )

        tween(
            rect1::alpha[0],
            rect2::alpha[0],
            time = 1.seconds
        )

        rect1.position(0, 0)
        rect2.position(0, 0)

        tween(
            rect1::alpha[1],
            rect2::alpha[1],
            time = 0.5.seconds
        )

        delay(0.25.seconds)
    }
}
