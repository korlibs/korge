package com.soywiz.korim.triangulate

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.bitmap.vector.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.triangle.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.triangle.poly2tri.*
import kotlin.test.*

class TriangulateTest {
    @Test
    fun testTriangulateSafe() {
        buildPath {
            rect(0, 0, 100, 100)
            rect(20, 20, 60, 60)
        }.also { path ->
            val triangles = path.triangulateSafe()
            //suspendTest { outputTriangles(triangles) }
            assertEquals(8, triangles.size)
            assertEquals(
                "[Triangle((0, 100), (20, 80), (100, 100)), Triangle((0, 100), (0, 0), (20, 80)), Triangle((0, 0), (20, 20), (20, 80)), Triangle((20, 20), (0, 0), (100, 0)), Triangle((80, 20), (20, 20), (100, 0)), Triangle((80, 80), (80, 20), (100, 0)), Triangle((80, 80), (100, 0), (100, 100)), Triangle((20, 80), (80, 80), (100, 100))]",
                triangles.getTriangles().toString()
            )
        }

        buildPath {
            rect(0, 0, 100, 100)
            rect(20, 20, 120, 60)
        }.also { path ->
            val triangles = path.triangulateSafe()
            //suspendTest { outputTriangles(triangles) }
            assertEquals(10, triangles.size)
            assertEquals(
                "[Triangle((0, 100), (20, 80), (100, 100)), Triangle((0, 100), (0, 0), (20, 80)), Triangle((0, 0), (20, 20), (20, 80)), Triangle((20, 20), (0, 0), (100, 0)), Triangle((20, 20), (100, 0), (100, 20)), Triangle((20, 80), (100, 80), (100, 100)), Triangle((20, 80), (100, 80), (100, 80)), Triangle((100, 80), (100, 80), (140, 20)), Triangle((100, 80), (140, 20), (140, 80)), Triangle((100, 80), (100, 20), (140, 20))]",
                triangles.getTriangles().toString()
            )
        }
    }

    @Suppress("unused")
    private suspend fun outputTriangles(triangles: TriangleList) {
        val image = NativeImage(512, 512).context2d {
            for (triangle in triangles.getTriangles()) {
                fillStroke(Colors.RED, Colors.BLUE) {
                    triangle(triangle.p0, triangle.p1, triangle.p2)
                }
            }
            //path(out.toVectorPath())
        }
        image.writeTo(localCurrentDirVfs["demo.png"], PNG)
    }
}
