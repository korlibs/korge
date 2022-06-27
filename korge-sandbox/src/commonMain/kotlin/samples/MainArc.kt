package samples

import com.soywiz.korev.Key
import com.soywiz.korge.input.keys
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.vector.gpuShapeView
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.bezier.Arc
import com.soywiz.korma.geom.vector.StrokeInfo
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.geom.vector.curves

class MainArc : Scene() {
    override suspend fun Container.sceneMain() {
        gpuShapeView { shape ->
            val p1 = Point(200, 100)
            val p2 = Point(300, 200)
            val radius = 100.0

            stroke(Colors.BLUE, StrokeInfo(thickness = 10.0)) {
                //fill(Colors.BLUE) {
                circle(Arc.findArcCenter(p1, p2, radius), radius)
            }
            stroke(Colors.RED, StrokeInfo(thickness = 5.0)) {
                curves(Arc.createArc(p1, p2, radius))
            }
            stroke(Colors.PURPLE, StrokeInfo(thickness = 5.0)) {
                curves(Arc.createArc(p1, p2, radius, counterclockwise = true))
            }
            fill(Colors.WHITE) {
                circle(p1, 10.0)
                circle(p2, 10.0)
                circle(Arc.findArcCenter(p1, p2, radius), 10.0)
            }
            shape.keys {
                down(Key.N9) { shape.antialiased = !shape.antialiased }
                down(Key.N0) { shape.debugDrawOnlyAntialiasedBorder = !shape.debugDrawOnlyAntialiasedBorder }
            }
        }
    }
}
