package com.soywiz.korge

import com.soywiz.korge.testing.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import org.junit.Test
import kotlin.test.*

class KorgeHeadlessJvmTest {
    @Test
    fun testHeadlessTest() {
        var wasCalled = false
        try {
            korgeOffscreenTest(width = 256, height = 256, bgcolor = Colors["#2b2b2b"]) {
                val image = solidRect(100, 100, Colors.RED) {
                    rotation = 16.degrees
                    anchor(.5, .5)
                    scale(.8)
                    position(128, 128)
                }
                val bmp = renderToBitmap(includeBackground = true)
                assertEquals(views.clearColor, bmp[0, 0])
                assertEquals(Colors.RED, bmp[128, 128])
                //bmp.showImageAndWait()
                //assertEquals()
                wasCalled = true
            }
        } finally {
            assertEquals(true, wasCalled)
        }
    }
}
