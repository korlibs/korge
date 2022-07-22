package com.soywiz.korim.format

import com.soywiz.korim.atlas.readAtlas
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.asPremultiplied
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.util.OS
import com.soywiz.korma.geom.vector.rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class NativeDecodingTest {
    val file = resourcesVfs["bubble-chat.9.png"]
    val colorPremult = Colors["#01010181"]
    val colorStraight = Colors["#02020281"]

    init {
        if (OS.isJsNodeJs) RegisteredImageFormats.register(PNG)
    }

    @Test
    fun testNativePNGDecoding() = suspendTest {
        val file = resourcesVfs["pma/spineboy-pma.png"]
        val bmp1 = file.readBitmapNative(props = ImageDecodingProps(asumePremultiplied = true))
        val bmp2 = file.readBitmapNoNative(props = ImageDecodingProps(asumePremultiplied = true, format = PNG))
        val diff = Bitmap32.diff(bmp1, bmp2).sumOf { it.a + it.r + it.g + it.b }
        if (OS.isJsBrowserOrWorker) {
            // In JS has some small dis-adjustments because, to native read image pixels on JS we need Canvas,
            // and Canvas has a pre-multiplied alpha always, that leads to lossy rounding errors
            assertTrue { diff < 61000 } //
        } else {
            assertEquals(0, diff)
        }
    }

    @Test
    fun testReadAsumePremultiplied() = suspendTest {
        for (preferKotlinDecoder in listOf(false, true)) {
            val atlas = resourcesVfs["pma/spineboy-pma.atlas"]
                .readAtlas(ImageDecodingProps(asumePremultiplied = true, preferKotlinDecoder = preferKotlinDecoder))
            val bitmaps = atlas.textures.map { it.value.bmp }.distinct()
            assertEquals(1, bitmaps.size)
            val bitmap = bitmaps[0]
            val color1 = bitmap.getRgbaRaw(190, 50)
            val color = if (color1 == Colors["#1c2a6d6e"]) Colors["#1c296d6e"] else color1 // Accept premultiplication lossy rounding error: https://stackoverflow.com/questions/43412842/reconstruct-original-16-bit-raw-pixel-data-from-the-html5-canvas
            assertEquals(Colors["#1c296d6e"] to true, color to bitmap.premultiplied)
            //assertEquals(Colors["#1c2a6d6e"] to true, bitmaps[0].getRgba(190, 50) to bitmaps[0].premultiplied)
            //assertEquals(Colors["#1c2a6d6e"].asPremultiplied(), bitmaps[0].getRgbaPremultiplied(190, 50))
        }
    }

    @Test
    fun testReadPremultiplied() = suspendTest {
        val image = file.readBitmapNative(ImageDecodingProps(premultiplied = true))
        if (image.premultiplied) {
            assertEquals(colorPremult to true, image.getRgbaRaw(34, 15) to image.premultiplied)
        }
    }

    @Test
    fun testReadNonPremultiplied() = suspendTest {
        val image = file.readBitmapNative(ImageDecodingProps(premultiplied = false))
        if (!image.premultiplied) {
            assertEquals(colorStraight to false, image.getRgbaRaw(34, 15) to image.premultiplied)
        }
    }

    @Test
    fun testReadAny() = suspendTest {
        val image = file.readBitmapNative(ImageDecodingProps(premultiplied = null))
        assertNotEquals(0, image.getRgbaRaw(34, 15).a)
    }

    @Test
    fun testReadAll() = suspendTest {
        for (premultiplied in listOf(true, false, null)) {
            val image = file.readBitmapNative(ImageDecodingProps(premultiplied = premultiplied))
            val x = 34; val y = 15
            assertNotEquals(0, image.getRgbaRaw(x, y).a)
            if (image.premultiplied) {
                assertEquals(colorPremult to true, image.getRgbaRaw(x, y) to image.premultiplied)
            } else {
                assertEquals(colorStraight to false, image.getRgbaRaw(x, y) to image.premultiplied)
            }
            assertEquals(colorPremult.asPremultiplied(), image.getRgbaPremultiplied(x, y))
        }
    }

    @Test
    fun testCreateStraight() = _testCreate(premultiplied = false)
    @Test
    fun testCreatePremult() = _testCreate(premultiplied = true)
    @Test
    fun testCreateUndefined() = _testCreate(premultiplied = null)

    private fun _testCreate(premultiplied: Boolean?) = suspendTest {
        val bmp = NativeImage(16, 16, premultiplied).context2d {
            fill(Colors.RED.withAd(0.5)) {
                rect(0, 0, 16, 16)
            }
        }
        val x = 8
        val y = 8
        val color = bmp.getRgbaRaw(x, y)
        val colorPremult = Colors["#7f00007f"]
        val colorStraight = Colors["#ff00007f"]
        if (bmp.premultiplied) {
            assertEquals(colorPremult to true, color to bmp.premultiplied)
        } else {
            assertEquals(colorStraight to false, color to bmp.premultiplied)
        }
        assertEquals(colorPremult.asPremultiplied(), bmp.getRgbaPremultiplied(x, y))
        assertEquals(colorStraight, bmp.getRgba(x, y))
    }
}
