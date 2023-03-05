package samples

import com.soywiz.klock.seconds
import com.soywiz.korge.scene.ScaledScene
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.position
import com.soywiz.korge.view.solidRect
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.delay

class MainTweens : ScaledScene(512, 512) {
    override suspend fun SContainer.sceneMain() {
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
                rect1::alphaF[0f],
                rect2::alphaF[0f],
                time = 1.seconds
            )

            rect1.position(0, 0)
            rect2.position(0, 0)

            tween(
                rect1::alphaF[1],
                rect2::alphaF[1],
                time = 0.5.seconds
            )

            delay(0.25.seconds)
        }
    }
}
