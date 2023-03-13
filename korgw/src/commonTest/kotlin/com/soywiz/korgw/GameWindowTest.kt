package com.soywiz.korgw

import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.*

class GameWindowTest {
    @Test
    fun testCustomCursor() = suspendTest {
        val cursor = GameWindow.CustomCursor(buildShape {
            fill(Colors.RED) {
                moveTo(Point(0, 0))
                lineTo(Point(-32, -32))
                lineTo(Point(+32, -32))
                close()
            }
        })
        val bitmap = cursor.createBitmap()
        assertEquals(SizeInt(64, 32), bitmap.bitmap.size)
        assertEquals(PointInt(32, 31), bitmap.hotspot)
        assertEquals(MPointInt(32, 31), bitmap.mhotspot)
        //bitmap.bitmap.showImageAndWait()
    }
}
