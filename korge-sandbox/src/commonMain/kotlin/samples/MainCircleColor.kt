package samples

import korlibs.image.color.*
import korlibs.korge.scene.*
import korlibs.korge.view.*

class MainCircleColor : Scene() {
    override suspend fun SContainer.sceneMain() {
        circle(100f).also { shape ->
            //roundRect(100.0, 200.0, 50.0, 50.0).also { shape ->
            shape.xD = 100.0
            shape.yD = 100.0
            //it.colorMul = Colors.RED.withAd(0.9)
            shape.stroke = Colors.RED
            shape.fill = Colors.GREEN
            shape.strokeThickness = 16.0
            addUpdater { shape.radius += 1f }
        }
    }
}
