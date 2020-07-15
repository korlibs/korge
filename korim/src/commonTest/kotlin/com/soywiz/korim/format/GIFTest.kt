package com.soywiz.korim.format

import com.soywiz.klock.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class GIFTest {
    @Test
    fun test() = suspendTestNoBrowser {
        val data = resourcesVfs["small-animated-gif-images-2.gif"].readImageData(GIF)
        assertEquals(500, data.width)
        assertEquals(500, data.height)
        assertEquals(3, data.frames.size)
        assertEquals(10.milliseconds, data.frames[0].duration)
        assertEquals(10.milliseconds, data.frames[1].duration)
        assertEquals(10.milliseconds, data.frames[2].duration)
        //for (frame in data.frames) frame.bitmap.showImageAndWait()
    }
}
