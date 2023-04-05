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
import korlibs.math.geom.Line
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
        lateinit var annotationsView: GpuShapeView

        val cursorShape = buildVectorPath { star(7, 10.0, 16.0) }

        val shapes = listOf(
            Circle(Point(100, 100), radius = 50f),
            Ellipse(Point(100, 100), radius = Size(50f, 30f)),
            Rectangle(50, 50, 250, 100),
            RoundRectangle(Rectangle(50, 50, 250, 100), RectCorners(10f, 15f, 20f, 30f)),
            buildVectorPath { star(7, 30.0, 50.0, x = 100.0, y = 100.0) },
            buildVectorPath { star(7, 30.0, 50.0, x = 100.0, y = 100.0); rectHole(Rectangle(80, 80, 40, 40)) },
            Line(Point(50, 50), Point(150, 120)),
        )
        var shape: Shape2D = shapes.first()

        fun update() {
            val pos = localMousePos(views)

            val intersects = Shape2D.intersects(shape, Matrix.NIL, cursorShape, Matrix.IDENTITY.translated(pos))
            val intersections = Shape2D.intersections(shape, Matrix.NIL, cursorShape, Matrix.IDENTITY.translated(pos))

            //println("intersections=$intersections")

            gpuShapeView.alpha = if (intersects) 1f else 0.5f
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
                annotationsView.updateShape {
                    intersections.fastForEach { p ->
                        fill(Colors.RED) {
                            circle(p, 4f)
                        }
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        fun updateShape() {
            gpuShapeView.updateShape {
                val vectorPath = shape.toVectorPath()
                if (vectorPath.isLastCommandClose) {
                    fill(Colors.WHITE) { path(vectorPath) }
                } else {
                    stroke(Colors.WHITE, lineWidth = 4.0) { path(vectorPath) }
                }
            }
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
        annotationsView = gpuShapeView { }
        gpuShapeView = gpuShapeView { }
        normalVectorView = gpuShapeView { }
        projected = circle(4f, Colors.RED).centered
        cursor = gpuShapeView { fill(Colors.GREEN.withAd(0.5)) { path(cursorShape) } }


        //container {
        //    xy(300, 300)
        //    val view1 = gpuShapeView { stroke(Colors.GREEN.withAd(0.5), lineWidth = 4.0) { circle(Point(100, 100), 100f) } }.scale(1.1).xy(-10, -80)
        //    val view2 = gpuShapeView { stroke(Colors.GREEN.withAd(0.5), lineWidth = 4.0) { circle(Point(100, 100), 100f) } }.scale(1.5).xy(70, 100)
        //    val circle1 = Circle(Point(100, 100), 100f)
        //    val circle2 = Circle(Point(100, 100), 100f)
        //    val intersections = circle1.intersectionsWith(view1.localMatrix, circle2, view2.localMatrix)
        //    println("matrices=${view1.localMatrix}, ${view2.localMatrix}")
        //    println("circles=$circle1, $circle2")
        //    println("intersections=$intersections")
        //    gpuShapeView {
        //        intersections.fastForEach { p ->
        //            fill(Colors.RED) {
        //                circle(p, 4f)
        //            }
        //            //println(it)
        //        }
        //    }
        //}


        addUpdater {
            update()
        }
        updateShape()
    }
}
