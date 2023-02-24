package com.soywiz.korim.format

import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.stream.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class NativeEncodingTest {
    @Test
    fun test() = suspendTest {
        if (Platform.isJsNodeJs) RegisteredImageFormats.register(PNG)
        val bytes = nativeImageFormatProvider.encodeSuspend(Bitmap32(10, 10, Colors.RED), ImageEncodingProps("image.png"))
        assertEquals(MSize(10, 10), PNG.decodeHeader(bytes.openSync())!!.size)

        val image = nativeImageFormatProvider.decodeSuspend(bytes)
        assertEquals(Colors.RED, image.toBMP32()[0, 0])
    }
}
