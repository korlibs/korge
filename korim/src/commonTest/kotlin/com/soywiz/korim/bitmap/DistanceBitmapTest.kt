package com.soywiz.korim.bitmap

import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.util.toStringDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class DistanceBitmapTest {
    @Test
    fun test() = suspendTest {
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
}
