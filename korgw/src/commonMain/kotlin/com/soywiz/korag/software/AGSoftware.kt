package com.soywiz.korag.software

import com.soywiz.korag.*
import com.soywiz.korim.bitmap.*

open class AGFactorySoftware() : AGFactory {
	override val supportsNativeFrame: Boolean = false
	override fun create(nativeControl: Any?, config: AGConfig): AG = AGSoftware(nativeControl as? Bitmap32 ?: Bitmap32(640, 480))
	override fun createFastWindow(title: String, width: Int, height: Int): AGWindow {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}

open class AGSoftware(val bitmap: Bitmap32) : AG() {
	override val nativeComponent: Any = bitmap

	init {
		ready()
	}
}
