package samples

import korlibs.image.color.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.math.geom.*

class MainCircleColor : Scene() {
    override suspend fun SContainer.sceneMain() {
        circle(100f).also { shape ->
            //roundRect(100.0, 200.0, 50.0, 50.0).also { shape ->
            shape.pos = Point(100, 100)
            //it.colorMul = Colors.RED.withAd(0.9)
            shape.stroke = Colors.RED
            shape.fill = Colors.GREEN
            shape.strokeThickness = 16f
            addUpdater { shape.radius += 1f }
        }
    }
}
