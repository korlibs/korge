package com.soywiz.korim.font

import com.soywiz.korim.bitmap.*
import kotlin.test.*

class NativeFontTest {
	@Test
	fun name() {
		val bmpFont = BitmapFont(SystemFont("Arial"), 64, CharacterSet("ABCDEFGHIJKLMNOPQRSTUVWXYZ"))
        // This check possible issues on native
        bmpFont.registerTemporarily {
            val bmp = Bitmap32(200, 200)
        }
		//bmp.drawText(bmpFont, "HELLO")
		//awtShowImage(bmp); Thread.sleep(10000)
	}
}
