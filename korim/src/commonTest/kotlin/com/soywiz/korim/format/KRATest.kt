package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class KRATest {
    val formats = ImageFormats(KRA, PNG)

    @Test
    fun kraTest() = suspendTestNoBrowser {
        val output = resourcesVfs["krita1.kra"].readBitmapNoNative(formats)
        val expected = resourcesVfs["krita1.kra.png"].readBitmapNoNative(formats)
        //output.showImageAndWait()
        assertEquals(0, output.matchContentsDistinctCount(expected))
    }
}
