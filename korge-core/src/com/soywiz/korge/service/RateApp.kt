package com.soywiz.korge.service

import com.soywiz.korio.error.unsupported
import java.util.*

open class RateApp protected constructor() {
	companion object {
		operator fun invoke() = ServiceLoader.load(RateApp::class.java).firstOrNull() ?: unsupported("Not ${RateApp::class.java.name} implementation found")
	}
}