package com.soywiz.korim.format

import com.soywiz.korim.bitmap.matchContentsDistinctCount
import com.soywiz.korio.async.suspendTestNoBrowser
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.test.Test
import kotlin.test.assertEquals

class KRATest {
    val formats = ImageFormats(KRA, PNG)

    @Test
    fun kraTest() = suspendTestNoBrowser {
        val output = resourcesVfs["krita1.kra"].readBitmap(formats)
        val expected = resourcesVfs["krita1.kra.png"].readBitmapNoNative(formats)
        //output.showImageAndWait()
        assertEquals(0, output.matchContentsDistinctCount(expected))
    }
}
