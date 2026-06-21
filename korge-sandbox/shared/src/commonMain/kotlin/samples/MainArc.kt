package samples

import korlibs.event.*
import korlibs.image.color.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.math.geom.bezier.*
import korlibs.math.geom.vector.*

class MainArc : Scene() {
    override suspend fun SContainer.sceneMain() {
        //graphics(renderer = GraphicsRenderer.GPU) { shape ->
        graphics(renderer = GraphicsRenderer.SYSTEM) { shape ->
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
