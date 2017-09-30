package com.soywiz.korge.service

import com.soywiz.korio.inject.Singleton

@Singleton
open class Share {
	suspend open fun shareMessage(title: String, message: String) {
	}

	//companion object {
	//	operator fun invoke() = Services.load(Share::class.java).firstOrNull() ?: unsupported("Not ${Share::class.java.name} implementation found")
	//}
}
