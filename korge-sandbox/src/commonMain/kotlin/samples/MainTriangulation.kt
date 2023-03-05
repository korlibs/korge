package samples

import com.soywiz.korge.input.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.triangle.triangulate.*

class MainTriangulation : Scene() {
    override suspend fun SContainer.sceneMain() {
        text("Add Points by clicking with the mouse", 14.0).position(5.0, 5.0)
        val g = graphics(renderer = GraphicsRenderer.SYSTEM)
        //val g = graphics(renderer = GraphicsRenderer.GPU)
        //val g = cpuGraphics()
        g.position(100, 100)

        val points = PointArrayList()

        var additionalPoint: Point? = null

        fun repaint(finished: Boolean) {
            g.updateShape {
                clear()
                /*
                val path = VectorPath {
                    rect(0, 0, 100, 100)
                    rect(25, 25, 50, 50)
                }
                 */

                val edges = points + listOfNotNull(additionalPoint).toPointArrayList()

                edges.fastForEachPoint { point ->
                    fill(Colors.RED) {
                        circle(point, 3f)
                    }
                }

                if (finished) {
                    println("Points: $points")
                }

                if (points.size >= 3) {
                    for (triangle in points.triangulate()) {
                        fill(Colors.GREEN.withAd(0.2)) {
                            val p0 = Point(triangle.p0)
                            val p1 = Point(triangle.p1)
                            val p2 = Point(triangle.p2)
                            moveTo(p0)
                            lineTo(p1)
                            lineTo(p2)
                            close()
                        }
                    }
                    stroke(Colors.GREEN, StrokeInfo(thickness = 1.0)) {
                        for (triangle in points.triangulate()) {
                            val p0 = Point(triangle.p0)
                            val p1 = Point(triangle.p1)
                            val p2 = Point(triangle.p2)
                            line(p0, p1)
                            line(p1, p2)
                            line(p2, p0)
                        }
                    }
                }

                for (n in 0 until edges.size - 1) {
                    val e0 = Point(edges[n])
                    val e1 = Point(edges[n + 1])
                    val last = n == edges.size - 2
                    stroke(if (last) Colors.RED else Colors.BLUE, StrokeInfo(thickness = 2.0)) {
                        line(e0, e1)
                    }
                }
            }
        }

        mouse {
            onClick {
                //additionalPoint = null
                try {
                    points.add(g.localMousePos(views).mutable)
                    if (points.size >= 3) {
                        points.triangulate()
                    }
                //} catch (e: Poly2Tri.PointError) {
                } catch (e: Throwable) {
                    e.printStackTrace()
                    points.removeLast()
                }
                repaint(finished = true)
                //println("CLICK")
            }

            onMove {
                additionalPoint = g.localMousePos(views)
                repaint(finished = false)
            }
        }

        repaint(finished = true)

    }
}
