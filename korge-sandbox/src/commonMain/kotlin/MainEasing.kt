import com.soywiz.korge.input.onClick
import com.soywiz.korge.input.onOut
import com.soywiz.korge.input.onOver
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.tween.tweenAsync
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Graphics
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.View
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.circle
import com.soywiz.korge.view.graphics
import com.soywiz.korge.view.line
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.text
import com.soywiz.korge.view.vector.GpuShapeView
import com.soywiz.korge.view.vector.gpuShapeView
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.vector.StrokeInfo
import com.soywiz.korio.lang.Cancellable
import com.soywiz.korio.lang.textCase
import com.soywiz.korio.util.StrReader
import com.soywiz.korma.geom.vector.line
import com.soywiz.korma.interpolation.Easing
import kotlinx.coroutines.Job

suspend fun Stage.mainEasing() {
    var ballTween: Job? = null
    val ball = circle(64.0, Colors.PURPLE).xy(64, 64)

    fun renderEasing(easing: Easing): View {
        return Container().apply {
            val bg = solidRect(64, -64, Colors.BLACK.withAd(0.2))
            gpuShapeView {
                updateShape {
                    stroke(Colors.RED, lineWidth = 4.0) {
                        this.line(0.0, 0.0, 0.0, -64.0)
                        this.line(0.0, 0.0, 64.0, 0.0)
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
                                moveTo(x, -y)
                            } else {
                                lineTo(x, -y)
                            }
                        }
                    }
                }
            }.addTo(this)
            val textSize = 10.0
            text("$easing", textSize = textSize).xy(0.0, textSize)
            onOver { bg.color = Colors.BLACK.withAd(1.0) }
            onOut { bg.color = Colors.BLACK.withAd(0.2) }
            onClick {
                ballTween?.cancel()
                ballTween = ball.tweenAsync(ball::x[64.0, 64.0 + 512.0], easing = easing)
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
