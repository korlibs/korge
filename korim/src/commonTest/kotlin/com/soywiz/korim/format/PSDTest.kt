package com.soywiz.korim.format

import com.soywiz.korim.bitmap.matchContentsDistinctCount
import com.soywiz.korio.async.suspendTestNoBrowser
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.test.Test
import kotlin.test.assertEquals

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
