package com.soywiz.korim.bitmap

import com.soywiz.klogger.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.Test
import kotlin.test.assertEquals

class DistanceBitmapTest {
    val logger = Logger("DistanceBitmapTest")

    @Test
    fun test1() = suspendTest {
        //val bmp = Bitmap32(256, 256) { _, _ -> Colors.TRANSPARENT_BLACK }
        val bmp = Bitmap1(7, 7)
        //val bmp = Bitmap1(256, 256)
        //val bmp = Bitmap1(16, 16)
        bmp[bmp.width / 2, bmp.height / 2] = 1
        //bmp[bmp.width / 3, bmp.height / 3] = 1
        //bmp[(bmp.width * 0.7).toInt(), (bmp.height * 0.9).toInt()] = 1
        val distanceBmp = bmp.distanceMap()
        assertEquals(
            """
                3.6,2.8,2.2,2.0,2.2,2.8,3.6
                2.8,2.2,1.4,1.0,1.4,2.2,2.8
                2.2,1.4,1.0,0.0,1.0,1.4,2.2
                2.0,1.0,0.0,0.0,0.0,1.0,2.0
                2.2,1.4,1.0,0.0,1.0,1.4,2.2
                2.8,2.2,1.4,1.0,1.4,2.2,2.8
                3.6,2.8,2.2,2.0,2.2,2.8,3.6
            """.trimIndent(),
            (0 until distanceBmp.height).joinToString("\n") { y -> (0 until distanceBmp.width).joinToString(",") { x -> distanceBmp.getDist(x, y).toStringDecimal(1) } }.replace("-", "")
        )
        //distanceBmp.toNormalizedDistanceBitmap8().showImageAndWait()
    }

    @Test
    fun test2() = suspendTest {
        val bmp = Bitmap1.fromString(
            """
            ..........
            ...X......
            ...XXXXX..
            ...XXXXX..
            ...XXXXX..
            ...XXXXX..
            .....X....
            ..........
        """.trimIndent())
        val distance = bmp.sdf()
        val floats = distance.toFloatArray2()

        assertEquals(
            """
                2.2, 1.4, 1, 0, 1, 1, 1, 1, 1.4, 2.2
                2, 1, 0, 0, 0, 0, 0, 0, 1, 1.4
                2, 1, 0, 0, 0, 0, 0, 0, 0, 1
                2, 1, 0, 0, -1, -1, -1, 0, 0, 1
                2, 1, 0, 0, -1, -1.4, -1, 0, 0, 1
                2, 1, 0, 0, 0, -1, 0, 0, 0, 1
                2.2, 1.4, 1, 0, 0, 0, 0, 0, 1, 1.4
                2.8, 2.2, 1.4, 1, 1, 0, 1, 1, 1.4, 2.2
            """.trimIndent(),
            (0 until floats.height).joinToString("\n") { y ->
                (0 until floats.width).joinToString(", ") { x ->
                    floats[x, y].niceStr(1)
                }
            }
        )


        //Bezier(0f, 0f, 1f, 2f).project()
        //println(floats)
        //bmp.showImageAndWait()
    }


    @Test
    fun testVectorSDF() {
        val path = buildVectorPath {
            this.circle(5, 5, 5)
        }
        val sdf = path.sdf(10, 10)
        val msdf = path.msdf(10, 10)
        logger.debug { sdf }
        logger.debug { msdf }
    }
}
