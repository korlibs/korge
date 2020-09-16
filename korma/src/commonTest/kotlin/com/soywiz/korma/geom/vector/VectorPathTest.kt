package com.soywiz.korma.geom.vector

import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.Test
import kotlin.test.assertEquals

class VectorPathTest {
    @Test
    fun testSimpleSquare() {
        val g = VectorPath()
        g.moveTo(0, 0)
        g.lineTo(100, 0)
        g.lineTo(100, 100)
        g.lineTo(0, 100)
        g.close()

        assertEquals(true, g.containsPoint(50, 50))
        assertEquals(false, g.containsPoint(150, 50))
        assertEquals(Rectangle(0, 0, 100, 100), g.getBounds())
    }

    @Test
    fun testCircle() {
        val g = VectorPath()
        g.circle(0, 0, 100)
        println(g.readStats())
        //println(g.numberOfIntersections(0, 0))
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
        assertEquals(Rectangle(0, 0, 100, 100), g.getBounds())
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
        buildPath { rect(0, 0, 10, 10) }.also {
            assertEquals(true, it.containsPoint(5, 5))
            assertEquals(false, it.containsPoint(-1, -1))
            assertEquals(false, it.containsPoint(10, 10))
        }
        buildPath(winding = Winding.NON_ZERO) {
            rect(0, 0, 10, 10)
            rect(20, 0, 10, 10)
        }.also {
            assertEquals(true, it.containsPoint(5, 5))
            assertEquals(true, it.containsPoint(25, 5))
            assertEquals(false, it.containsPoint(-1, -1))
            assertEquals(false, it.containsPoint(10, 10))
            assertEquals(false, it.containsPoint(19, 5))
        }
        buildPath(winding = Winding.EVEN_ODD) {
            rect(0, 0, 20, 10)
            rect(10, 0, 20, 10)
        }.also {
            // [0-10] [10-20] [20-30]
            assertEquals(true, it.containsPoint(5, 5))
            assertEquals(false, it.containsPoint(15, 5))
            assertEquals(true, it.containsPoint(25, 5))
        }

        buildPath(winding = Winding.NON_ZERO) {
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
        buildPath(winding = Winding.NON_ZERO) {
            moveTo(1, 1)
            lineTo(2, 1)
            lineTo(2, 2)
            lineTo(1, 2)
            close()
        }.also {
            assertEquals(false, it.containsPoint(0.99, 0.99))
            assertEquals(false, it.containsPoint(0.9999, 0.9999))
            assertEquals(true, it.containsPoint(1, 1))
            assertEquals(true, it.containsPoint(1.1, 1.1))
            assertEquals(true, it.containsPoint(1.9, 1.9))
            //assertEquals(true, it.containsPoint(2, 2)) // @TODO: This is true on JS
            assertEquals(false, it.containsPoint(2.01, 2.01))
            assertEquals(false, it.containsPoint(2.1, 2.1))
            assertEquals(false, it.containsPoint(0, 0))
        }

        buildPath(winding = Winding.EVEN_ODD) {
            moveTo(-1, -1)
            lineTo(+1, -1)
            lineTo(+1, 0)
            lineTo(+1, +1)
            lineTo(-1, +1)
            lineTo(-1, 0)
            close()
        }.also {
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

    val path1 = buildPath { rect(0, 0, 100, 100) }
    val path2 = buildPath { rect(10, 10, 150, 80) }
    val path3 = buildPath { rect(110, 0, 100, 100) }

    val path2b = path2.clone().applyTransform(Matrix().scale(2.0))

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
        assertEquals(false, buildPath { rect(0, 0, 15, 15) }.intersectsWith(Matrix(), path2, Matrix().scale(2.0)))
        assertEquals(true, buildPath { rect(0, 0, 15, 15) }.intersectsWith(Matrix().scale(2.0, 2.0), path2, Matrix().scale(2.0)))
        assertEquals(true, buildPath { rect(0, 0, 15, 15) }.intersectsWith(Matrix().scale(2.0, 2.0), path2, Matrix()))

        assertEquals(true, VectorPath.intersects(path1, Matrix(), path1, Matrix()))
        assertEquals(true, VectorPath.intersects(path1, Matrix().translate(101.0, 0.0), path1, Matrix().translate(101.0, 0.0)))
        assertEquals(true, VectorPath.intersects(path1, Matrix().translate(50.0, 0.0), path1, Matrix().translate(100.0, 0.0)))
        assertEquals(true, VectorPath.intersects(path1, Matrix().translate(100.0, 0.0), path1, Matrix().translate(50.0, 0.0)))
        assertEquals(false, VectorPath.intersects(path1, Matrix().translate(101.0, 0.0), path1, Matrix()))
        assertEquals(false, VectorPath.intersects(path1, Matrix(), path1, Matrix().translate(101.0, 0.0)))
    }
}
