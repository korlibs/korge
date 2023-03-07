package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.kmem.*
import com.soywiz.korev.*
import com.soywiz.korge.component.*
import com.soywiz.korge.input.*
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
                    fill(Colors.BLACK) { circle(Point(0, 0), radius.toFloat()) }
                    g.alpha(0.2)
                }
                ball = graphics { g ->
                    fill(Colors.WHITE) { circle(Point(0, 0), (radius * 0.7).toFloat()) }
                    g.alpha(0.2)
                }
            }

            fun <T : View> T.decorateButton(button: Int) = this.apply {
                var pressing = false
                onDown {
                    pressing = true
                    alphaF = 0.3f
                    onButton(button, true)
                }
                onUpAnywhere {
                    if (pressing) {
                        pressing = false
                        alphaF = 0.2f
                        onButton(button, false)
                    }
                }
            }

            for (n in 0 until 2) {
                graphics { g ->
                    g.position(width - radius * 1.1 - (diameter * n), height - radius * 1.1)
                    fill(Colors.WHITE) { circle(Point(0, 0), (radius * 0.7).toFloat()) }
                    g.alpha(0.2)
                    g.decorateButton(n)
                }
            }

            var dragging = false
            var start = Point(0, 0)

            view.onEvent(MouseEvent.Type.DOWN, MouseEvent.Type.MOVE, MouseEvent.Type.DRAG, MouseEvent.Type.UP) { event ->
                val p = view.globalMatrixInv.transform(event.pos.toFloat())

                when (event.type) {
                    MouseEvent.Type.DOWN -> {
                        if (p.x >= width / 2) return@onEvent
                        start = p
                        ball.alphaF = 0.3f
                        dragging = true
                    }
                    MouseEvent.Type.MOVE, MouseEvent.Type.DRAG -> {
                        if (dragging) {
                            val deltaX = p.x - start.x
                            val deltaY = p.y - start.y
                            val length = hypot(deltaX, deltaY)
                            val maxLength = radius * 0.3f
                            val lengthClamped = length.clamp(0f, maxLength.toFloat())
                            val angle = Angle.between(start, p)
                            ball.position(cosd(angle) * lengthClamped, sind(angle) * lengthClamped)
                            val lengthNormalized = lengthClamped / maxLength
                            onStick(cosd(angle) * lengthNormalized, sind(angle) * lengthNormalized)
                        }
                    }
                    MouseEvent.Type.UP -> {
                        ball.position(0, 0)
                        ball.alphaF = 0.2f
                        dragging = false
                        onStick(0.0, 0.0)
                    }
                    else -> Unit
                }
            }
        }
    }
}
