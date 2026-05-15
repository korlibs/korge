package samples

import korlibs.image.color.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import kotlinx.coroutines.*

class MainEasing : Scene() {
    override suspend fun SContainer.sceneMain() {
        var ballTween: Job? = null
        val ball = circle(64.0, Colors.PURPLE).xy(64, 64)

        fun renderEasing(easing: Easing): View {
            return Container().apply {
                val bg = solidRect(64, -64, Colors.BLACK.withAd(0.2))
                graphics { shape ->
                    stroke(Colors.RED, lineWidth = 4.0) {
                        this.line(Point(0, 0), Point(0, -64))
                        this.line(Point(0, 0), Point(64, 0))
                    }
                    stroke(Colors.WHITE, lineWidth = 2.0) {
                        var first = true
                        //val overflow = 8
                        val overflow = 0
                        for (n in (-overflow)..(64 + overflow)) {
                            val ratio = n.toDouble() / 64.0
                            val x = n.toDouble()
                            val y = easing(ratio) * 64
                            //println("x=$x, y=$y, ratio=$ratio")
                            if (first) {
                                first = false
                                moveTo(Point(x, -y))
                            } else {
                                lineTo(Point(x, -y))
                            }
                        }
                    }
                }.addTo(this)
                val textSize = 10.0
                text("$easing", textSize = textSize).xy(0.0, textSize)
                onOver { bg.color = Colors.BLACK.withAd(1.0) }
                onOut { bg.color = Colors.BLACK.withAd(0.2) }
                onClickSuspend(coroutineContext) {
                    ballTween?.cancel()
                    ballTween = ball.tweenAsync(ball::x[64f, 64f + 512f], easing = easing)
                }
            }
        }

        val easings = listOf(
            *Easing.ALL.values.toTypedArray(),
            Easing.cubic(.86, .13, .22, .84),
        )

        var mn = 0
        for (my in 0 until 4) {
            for (mx in 0 until 8) {
                val easing = easings.getOrNull(mn++) ?: continue
                renderEasing(easing).xy(50 + mx * 100, 300 + my * 100).addTo(this)
            }
        }
    }
}
