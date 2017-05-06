package com.soywiz.korge.service

import com.soywiz.korio.inject.Singleton
import com.soywiz.korui.light.defaultLight
import java.net.URL

@Singleton
open class Browser {
	//companion object {
	//	operator fun invoke() = Services.load(Browser::class.java).firstOrNull() ?: unsupported("Not ${Browser::class.java.name} implementation found")
	//}
	open fun browse(url: URL) {
		defaultLight.openURL(url.toString())
	}
}
