package com.soywiz.korgw

import com.soywiz.korim.color.Colors
import com.soywiz.korim.vector.buildShape
import com.soywiz.korio.async.suspendTest
import com.soywiz.korma.geom.MPointInt
import com.soywiz.korma.geom.MSize
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
        assertEquals(MSize(64, 32), bitmap.bitmap.size)
        assertEquals(MPointInt(32, 31), bitmap.hotspot)
        //bitmap.bitmap.showImageAndWait()
    }
}
