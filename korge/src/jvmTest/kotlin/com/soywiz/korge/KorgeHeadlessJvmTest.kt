package com.soywiz.korge

import com.soywiz.klogger.*
import com.soywiz.korge.testing.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import org.junit.Test
import kotlin.test.*

class KorgeHeadlessJvmTest {
    val logger = Logger("KorgeHeadlessJvmTest")

    @Test
    fun testHeadlessTest() {
        if (Environment["DISABLE_HEADLESS_TEST"] == "true") return
        var wasCalled = false
        logger.info { "1" }
        korgeOffscreenTest(width = 256, height = 256, bgcolor = Colors["#2b2b2b"]) {
            val image = solidRect(100, 100, Colors.RED) {
                rotation = 16.degrees
                anchor(.5, .5)
                scale(.8)
                position(128, 128)
            }
            logger.info { "2" }
            val bmp = renderToBitmap(includeBackground = true)
            logger.info { "3" }
            assertEquals(views.clearColor, bmp[0, 0])
            assertEquals(Colors.RED, bmp[128, 128])
            logger.info { "4" }
            //bmp.showImageAndWait()
            //assertEquals()
            wasCalled = true
        }
        logger.info { "5" }
        assertEquals(true, wasCalled)
    }
}
