package samples

import com.soywiz.klock.*
import com.soywiz.korge.input.*
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.*
import com.soywiz.korge.view.Circle
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.async.delay
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

class MainShapes : Scene() {
    override suspend fun SContainer.sceneMain() {

        setupCircle()
        setupRects()

        solidRect(300, 200, Colors.DARKCYAN)
        graphics{
            fill(Colors.DARKCYAN) {
                rect(-1.0, -1.0, 3.0, 2.0)
            }
            fill(Colors.AQUAMARINE) {
                circle(Point(0, 0), 1f)
            }
            fill(Colors.AQUAMARINE) {
                circle(Point(1, 0), 1f)
            }
        }
            .position(100, 100)
            .scale(100.0, 100.0)
            .interactive()
    }


    fun Container.setupCircle() {
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

    fun Container.setupRects() {
        val rect1 = roundRect(80.0, 100.0, 5.0, fill = Colors.GREEN).position(820, 128)
        val rect2 = roundRect(80.0, 100.0, 5.0, fill = Colors.GREEN, stroke = Colors.RED, strokeThickness = 4.0).position(1020, 128).anchor(0.5, 0.5)
        addFixedUpdater(60.timesPerSecond) {
            rect1.rotation += 1.degrees
            rect2.rotation += 1.degrees
            //no need for delay
            //delay(16.milliseconds)
        }
    }

    fun <T : View> T.interactive(): T = apply {
        alphaF = 0.5f
        onOver { alphaF = 1.0f }
        onOut { alphaF = 0.5f }
    }

}
