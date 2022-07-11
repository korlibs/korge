package samples

import com.soywiz.korev.Key
import com.soywiz.korge.input.keys
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.centered
import com.soywiz.korge.view.circle
import com.soywiz.korge.view.cpuGraphics
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.minus
import com.soywiz.korma.geom.plus
import com.soywiz.korma.geom.vector.rect

class MainRotateCircle : Scene() {
    override suspend fun SContainer.sceneMain() {
        //val circle = circle(radius = 50.0, fill = Colors.RED, stroke = Colors.BLUE, strokeThickness = 20.0).xy(0, 0).also {
        //val circle = circle(radius = 50.0, fill = Colors.RED, stroke = Colors.BLUE, strokeThickness = 20.0).xy(0, 0).centered.also {
        solidRect(300.0, 300.0, Colors.YELLOW).xy(250, 250).centered
        val circle = circle(radius = 150.0, fill = Colors.RED, stroke = Colors.BLUE, strokeThickness = 40.0).xy(250, 250).centered.also {
            //val circle = circle(radius = 50.0, fill = Colors.RED).xy(100, 100).centered.also {
            it.autoScaling = false
            //it.autoScaling = true
            //it.preciseAutoScaling = true
            //it.useNativeRendering = false
        }
        cpuGraphics({
            fill(Colors.PURPLE) {
                rect(-50, -50, 60, 60)
            }
        })
        stage!!.keys {
            downFrame(Key.LEFT) { circle.rotation -= 10.degrees }
            downFrame(Key.RIGHT) { circle.rotation += 10.degrees }
        }
    }
}
