package korlibs.image.format

import korlibs.memory.*
import korlibs.image.atlas.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.platform.*
import kotlin.test.*

class NativeDecodingTest {
    val file = resourcesVfs["bubble-chat.9.png"]
    val colorPremult: RGBAPremultiplied = Colors["#01010181"].asPremultiplied()
    val colorPremultAlt: RGBAPremultiplied = Colors["#00000081"].asPremultiplied()
    val colorStraight: RGBA = Colors["#02020281"]

    init {
        if (Platform.isJsNodeJs) RegisteredImageFormats.register(PNG)
    }

    @Test
    fun testNativePNGDecoding() = suspendTest {
        val file = resourcesVfs["pma/spineboy-pma.png"]
        val bmp1 = file.readBitmapNative(props = ImageDecodingProps(premultiplied = false, asumePremultiplied = true))
        val bmp2 = file.readBitmapNoNative(props = ImageDecodingProps(premultiplied = false, asumePremultiplied = true, format = PNG))
        val diff = Bitmap32.diff(bmp1, bmp2).premultiplied().sumOf { it.a + it.r + it.g + it.b }
        if (Platform.isJsBrowserOrWorker) {
            // In JS has some small dis-adjustments because, to native read image pixels on JS we need Canvas,
            // and Canvas has a pre-multiplied alpha always, that leads to lossy rounding errors
            assertTrue("diff=$diff < 61000") { diff < 61000 } //
        } else {
            assertEquals(0, diff)
        }
    }

    @Test
    fun testReadAsumePremultiplied() = suspendTest {
        RegisteredImageFormats.temporalRegister(PNG) {
            for (preferKotlinDecoder in listOf(false, true)) {
                val atlas = resourcesVfs["pma/spineboy-pma.atlas"]
                    .readAtlas(ImageDecodingProps(premultiplied = false, asumePremultiplied = true, preferKotlinDecoder = preferKotlinDecoder))
                val bitmaps = atlas.textures.map { it.value.bmp }.distinct()
                assertEquals(1, bitmaps.size)
                val bitmap = bitmaps[0]
                val color1 = bitmap.getRgbaRaw(190, 50)
                val color =
                    if (color1 == Colors["#1c2a6d6e"]) Colors["#1c296d6e"] else color1 // Accept premultiplication lossy rounding error: https://stackoverflow.com/questions/43412842/reconstruct-original-16-bit-raw-pixel-data-from-the-html5-canvas
                assertEquals(Colors["#1c296d6e"] to true, color to bitmap.premultiplied)
                //assertEquals(Colors["#1c2a6d6e"] to true, bitmaps[0].getRgba(190, 50) to bitmaps[0].premultiplied)
                //assertEquals(Colors["#1c2a6d6e"].asPremultiplied(), bitmaps[0].getRgbaPremultiplied(190, 50))
            }
        }
    }

    @Test
    fun testReadPremultiplied() = suspendTest {
        val image = file.readBitmapNative(ImageDecodingProps(premultiplied = true))
        //println((image as BitmapNativeImage).bitmap.premultiplied)
        if (image.premultiplied) {
            assertEquals(colorPremult to true, image.getRgbaPremultiplied(34, 15) to image.premultiplied)
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
                image.getRgbaRaw(x, y).asPremultiplied().also { col ->
                    assertTrue("$col != $colorPremult || $colorPremultAlt premultiplied=$premultiplied") { colorPremult == col || colorPremultAlt == col }
                }
            } else {
                assertEquals(colorStraight to false, image.getRgbaRaw(x, y) to image.premultiplied)
            }
            image.getRgbaPremultiplied(x, y).also { col ->
                assertTrue("$col != $colorPremult || $colorPremultAlt premultiplied=$premultiplied") { colorPremult == col || colorPremultAlt == col }
            }
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
        val colorPremult = Colors["#80000080"]
        val colorStraight = Colors["#ff000080"]
        if (bmp.premultiplied) {
            assertEquals(colorPremult to true, color to bmp.premultiplied)
        } else {
            assertEquals(colorStraight to false, color to bmp.premultiplied)
        }
        assertEquals(colorPremult.asPremultiplied(), bmp.getRgbaPremultiplied(x, y))
        assertEquals(colorStraight, bmp.getRgba(x, y))
    }

    @Test
    fun testNativeImageDecodedIsMutable() = suspendTest {
        assertTrue { resourcesVfs["kotlin32.png"].readBitmap().flipX() is NativeImage }
    }
}
