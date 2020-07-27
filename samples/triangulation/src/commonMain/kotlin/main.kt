import com.soywiz.korge.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.triangle.triangulate.*

suspend fun main() = Korge(width = 512, height = 512) {
	val stage = this
	text("Add Points by clicking with the mouse", 14.0).position(5.0, 5.0)
	graphics {
		val graphics = this
		graphics.useNativeRendering = false
		position(100, 100)

		val points = arrayListOf<Point>()

		var additionalPoint: Point? = null

		fun repaint(finished: Boolean) {
			clear()
			/*
			val path = VectorPath {
				rect(0, 0, 100, 100)
				rect(25, 25, 50, 50)
			}
			 */

			val edges = points + listOfNotNull(additionalPoint)

			for (point in edges) {
				fill(Colors.RED) {
					circle(point.x, point.y, 3.0)
				}
			}

			if (finished) {
				println("Points: $points")
			}

			if (points.size >= 3) {
				stroke(Colors.GREEN, Context2d.StrokeInfo(thickness = 1.0)) {
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
				stroke(if (last) Colors.RED else Colors.BLUE, Context2d.StrokeInfo(thickness = 2.0)) {
					line(e0, e1)
				}
			}
		}

		stage.mouse {
			onClick {
				points.add(graphics.localMouseXY(views))
				repaint(finished = true)
				//println("CLICK")
			}

			onMove {
				additionalPoint = graphics.localMouseXY(views)
				repaint(finished = false)
			}
		}

		repaint(finished = true)
	}
}
