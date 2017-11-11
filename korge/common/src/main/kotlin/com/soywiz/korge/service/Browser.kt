package com.soywiz.korge.service

import com.soywiz.korio.error.invalidOp
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korinject.Singleton
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korui.light.defaultLight
import com.soywiz.korui.ui.Frame

@Singleton
open class Browser(val injector: AsyncInjector) {
	//companion object {
	//	operator fun invoke() = Services.load(Browser::class.java).firstOrNull() ?: unsupported("Not ${Browser::class.java.name} implementation found")
	//}
	open suspend fun browse(url: String) {
		defaultLight.openURL(url.toString())
	}

	open suspend fun openFile(filter: String = ""): VfsFile {
		val frame = injector.getOrNull(Frame::class) ?: invalidOp("Frame not available at korge")
		return frame.dialogOpenFile(filter)
	}
}
