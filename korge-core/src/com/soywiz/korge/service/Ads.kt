package com.soywiz.korge.service

import com.soywiz.korio.inject.Singleton

@Singleton
open class Ads() {
	suspend fun preload() {
	}

	suspend fun showInterstial() {
	}

	//companion object {
	//    operator fun invoke() = Services.load(Ads::class.java).firstOrNull() ?: unsupported("Not ${Ads::class.java.name} implementation found")
	//}
}
