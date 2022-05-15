package com.soywiz.korge.render

import com.soywiz.korag.log.LogAG
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.sliceWithSize
import kotlin.test.Test
import kotlin.test.assertSame

class AgBitmapTextureManagerTest {
	@Test
	fun test() {
		val ag = LogAG()
		val tm = AgBitmapTextureManager(ag)
		val bmp1 = Bitmap32(32, 32)
		val slice1 = bmp1.sliceWithSize(0, 0, 16, 16)
		val slice2 = bmp1.sliceWithSize(16, 0, 16, 16)
		val tex1a = tm.getTexture(slice1)
		val tex1b = tm.getTexture(slice1)
		val tex2a = tm.getTexture(slice2)
		val tex1c = tm.getTexture(slice1)
		val tex2b = tm.getTexture(slice2)
		assertSame(tex1a, tex1b)
		assertSame(tex1a, tex1c)
		assertSame(tex2a, tex2b)
		tm.gc()
		val tex1AfterGc = tm.getTexture(slice1)
		//assertNotSame(tex1a, tex1AfterGc) // @TODO: Check this!
	}
}
