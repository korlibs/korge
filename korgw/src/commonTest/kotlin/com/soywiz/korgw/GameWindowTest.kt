package com.soywiz.korgw

import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.showImageAndWait
import com.soywiz.korim.vector.buildShape
import com.soywiz.korim.vector.renderWithHotspot
import com.soywiz.korio.async.suspendTest
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.vector.*
import kotlin.test.Test

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
        bitmap.bitmap.showImageAndWait()
    }
}
