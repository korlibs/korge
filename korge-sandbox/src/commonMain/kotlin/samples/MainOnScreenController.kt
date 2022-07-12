package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.kmem.*
import com.soywiz.korev.*
import com.soywiz.korge.component.*
import com.soywiz.korge.input.*
import com.soywiz.korge.scene.ScaledScene
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.util.toStringDecimal
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.math.*

class MainOnScreenController : Scene() {
//class MainOnScreenController : ScaledScene(1280, 720) {
    override suspend fun SContainer.sceneMain() {
        val text1 = text("-").position(5, 5).apply { smoothing = false }
        val buttonTexts = (0 until 2).map {
            text("-").position(5, 20 * (it + 1) + 5).apply { smoothing = false }
        }

        addTouchGamepad(
            sceneWidth.toDouble(), sceneHeight.toDouble(),
            onStick = { x, y -> text1.setText("Stick: (${x.toStringDecimal(2)}, ${y.toStringDecimal(2)})") },
            onButton = { button, pressed -> buttonTexts[button].setText("Button: $button, $pressed") }
        )
    }

    companion object {

        fun Container.addTouchGamepad(
            width: Double = 320.0,
            height: Double = 224.0,
            radius: Double = height / 8,
            onStick: (x: Double, y: Double) -> Unit = { _, _ -> },
            onButton: (button: Int, pressed: Boolean) -> Unit = { _, _ -> }
        ) {
            val view = this
            lateinit var ball: View
            val diameter = radius * 2

            container {
                position(radius * 1.1, height - radius * 1.1)
                graphics { g ->
                    fill(Colors.BLACK) { circle(0.0, 0.0, radius) }
                    g.alpha(0.2)
                }
                ball = graphics { g ->
                    fill(Colors.WHITE) { circle(0.0, 0.0, radius * 0.7) }
                    g.alpha(0.2)
                }
            }

            fun <T : View> T.decorateButton(button: Int) = this.apply {
                var pressing = false
                onDown {
                    pressing = true
                    alpha = 0.3
                    onButton(button, true)
                }
                onUpAnywhere {
                    if (pressing) {
                        pressing = false
                        alpha = 0.2
                        onButton(button, false)
                    }
                }
            }

            for (n in 0 until 2) {
                graphics { g ->
                    g.position(width - radius * 1.1 - (diameter * n), height - radius * 1.1)
                    fill(Colors.WHITE) { circle(0.0, 0.0, radius * 0.7) }
                    g.alpha(0.2)
                    g.decorateButton(n)
                }
            }

            var dragging = false
            val start = Point(0, 0)

            view.addComponent(object : MouseComponent {
                override val view: View = view

                override fun onMouseEvent(views: Views, event: MouseEvent) {
                    val px = view.globalMatrixInv.transformX(event.x.toDouble(), event.y.toDouble())
                    val py = view.globalMatrixInv.transformY(event.x.toDouble(), event.y.toDouble())

                    when (event.type) {
                        MouseEvent.Type.DOWN -> {
                            if (px >= width / 2) return
                            start.x = px
                            start.y = py
                            ball.alpha = 0.3
                            dragging = true
                        }
                        MouseEvent.Type.MOVE, MouseEvent.Type.DRAG -> {
                            if (dragging) {
                                val deltaX = px - start.x
                                val deltaY = py - start.y
                                val length = hypot(deltaX, deltaY)
                                val maxLength = radius * 0.3
                                val lengthClamped = length.clamp(0.0, maxLength)
                                val angle = Angle.between(start.x, start.y, px, py)
                                ball.position(cos(angle) * lengthClamped, sin(angle) * lengthClamped)
                                val lengthNormalized = lengthClamped / maxLength
                                onStick(cos(angle) * lengthNormalized, sin(angle) * lengthNormalized)
                            }
                        }
                        MouseEvent.Type.UP -> {
                            ball.position(0, 0)
                            ball.alpha = 0.2
                            dragging = false
                            onStick(0.0, 0.0)
                        }
                        else -> Unit
                    }
                }
            })
        }
    }
}
