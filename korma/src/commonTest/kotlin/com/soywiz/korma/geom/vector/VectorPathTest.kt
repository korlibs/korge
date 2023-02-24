package com.soywiz.korma.geom.vector

import com.soywiz.korma.geom.MMatrix
import com.soywiz.korma.geom.MRectangle
import com.soywiz.korma.geom.bezier.Bezier
import com.soywiz.korma.geom.shape.buildVectorPath
import kotlin.test.Test
import kotlin.test.assertEquals

class VectorPathTest {
    @Test
    fun testSimpleSquare() {
        val g = buildVectorPath {
            moveTo(0, 0)
            lineTo(100, 0)
            lineTo(100, 100)
            lineTo(0, 100)
            close()
        }

        assertEquals(true, g.containsPoint(50, 50))
        assertEquals(false, g.containsPoint(150, 50))
        assertEquals(MRectangle(0, 0, 100, 100), g.getBounds())
    }

    @Test
    fun testCircle() {
        val g = VectorPath()
        g.circle(0, 0, 100)
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
        g.moveTo(0, 0)
        g.lineTo(100, 0)
        g.lineTo(100, 100)
        g.lineTo(0, 100)
        g.close()

        g.moveTo(75, 25)
        g.lineTo(25, 25)
        g.lineTo(25, 75)
        g.lineTo(75, 75)
        g.close()

        assertEquals(false, g.containsPoint(50, 50))
        assertEquals(false, g.containsPoint(150, 50))
        assertEquals(true, g.containsPoint(20, 50))
        assertEquals(MRectangle(0, 0, 100, 100), g.getBounds())
        //g.filled(Context2d.Color(Colors.RED)).raster().showImageAndWaitExt()
    }

    @Test
    fun testRotatedSquare() {
        val vp = VectorPath().apply {
            // /\
            // \/
            moveTo(0, -50)
            lineTo(-50, 0)
            lineTo(0, +50)
            lineTo(+50, 0)
            lineTo(0, -50)
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
            assertEquals(true, it.containsPoint(10, 10)) // This is true in JS: var ctx = document.createElement('canvas').getContext('2d'); ctx.beginPath(), ctx.rect(0, 0, 100, 100), ctx.isPointInPath(100, 100)
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
            moveTo(1, 1)
            lineTo(2, 1)
            lineTo(2, 2)
            lineTo(1, 2)
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
            moveTo(-1, -1)
            lineTo(+1, -1)
            lineTo(+1, 0)
            lineTo(+1, +1)
            lineTo(-1, +1)
            lineTo(-1, 0)
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

    val path2b = path2.clone().applyTransform(MMatrix().scale(2.0))

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
        }.intersectsWith(MMatrix(), path2, MMatrix().scale(2.0)))
        assertEquals(true, buildVectorPath(VectorPath()) {
            rect(0, 0, 15, 15)
        }.intersectsWith(MMatrix().scale(2.0, 2.0), path2, MMatrix().scale(2.0)))
        assertEquals(true, buildVectorPath(VectorPath()) {
            rect(0, 0, 15, 15)
        }.intersectsWith(MMatrix().scale(2.0, 2.0), path2, MMatrix()))

        assertEquals(true, VectorPath.intersects(path1, MMatrix(), path1, MMatrix()))
        assertEquals(true, VectorPath.intersects(path1, MMatrix().translate(101.0, 0.0), path1, MMatrix().translate(101.0, 0.0)))
        assertEquals(true, VectorPath.intersects(path1, MMatrix().translate(50.0, 0.0), path1, MMatrix().translate(100.0, 0.0)))
        assertEquals(true, VectorPath.intersects(path1, MMatrix().translate(100.0, 0.0), path1, MMatrix().translate(50.0, 0.0)))
        assertEquals(false, VectorPath.intersects(path1, MMatrix().translate(101.0, 0.0), path1, MMatrix()))
        assertEquals(false, VectorPath.intersects(path1, MMatrix(), path1, MMatrix().translate(101.0, 0.0)))
    }

    @Test
    fun testVisitEdgesSimplified() {
        val log = arrayListOf<String>()
        buildVectorPath(VectorPath()) {
            moveTo(100, 100)
            quadTo(100, 200, 200, 200)
            close()
        }.visitEdgesSimple(
            { x0, y0, x1, y1 -> log.add("line(${x0.toInt()}, ${y0.toInt()}, ${x1.toInt()}, ${y1.toInt()})") },
            { x0, y0, x1, y1, x2, y2, x3, y3 -> log.add("cubic(${x0.toInt()}, ${y0.toInt()}, ${x1.toInt()}, ${y1.toInt()}, ${x2.toInt()}, ${y2.toInt()}, ${x3.toInt()}, ${y3.toInt()})") },
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
                    Bezier(100.0, 0.0, 100.0, -55.23, 55.23, -100.0, 0.0, -100.0),
                    Bezier(0.0, -100.0, -55.23, -100.0, -100.0, -55.23, -100.0, 0.0),
                    Bezier(-100.0, 0.0, -100.0, 55.23, -55.23, 100.0, 0.0, 100.0),
                    Bezier(0.0, 100.0, 55.23, 100.0, 100.0, 55.23, 100.0, 0.0),
                )
            ),
            buildVectorPath { circle(0, 0, 100) }.toCurvesList().map { it.beziers.map { it.roundDecimalPlaces(2) } }
        )
    }
}
