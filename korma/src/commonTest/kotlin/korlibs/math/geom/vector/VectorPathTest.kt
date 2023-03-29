package korlibs.math.geom.vector

import korlibs.math.geom.*
import korlibs.math.geom.bezier.*
import korlibs.math.geom.shape.*
import kotlin.test.*

class VectorPathTest {
    @Test
    fun testSimpleSquare() {
        val g = buildVectorPath {
            moveTo(Point(0, 0))
            lineTo(Point(100, 0))
            lineTo(Point(100, 100))
            lineTo(Point(0, 100))
            close()
        }

        assertEquals(true, g.containsPoint(50, 50))
        assertEquals(false, g.containsPoint(150, 50))
        assertEquals(Rectangle(0, 0, 100, 100), g.getBounds())
    }

    @Test
    fun testCircle() {
        val g = VectorPath()
        g.circle(Point(0, 0), 100f)
        assertEquals("Stats(moveTo=1, lineTo=0, quadTo=0, cubicTo=4, close=1)", g.readStats().toString())
        //println(g.numberOfIntersections(0, 0))
        assertEquals(true, g.containsPoint(0, -1))
        assertEquals(true, g.containsPoint(0, +1))
        assertEquals(true, g.containsPoint(0, 0))
        assertEquals(false, g.containsPoint(120, 0))
        assertEquals(false, g.containsPoint(-100, -100))
        assertEquals(true, g.containsPoint(64, 64))
        assertEquals(false, g.containsPoint(78, 78))
    }

    @Test
    fun testSquareWithHole() {
        val g = VectorPath()
        g.moveTo(Point(0, 0))
        g.lineTo(Point(100, 0))
        g.lineTo(Point(100, 100))
        g.lineTo(Point(0, 100))
        g.close()

        g.moveTo(Point(75, 25))
        g.lineTo(Point(25, 25))
        g.lineTo(Point(25, 75))
        g.lineTo(Point(75, 75))
        g.close()

        assertEquals(false, g.containsPoint(50, 50))
        assertEquals(false, g.containsPoint(150, 50))
        assertEquals(true, g.containsPoint(20, 50))
        assertEquals(Rectangle(0, 0, 100, 100), g.getBounds())
        //g.filled(Context2d.Color(Colors.RED)).raster().showImageAndWaitExt()
    }

    @Test
    fun testRotatedSquare() {
        val vp = VectorPath().apply {
            // /\
            // \/
            moveTo(Point(0, -50))
            lineTo(Point(-50, 0))
            lineTo(Point(0, +50))
            lineTo(Point(+50, 0))
            lineTo(Point(0, -50))
            close()
        }
        assertEquals(true, vp.containsPoint(0, 0))
        assertEquals(false, vp.containsPoint(-51, 0))
    }

    @Test
    fun testContainsPoint() {
        buildVectorPath { rect(0, 0, 10, 10) }.also {
            assertEquals(true, it.containsPoint(5, 5))
            assertEquals(false, it.containsPoint(-1, -1))
            assertEquals(
                true,
                it.containsPoint(10, 10)
            ) // This is true in JS: var ctx = document.createElement('canvas').getContext('2d'); ctx.beginPath(), ctx.rect(0, 0, 100, 100), ctx.isPointInPath(100, 100)
            assertEquals(false, it.containsPoint(11, 11))
        }
        buildVectorPath(winding = Winding.NON_ZERO) {
            rect(0, 0, 10, 10)
            rect(20, 0, 10, 10)
        }.also {
            assertEquals(true, it.containsPoint(5, 5))
            assertEquals(true, it.containsPoint(25, 5))
            assertEquals(false, it.containsPoint(-1, -1))
            assertEquals(true, it.containsPoint(10, 10))
            assertEquals(false, it.containsPoint(11, 11))
            assertEquals(false, it.containsPoint(19, 5))
        }
        buildVectorPath(winding = Winding.EVEN_ODD) {
            rect(0, 0, 20, 10)
            rect(10, 0, 20, 10)
        }.also {
            // [0-10] [10-20] [20-30]
            assertEquals(true, it.containsPoint(5, 5))
            assertEquals(false, it.containsPoint(15, 5))
            assertEquals(true, it.containsPoint(25, 5))
        }

        buildVectorPath(winding = Winding.NON_ZERO) {
            rect(0, 0, 20, 10)
            rect(10, 0, 20, 10)
        }.also {
            // [0-30]
            assertEquals(true, it.containsPoint(5, 5))
            assertEquals(true, it.containsPoint(15, 5))
            assertEquals(true, it.containsPoint(25, 5))
        }
    }

