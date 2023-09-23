package samples

import korlibs.event.*
import korlibs.image.color.*
import korlibs.image.paint.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge.view.vector.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*
import korlibs.render.*

class MainGpuVectorRendering2 : Scene() {
    override suspend fun SContainer.sceneMain() {
        val mainStrokePaint = LinearGradientPaint(0, 0, 0, 300).addColorStop(0.0, Colors.GREEN).addColorStop(0.5,
            Colors.RED
        ).addColorStop(1.0, Colors.BLUE)
        val secondaryStrokePaint = Colors.GREEN.withAd(0.5)

        //circle(128.0, fill = Colors.RED).xy(200, 200).also { it.antialiased = false }
        //roundRect(300, 300, 64, fill = mainStrokePaint).xy(200, 200).also { it.antialiased = true }

        //return

        lateinit var shape: GpuShapeView

        container {
            //xy(0, 0)
            xy(300, 300)
            rotation = 30.degrees
            //val shape = graphics({
            shape = gpuShapeView({
                //val lineWidth = 6.12123231 * 2
                val lineWidth = 12.0
                val width = 300.0
                val height = 300.0
                //rotation = 180.degrees
                this.stroke(mainStrokePaint, lineWidth = lineWidth, lineJoin = LineJoin.MITER, lineCap = LineCap.BUTT) {
                    //this.fill(mainStrokePaint) {
                    this.rect(lineWidth / 2, lineWidth / 2, width, height)
                    this.rect(lineWidth / 2 + 32, lineWidth / 2 + 32, width - 64, height - 64)
                }
                //this.fill(secondaryStrokePaint) {
                //    this.rect(600, 50, 300, 200)
                //}
            }) {
                xy(-150, -150)
                keys {
                    down(Key.N0) { antialiased = !antialiased }
                    down(Key.A) { antialiased = !antialiased }
                }
            }
            keys {
                downFrame(Key.N1) { rotation = 15.degrees * 0 }
                downFrame(Key.N2) { rotation = 15.degrees * 1 }
                downFrame(Key.N3) { rotation = 15.degrees * 2 }
                downFrame(Key.N4) { rotation = 15.degrees * 3 }
                downFrame(Key.N5) { rotation = 15.degrees * 4 }
                downFrame(Key.N6) { rotation = 15.degrees * 5 }
                downFrame(Key.N7) { rotation = 15.degrees * 6 }
                downFrame(Key.N8) { rotation = 15.degrees * 7 }
                downFrame(Key.N9) { rotation = 180.degrees }
                downFrame(Key.LEFT) { rotation -= 1.degrees }
                downFrame(Key.RIGHT) { rotation += 1.degrees }
                up(Key.Q) { views.gameWindow.quality = if (views.gameWindow.quality == GameWindow.Quality.PERFORMANCE) GameWindow.Quality.QUALITY else GameWindow.Quality.PERFORMANCE }
            }
        }

        gamepad {
            connected { println("CONNECTED gamepad=${it}") }
            disconnected { println("DISCONNECTED gamepad=${it}") }
            button { playerId, pressed, button, value ->
                if (pressed && button == GameButton.START) {
                    shape.antialiased = !shape.antialiased
                }
                println("BUTTON: $playerId, $pressed, button=$button, value=$value")
            }
            stick { playerId, stick, x, y ->
                if (stick == GameStick.LEFT) {
                    rotation += x.degrees
                }
            }
            updatedGamepad {
                shape.rotation += it.ly.degrees
            }
        }
    }
}
