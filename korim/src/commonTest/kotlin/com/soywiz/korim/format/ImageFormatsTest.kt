package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korio.async.suspendTestNoBrowser
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageFormatsTest {
	val props = ImageDecodingProps(format = ImageFormats(PNG, SVG, ICO, TGA, BMP))

	//@Test
	//fun demo1() = imageTest {
	//  val tempVfs = LocalVfs("c:/temp/")
	//	tempVfs["1.png"].readBitmap().writeTo(tempVfs["1.out.png"])
	//	Bitmap32(32, 32, premultiplied = true) { x, y -> if ((x + y) % 2 == 0) Colors.RED else Colors.BLUE }.writeTo(tempVfs["red.png"])
	//	//println("ResourcesVfs.absolutePath:" + ResourcesVfs.absolutePath)
	//}


	@Test
	fun png8() = suspendTestNoBrowser {
		//println("ResourcesVfs.absolutePath:" + ResourcesVfs.absolutePath)
		val bitmap = resourcesVfs["kotlin8.png"].readBitmapNoNative(props)
		assertEquals("Bitmap8(190, 190, palette=32)", bitmap.toString())
	}

	@Test
	fun png24() = suspendTestNoBrowser {
		val bitmap = resourcesVfs["kotlin24.png"].readBitmapNoNative(props)
		//JailedLocalVfs("c:/temp/")["lol.png"].writeBitmap(bitmap, formats)
		//root["kotlin8.png"].writeBitmap()
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@Test
	fun bmp() = suspendTestNoBrowser {
		val bitmap = resourcesVfs["kotlin.bmp"].readBitmapNoNative(props)
		//JailedLocalVfs("c:/temp/")["lol.png"].writeBitmap(bitmap, formats)
		//root["kotlin8.png"].writeBitmap()
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
		//showImageAndWait(bitmap)
	}

    @Test
    fun bmp32() = suspendTestNoBrowser {
        val bitmap = resourcesVfs["kotlin32.bmp"].readBitmapNoNative(props)
        val expectedBitmap = resourcesVfs["kotlin32.png"].readBitmapNoNative(props)
        //JailedLocalVfs("c:/temp/")["lol.png"].writeBitmap(bitmap, formats)
        //root["kotlin8.png"].writeBitmap()
        assertEquals("Bitmap32(190, 190)", bitmap.toString())
        assertEquals(true, Bitmap32.matches(bitmap, expectedBitmap))
    }

    @Test
	fun png32Encoder() = suspendTestNoBrowser {
		val bitmap = resourcesVfs["kotlin24.png"].readBitmapNoNative(props)
		val data = PNG.encode(bitmap)
		val bitmap2 = PNG.decode(data)
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
		assertEquals("Bitmap32(190, 190)", bitmap2.toString())
		assertEquals(true, Bitmap32.matches(bitmap, bitmap2))
	}

	@Test
	fun png32EncoderPremultiplied() = suspendTestNoBrowser {
		val bitmapOriginal = resourcesVfs["kotlin32.png"].readBitmapNoNative(props).toBMP32()
		val bitmap = bitmapOriginal.premultiplied()
		//showImageAndWait(bitmap)
		val data = PNG.encode(bitmap)
		val bitmap2 = PNG.decode(data)
		//showImageAndWait(bitmap2)
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
		assertEquals("Bitmap32(190, 190)", bitmap2.toString())
		//showImageAndWait(Bitmap32.diff(bitmap, bitmap2))
		assertEquals(true, Bitmap32.matches(bitmapOriginal, bitmap2))
	}

	@Test
	fun png32() = suspendTestNoBrowser {
		val bitmap = resourcesVfs["kotlin32.png"].readBitmapNoNative(props)
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@Test
	fun tga() = suspendTestNoBrowser {
		val bitmap = resourcesVfs["kotlin.tga"].readBitmapNoNative(props)
        val expectedBitmap = resourcesVfs["kotlin24.png"].readBitmapNoNative(props)
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
        //bitmap.showImageAndWait()
        assertEquals(true, Bitmap32.matches(bitmap, expectedBitmap))
	}

    @Test
    fun tga32() = suspendTestNoBrowser {
        val bitmap = resourcesVfs["kotlin32.tga"].readBitmapNoNative(props)
        val expectedBitmap = resourcesVfs["kotlin32.png"].readBitmapNoNative(props)
        assertEquals("Bitmap32(190, 190)", bitmap.toString())
        //bitmap.showImageAndWait()
        assertEquals(true, Bitmap32.matches(bitmap, expectedBitmap))
    }

    @Test
	fun ico() = suspendTestNoBrowser {
		val bitmaps = resourcesVfs["icon.ico"].readBitmapListNoNative(props)
		assertEquals(
			"[Bitmap32(256, 256), Bitmap32(128, 128), Bitmap32(96, 96), Bitmap32(72, 72), Bitmap32(64, 64), Bitmap32(48, 48), Bitmap32(32, 32), Bitmap32(24, 24), Bitmap32(16, 16)]",
			bitmaps.toString()
		)
	}

	//@Test
	//fun huge() = imageTest {
	//	//Thread.sleep(10000)
	//	val bitmap = Bitmap32(8196, 8196)
	//	//val bitmap = Bitmap32(32, 32)
	//	//val bitmap = Bitmap32(1, 1)
	//	val data = PNG().encode(bitmap, props = ImageEncodingProps(quality = 0.0))
	//	val bitmap2 = PNG().decode(data)
	//	assertEquals("Bitmap32(8196, 8196)", bitmap.toString())
	//	//assertEquals("Bitmap32(8196, 8196)", bitmap2.toString())
	//}
}
