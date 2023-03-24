package samples

import korlibs.time.timesPerSecond
import korlibs.korge.scene.ScaledScene
import korlibs.korge.view.SContainer
import korlibs.korge.view.position
import korlibs.korge.view.solidRect
import korlibs.image.color.Colors
import korlibs.io.async.launchImmediately
import korlibs.math.geom.MPoint
import korlibs.math.interpolation.*
import korlibs.math.random.get
import util.*
import kotlin.random.Random

class MainCoroutine : ScaledScene(512, 512) {
    override suspend fun SContainer.sceneMain() {
        val random = Random
        for (n in 0 until 2_000) {
            launchImmediately {
                val view = solidRect(10, 10, Colors.RED.interpolateWith(random[Ratio.ZERO, Ratio.ONE], Colors.BLUE))
                view.position(random[0, 512], random[0, 512])

                frameBlock(60.timesPerSecond) {
                    //view.frameBlock(60.timesPerSecond) {
                    while (true) {
                        val targetX = random[0, 512].toDouble()
                        val targetY = random[0, 512].toDouble()

                        while (MPoint.distance(view.x, view.y, targetX, targetY) > 5.0) {
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
