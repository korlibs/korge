package samples

import korlibs.korge.scene.Scene
import korlibs.korge.view.SContainer
import korlibs.korge.view.addUpdater
import korlibs.korge.view.circle
import korlibs.image.color.Colors

class MainCircleColor : Scene() {
    override suspend fun SContainer.sceneMain() {
        circle(100.0).also { shape ->
            //roundRect(100.0, 200.0, 50.0, 50.0).also { shape ->
            shape.x = 100.0
            shape.y = 100.0
            //it.colorMul = Colors.RED.withAd(0.9)
            shape.stroke = Colors.RED
            shape.fill = Colors.GREEN
            shape.strokeThickness = 16.0
            addUpdater { shape.radius += 1.0 }
        }
    }
}