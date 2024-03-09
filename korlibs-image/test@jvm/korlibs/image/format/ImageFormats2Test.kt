package korlibs.image.format

import korlibs.image.awt.*
import korlibs.image.bitmap.*
import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.file.std.*
import kotlin.test.*

class ImageFormats2Test {
    val formats = ImageFormats(PNG, ICO)
    //val root = ResourcesVfs
    lateinit var root: VfsFile

    fun imgTest(callback: suspend () -> Unit) = suspendTest {
        for (path in listOf(applicationVfs["src/test/resources"], resourcesVfs)) {
            root = path
            if (root["kotlin8.png"].exists()) break
        }
        callback()
    }

    @Test
    fun png8() = imgTest {
        val bitmap = root["kotlin8.png"].readBitmapNoNative(formats)
        assertEquals("Bitmap8(190, 190, palette=32)", bitmap.toString())
        //awtShowImage(bitmap); Thread.sleep(10000L)
    }

    @Test
    fun png24() = imgTest {
        val bitmap = root["kotlin24.png"].readBitmapNoNative(formats)
        assertEquals("Bitmap32(190, 190)", bitmap.toString())
        //val bitmap2 = bitmap.toBMP32().mipmap(2)
        //val bitmap2 = bitmap.toBMP32()
        //awtShowImage(bitmap2); Thread.sleep(10000L)
    }

    @Test
    fun lucida_console32_png() = imgTest {
        val bitmap = root["lucida_console32.png"].readBitmapNoNative(formats)
        assertEquals("Bitmap8(576, 512, palette=256)", bitmap.toString())
    }

    @Test
    fun mipmaps() = imgTest {
        val bitmap = root["kotlin24.png"].readBitmapNoNative(formats)
        assertEquals("Bitmap32(190, 190)", bitmap.toString())
        val bitmap2 = bitmap.toBMP32().mipmap(2)
        //bitmap2.showImageAndWait()
    }

    @Test
    fun png32() = imgTest {
        val bitmap = root["kotlin32.png"].readBitmapNoNative(formats)
        assertEquals("Bitmap32(190, 190)", bitmap.toString())
        //awtShowImage(bitmap); Thread.sleep(10000L)
        //bitmap.writeTo(LocalVfs("c:/temp/img1.jpg.png"), formats = formats)
    }

    @Test
    fun ico() = imgTest {
        val bitmaps = root["icon.ico"].readBitmapListNoNative(formats)
        assertEquals(
            "[Bitmap32(256, 256), Bitmap32(128, 128), Bitmap32(96, 96), Bitmap32(72, 72), Bitmap32(64, 64), Bitmap32(48, 48), Bitmap32(32, 32), Bitmap32(24, 24), Bitmap32(16, 16)]",
            bitmaps.toString()
        )
    }

    @Test
    fun pngInterlaced() = imgTest {
        val bitmap1 = root["icon0.png"].readBitmapNoNative(formats)
        val bitmap2 = root["icon0.deinterlaced.png"].readBitmapNoNative(formats)
        assertEquals(0, bitmap1.matchContentsDistinctCount(bitmap2))
        //bitmap1.writeTo(LocalVfs("c:/temp/demo1.png"), formats = formats)
        //bitmap2.writeTo(LocalVfs("c:/temp/demo2.png"), formats = formats)
    }

    @Test
    fun pngDemo() = imgTest {
        val bmp = root["kotlin32-extra-bytes.png"].readBitmapNoNative(formats)
    }
}
