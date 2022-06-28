package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.circle
import com.soywiz.korim.color.Colors

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
