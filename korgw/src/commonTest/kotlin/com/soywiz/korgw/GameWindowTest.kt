package com.soywiz.korgw

import com.soywiz.korim.color.Colors
import com.soywiz.korim.vector.buildShape
import com.soywiz.korio.async.suspendTest
import com.soywiz.korma.geom.PointInt
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.vector.lineTo
import com.soywiz.korma.geom.vector.moveTo
import kotlin.test.Test
import kotlin.test.assertEquals

class GameWindowTest {
    @Test
    fun testCustomCursor() = suspendTest {
        val cursor = GameWindow.CustomCursor(buildShape {
            fill(Colors.RED) {
                moveTo(0, 0)
                lineTo(-32, -32)
                lineTo(+32, -32)
                close()
            }
        })
        val bitmap = cursor.createBitmap()
        assertEquals(Size(64, 32), bitmap.bitmap.size)
        assertEquals(PointInt(32, 31), bitmap.hotspot)
        //bitmap.bitmap.showImageAndWait()
    }
}
