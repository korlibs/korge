package samples

import com.soywiz.klock.milliseconds
import com.soywiz.klock.timesPerSecond
import com.soywiz.korge.input.onOut
import com.soywiz.korge.input.onOver
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Circle
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.View
import com.soywiz.korge.view.addFixedUpdater
import com.soywiz.korge.view.anchor
import com.soywiz.korge.view.graphics
import com.soywiz.korge.view.position
import com.soywiz.korge.view.roundRect
import com.soywiz.korge.view.scale
import com.soywiz.korge.view.solidRect
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.delay
import com.soywiz.korio.async.launch
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.plus
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.geom.vector.rect

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
                circle(0.0, 0.0, 1.0)
            }
            fill(Colors.AQUAMARINE) {
                circle(1.0, 0.0, 1.0)
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
        alpha = 0.5
        onOver { alpha = 1.0 }
        onOut { alpha = 0.5 }
    }

}
