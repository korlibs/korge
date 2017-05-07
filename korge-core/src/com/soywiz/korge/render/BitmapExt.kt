package com.soywiz.korge.render

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korma.numeric.isPowerOfTwo
import com.soywiz.korma.numeric.nextPowerOfTwo

fun Bitmap32.ensurePowerOfTwo(): Bitmap32 {
	if (this.width.isPowerOfTwo && this.height.isPowerOfTwo) {
		return this
	} else {
		val out = Bitmap32(this.width.nextPowerOfTwo, this.height.nextPowerOfTwo)
		out.put(this)
		return out
	}
}
