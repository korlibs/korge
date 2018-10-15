package com.soywiz.korge.service

import com.soywiz.korinject.*
import com.soywiz.korio.error.*
import com.soywiz.korio.file.*
import com.soywiz.korui.*
import com.soywiz.korui.light.*
import com.soywiz.korui.ui.*
import kotlin.coroutines.*

//@Singleton
open class Browser(val injector: AsyncInjector) {
	//companion object {
	//	operator fun invoke() = Services.load(Browser::class.java).firstOrNull() ?: unsupported("Not ${Browser::class.java.name} implementation found")
	//}
	open suspend fun browse(url: String) {
		injector.get<Application>().light.openURL(url.toString())
	}

	suspend fun frame() = injector.getOrNull(Frame::class) ?: invalidOp("Frame not available at korge")

	open suspend fun openFile(filter: String = ""): VfsFile {
		return frame().dialogOpenFile(filter)
	}

	open suspend fun prompt(title: String, initialValue: String): String {
		return frame().prompt(title, initialValue)
	}

	open suspend fun alert(message: String) {
		frame().alert(message)
	}
}
