package com.soywiz.korag

import com.soywiz.kgl.*

actual object AGOpenglFactory {
	actual fun create(nativeComponent: Any?): AGFactory = AGFactoryNative
	actual val isTouchDevice: Boolean = false
}

object AGFactoryNative : AGFactory {
	override val supportsNativeFrame: Boolean = false
	override fun create(nativeControl: Any?, config: AGConfig): AG = AGNative()
	override fun createFastWindow(title: String, width: Int, height: Int): AGWindow = TODO()
}

open class AGNative(override val gles: Boolean = false) : AGOpengl() {
	override val nativeComponent = Any()
	override val gl: KmlGl = com.soywiz.kgl.KmlGlNative()
}
