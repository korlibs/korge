package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HEICTest {
    @Test
    fun test() = suspendTest {
        val heic = resourcesVfs["heic.heic"].readBitmap().toBMP32().premultiplied()
        val png = resourcesVfs["heic.heic.png"].readBitmap(PNG.toProps()).toBMP32().premultiplied()
        //localVfs("/tmp/heic.heic.png").writeBitmap(heic, PNG)
        assertEquals(true, Bitmap32.matches(heic, png))
    }
}
