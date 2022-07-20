package com.soywiz.korim.format

import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.util.OS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class NativeDecodingTest {
    val file = resourcesVfs["bubble-chat.9.png"]
    val colorPremult = Colors["#01010181"]
    val colorStraight = Colors["#02020281"]

    init {
        if (OS.isJsNodeJs) RegisteredImageFormats.register(PNG)
    }

    @Test
    fun testReadPremultiplied() = suspendTest {
        val image = file.readBitmapNative(premultiplied = true)
        if (image.premultiplied) {
            assertEquals(colorPremult to true, image.getRgba(34, 15) to image.premultiplied)
        }
    }

    @Test
    fun testReadNonPremultiplied() = suspendTest {
        val image = file.readBitmapNative(premultiplied = false)
        if (!image.premultiplied) {
            assertEquals(colorStraight to false, image.getRgba(34, 15) to image.premultiplied)
        }
    }

    @Test
    fun testReadAny() = suspendTest {
        val image = file.readBitmapNative(premultiplied = null)
        assertNotEquals(0, image.getRgba(34, 15).a)
    }

    @Test
    fun testReadAll() = suspendTest {
        for (premultiplied in listOf(true, false, null)) {
            val image = file.readBitmapNative(premultiplied = premultiplied)
            assertNotEquals(0, image.getRgba(34, 15).a)
            if (image.premultiplied) {
                assertEquals(colorPremult to true, image.getRgba(34, 15) to image.premultiplied)
            } else {
                assertEquals(colorStraight to false, image.getRgba(34, 15) to image.premultiplied)
            }
        }
    }
}
