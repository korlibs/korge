package samples

import korlibs.event.*
import korlibs.image.color.*
import korlibs.io.util.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.math.*
import korlibs.math.geom.*
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
                    fill(Colors.BLACK) { circle(Point(0, 0), radius) }
                    g.alpha(0.2)
                }
                ball = graphics { g ->
                    fill(Colors.WHITE) { circle(Point(0, 0), (radius * 0.7)) }
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
                    fill(Colors.WHITE) { circle(Point(0, 0), (radius * 0.7)) }
                    g.alpha(0.2)
                    g.decorateButton(n)
                }
            }

            var dragging = false
            var start = Point(0, 0)

            view.onEvents(*MouseEvent.Type.ALL) { event ->
                val p = view.globalMatrixInv.transform(event.pos.toDouble())

                when (event.type) {
                    MouseEvent.Type.DOWN -> {
                        if (p.x >= width / 2) return@onEvents
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
                            val lengthClamped = length.clamp(0.0, maxLength)
                            val angle = Angle.between(start, p)
                            ball.position(cos(angle) * lengthClamped, sin(angle) * lengthClamped)
                            val lengthNormalized = lengthClamped / maxLength
                            onStick(cos(angle) * lengthNormalized, sin(angle) * lengthNormalized)
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
