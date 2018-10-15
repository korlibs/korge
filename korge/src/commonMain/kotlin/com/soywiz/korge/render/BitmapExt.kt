package com.soywiz.korge.render

import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*

fun Bitmap32.ensurePowerOfTwo(): Bitmap32 {
	return if (this.width.isPowerOfTwo && this.height.isPowerOfTwo) {
		this
	} else {
		val out = Bitmap32(this.width.nextPowerOfTwo, this.height.nextPowerOfTwo)
		out.put(this)
		out
	}
}
