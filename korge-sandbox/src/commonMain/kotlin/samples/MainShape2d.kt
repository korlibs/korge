package samples

import korlibs.image.color.*
import korlibs.io.lang.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.vector.*
import korlibs.math.geom.*
import korlibs.math.geom.Circle
import korlibs.math.geom.Ellipse
import korlibs.math.geom.shape.*

class MainShape2dScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        lateinit var textDistanceView: UIText
        lateinit var textAreaView: UIText
        lateinit var textPerimeterView: UIText
        lateinit var projected: View
        lateinit var cursor: View
        lateinit var gpuShapeView: GpuShapeView
        lateinit var normalVectorView: GpuShapeView

        val cursorShape = buildVectorPath { star(7, 10.0, 16.0) }

        val shapes = listOf(
            Circle(Point(100, 100), radius = 50f),
            Ellipse(Point(100, 100), radius = Size(50f, 30f)),
            Rectangle(50, 50, 250, 100),
            RoundRectangle(Rectangle(50, 50, 250, 100), RectCorners(10f, 15f, 20f, 30f)),
            buildVectorPath { star(7, 30.0, 50.0, x = 100.0, y = 100.0) },
            buildVectorPath { star(7, 30.0, 50.0, x = 100.0, y = 100.0); rectHole(Rectangle(80, 80, 40, 40)) },
        )
        var shape: NShape2d = shapes.first()

        fun update() {
            val pos = localMousePos(views)

            val intersects = NShape2d.intersects(shape, Matrix.NIL, cursorShape, Matrix.IDENTITY.translated(pos))

            gpuShapeView.alpha = if (intersects) 1.0 else 0.5
            //println("intersects=$intersects")
            try {
                cursor.pos = pos
                textDistanceView.text = "distance: ${shape.distance(pos)}"
                val projectedPos = shape.projectedPoint(pos)
                projected.pos = projectedPos
                normalVectorView.pos = projectedPos
                normalVectorView.updateShape {
                    stroke(Colors.GREEN, lineWidth = 4.0) {
                        moveTo(0, 0)
                        lineTo(shape.normalVectorAt(pos) * 10)
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        fun updateShape() {
            gpuShapeView.updateShape { fill(Colors.WHITE) { path(shape.toVectorPath()) } }
            textAreaView.text = "area: ${shape.area}"
            textPerimeterView.text = "perimeter: ${shape.perimeter}"
            update()
        }
        uiHorizontalStack(adjustHeight = false) {
            for (rshape in shapes) {
                uiButton(rshape::class.portableSimpleName) { clicked { shape = rshape; updateShape() } }
            }
            textDistanceView = uiText("")
            textAreaView = uiText("")
            textPerimeterView = uiText("")
        }
        //val shape = Ellipse(Point(100, 100), radius = Size(50f, 30f))
        gpuShapeView = gpuShapeView { }
        normalVectorView = gpuShapeView { }
        projected = circle(4.0, Colors.RED).centered
        cursor = gpuShapeView { fill(Colors.GREEN.withAd(0.5)) { path(cursorShape) } }

        addUpdater {
            update()
        }
        updateShape()
    }
}
