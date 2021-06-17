package com.soywiz.korim.triangulate

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.bitmap.vector.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.shape.ops.internal.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.triangle.internal.*
import com.soywiz.korma.triangle.triangulate.*
import kotlin.test.*

class TriangulateTest {
    /*
    @Test
    fun test() = suspendTest {
        val path = buildPath {
            rect(0, 0, 100, 100)
            rect(20, 20, 60, 60)
        }
        val triangles = path.triangulateNew()
        println(triangles)
        val image = NativeImage(512, 512)
        image.context2d {
            for (triangle in triangles) {
                fill(Colors.RED) {
                    triangle(triangle.p0, triangle.p1, triangle.p2)
                }
            }
        }
        localCurrentDirVfs["demo.png"].writeBitmap(image, PNG)
    }
    */

    /*
    @Test
    fun test2() = suspendTest {
        val clipper = DefaultClipper()
        val path = buildPath {
            rect(0, 0, 100, 100)
            rect(20, 20, 120, 60)
            //rect(20, 20, 60, 60)
        }
        val polytree = PolyTree()
        clipper.addPaths(path.toClipperPaths(), Clipper.PolyType.SUBJECT, true)
        //clipper.execute(Clipper.ClipType.UNION, polytree)
        val out = clipper.executePaths(Clipper.ClipType.UNION)
        //clipper.execute(Clipper.ClipType.UNION, out)
        //assertEquals("...", out.toVectorPath().toString())
        val triangles = out.toVectorPath().triangulate()

        //val triangles = path.triangulate()

        //println(out.toVectorPath().toString())

        val image = NativeImage(512, 512).context2d {
            for (triangle in triangles) {
                fillStroke(Colors.RED, Colors.BLUE) {
                    triangle(triangle.p0, triangle.p1, triangle.p2)
                }
            }
            //path(out.toVectorPath())
        }
        image.writeTo(localCurrentDirVfs["demo.png"], PNG)
    }
     */
}