    @Test
    fun testContainsPoint2() {
        buildVectorPath(winding = Winding.NON_ZERO, block = fun VectorPath.() {
            moveTo(Point(1, 1))
            lineTo(Point(2, 1))
            lineTo(Point(2, 2))
            lineTo(Point(1, 2))
            close()
        }).also {
            assertEquals(false, it.containsPoint(0.99, 0.99))
            //assertEquals(false, it.containsPoint(0.9999, 0.9999))
            assertEquals(true, it.containsPoint(1, 1))
            assertEquals(true, it.containsPoint(1.1, 1.1))
            assertEquals(true, it.containsPoint(1.9, 1.9))
            //assertEquals(true, it.containsPoint(2, 2)) // @TODO: This is true on JS
            assertEquals(false, it.containsPoint(2.01, 2.01))
            assertEquals(false, it.containsPoint(2.1, 2.1))
            assertEquals(false, it.containsPoint(0, 0))
        }

        buildVectorPath(winding = Winding.EVEN_ODD, block = fun VectorPath.() {
            moveTo(Point(-1, -1))
            lineTo(Point(+1, -1))
            lineTo(Point(+1, 0))
            lineTo(Point(+1, +1))
            lineTo(Point(-1, +1))
            lineTo(Point(-1, 0))
            close()
        }).also {
            assertEquals(true, it.containsPoint(0, 0))
        }

        // Verified on JS
        //const canvas = document.querySelector("canvas")
        //const ctx = canvas.getContext('2d')
        //const path = new Path2D();
        //path.moveTo(1, 1)
        //path.lineTo(2, 1)
        //path.lineTo(2, 2)
        //path.lineTo(1, 2)
        //path.closePath()
        //ctx.fill(path)
        //console.log(ctx.isPointInPath(path, 0.99, 0.99)) // false
        //console.log(ctx.isPointInPath(path, 0.9999, 0.9999)) // false
        //console.log(ctx.isPointInPath(path, 1, 1)) // true
        //console.log(ctx.isPointInPath(path, 1.1, 1.1)) // true
        //console.log(ctx.isPointInPath(path, 1.9, 1.9)) // true
        //console.log(ctx.isPointInPath(path, 2, 2)) // true
        //console.log(ctx.isPointInPath(path, 2.001, 2.001)) // false
        //console.log(ctx.isPointInPath(path, 2.1, 2.1)) // false
        //console.log(ctx.isPointInPath(path, 0, 0)) // false
    }

    val path1 = buildVectorPath(VectorPath()) {
        rect(0, 0, 100, 100)
    }
    val path2 = buildVectorPath(VectorPath()) {
        rect(10, 10, 150, 80)
    }
    val path3 = buildVectorPath(VectorPath()) {
        rect(110, 0, 100, 100)
    }

    val path2b = path2.clone().applyTransform(Matrix().scaled(2.0))

    @Test
    fun testToString() {
        assertEquals("VectorPath(M20,20 L320,20 L320,180 L20,180 Z)", path2b.toString())
    }

    @Test
    fun testCollides() {
        assertEquals(true, path1.intersectsWith(path2))
        assertEquals(true, path2.intersectsWith(path3))
        assertEquals(false, path1.intersectsWith(path3))

        val path2clone = path2.clone()  // here VectorPath.version == 0
        assertEquals(true, path1.intersectsWith(path2clone))
        path2clone.clear()
        assertEquals(false, path1.intersectsWith(path2clone))
    }

    @Test
    fun testCollidesTransformed() {
        assertEquals(false, buildVectorPath(VectorPath()) {
            rect(0, 0, 15, 15)
        }.intersectsWith(Matrix(), path2, Matrix().scaled(2.0)))
        assertEquals(true, buildVectorPath(VectorPath()) {
            rect(0, 0, 15, 15)
        }.intersectsWith(Matrix().scaled(2.0, 2.0), path2, Matrix().scaled(2.0)))
        assertEquals(true, buildVectorPath(VectorPath()) {
            rect(0, 0, 15, 15)
        }.intersectsWith(Matrix().scaled(2.0, 2.0), path2, Matrix()))

        assertEquals(true, VectorPath.intersects(path1, Matrix(), path1, Matrix()))
        assertEquals(
            true,
            VectorPath.intersects(path1, Matrix().translated(101.0, 0.0), path1, Matrix().translated(101.0, 0.0))
        )
        assertEquals(
            true,
            VectorPath.intersects(path1, Matrix().translated(50.0, 0.0), path1, Matrix().translated(100.0, 0.0))
        )
        assertEquals(
            true,
            VectorPath.intersects(path1, Matrix().translated(100.0, 0.0), path1, Matrix().translated(50.0, 0.0))
        )
        assertEquals(false, VectorPath.intersects(path1, Matrix().translated(101.0, 0.0), path1, Matrix()))
        assertEquals(false, VectorPath.intersects(path1, Matrix(), path1, Matrix().translated(101.0, 0.0)))
    }

    @Test
    fun testVisitEdgesSimplified() {
        val log = arrayListOf<String>()
        buildVectorPath(VectorPath()) {
            moveTo(Point(100, 100))
            quadTo(Point(100, 200), Point(200, 200))
            close()
        }.visitEdgesSimple(
            { p0, p1 -> log.add("line(${p0.x.toInt()}, ${p0.y.toInt()}, ${p1.x.toInt()}, ${p1.y.toInt()})") },
            { p0, p1, p2, p3 -> log.add("cubic(${p0.x.toInt()}, ${p0.y.toInt()}, ${p1.x.toInt()}, ${p1.y.toInt()}, ${p2.x.toInt()}, ${p2.y.toInt()}, ${p3.x.toInt()}, ${p3.y.toInt()})") },
            { log.add("close") },
        )
        assertEquals(
            """
                cubic(100, 100, 100, 166, 133, 200, 200, 200)
                line(200, 200, 100, 100)
                close
            """.trimIndent(),
            log.joinToString("\n")
        )
    }

    @Test
    fun testCircleCurves() {
        assertEquals(
            listOf(
                listOf(
                    Bezier(Point(100.0, 0.0), Point(100.0, -55.23), Point(55.23, -100.0), Point(0.0, -100.0)),
                    Bezier(Point(0.0, -100.0), Point(-55.23, -100.0), Point(-100.0, -55.23), Point(-100.0, 0.0)),
                    Bezier(Point(-100.0, 0.0), Point(-100.0, 55.23), Point(-55.23, 100.0), Point(0.0, 100.0)),
                    Bezier(Point(0.0, 100.0), Point(55.23, 100.0), Point(100.0, 55.23), Point(100.0, 0.0)),
                )
            ),
            buildVectorPath { circle(Point(0, 0), 100f) }.toCurvesList()
                .map { it.beziers.map { it.roundDecimalPlaces(2) } }
        )
    }
}
