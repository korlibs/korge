package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class PSDTest {
    val formats = ImageFormats(PNG, PSD)

    @Test
    fun psdTest() = suspendTestNoBrowser {
        val output = resourcesVfs["small.psd"].readBitmapNoNative(formats)
        val expected = resourcesVfs["small.psd.png"].readBitmapNoNative(formats)
        //showImageAndWait(output)
        assertEquals(0, output.matchContentsDistinctCount(expected))
    }
}
