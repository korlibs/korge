package samples

import korlibs.image.color.*
import korlibs.korge.scene.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.time.*

class MainTweens : ScaledScene(512, 512) {
    override suspend fun SContainer.sceneMain() {
        val rect1 = solidRect(100, 100, Colors.RED)
        val rect2 = solidRect(100, 100, Colors.BLUE)

        while (true) {
            tween(
                rect1::xD[widthD - 100],
                rect2::yD[heightD - 200],
                time = 1.seconds
            )

            tween(
                rect1::yD[heightD - 100],
                rect2::xD[widthD - 100],
                rect2::yD[heightD - 100],
                time = 1.seconds,
            )

            tween(
                rect1::alphaF[0f],
                rect2::alphaF[0f],
                time = 1.seconds
            )

            rect1.position(0, 0)
            rect2.position(0, 0)

            tween(
                rect1::alphaF[1],
                rect2::alphaF[1],
                time = 0.5.seconds
            )

            delay(0.25.seconds)
        }
    }
}
