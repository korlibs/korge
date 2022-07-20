package samples

import com.soywiz.klock.timesPerSecond
import com.soywiz.korge.scene.ScaledScene
import com.soywiz.korge.time.frameBlock
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.position
import com.soywiz.korge.view.solidRect
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korma.geom.Point
import com.soywiz.korma.random.get
import kotlin.random.Random

class MainCoroutine : ScaledScene(512, 512) {
    override suspend fun SContainer.sceneMain() {
        val random = Random
        for (n in 0 until 2_000) {
            launchImmediately {
                val view = solidRect(10, 10, Colors.RED.interpolateWith(random[0.0, 1.0], Colors.BLUE))
                view.position(random[0, 512], random[0, 512])

                frameBlock(60.timesPerSecond) {
                    //view.frameBlock(60.timesPerSecond) {
                    while (true) {
                        val targetX = random[0, 512].toDouble()
                        val targetY = random[0, 512].toDouble()

                        while (Point.distance(view.x, view.y, targetX, targetY) > 5.0) {
                            when {
                                view.x < targetX -> view.x += 2
                                view.x > targetX -> view.x -= 2
                            }
                            when {
                                view.y < targetY -> view.y += 2
                                view.y > targetY -> view.y -= 2
                            }
                            frame()
                        }
                    }
                }
            }
        }
    }
}
