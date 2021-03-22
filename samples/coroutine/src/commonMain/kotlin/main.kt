import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.random.*
import kotlin.random.*

suspend fun main() = Korge(width = 512, height = 512, bgcolor = Colors["#2b2b2b"], clipBorders = false) {
    val random = Random
    for (n in 0 until 2000) {
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
