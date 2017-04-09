package com.soywiz.korge.service

import com.soywiz.korio.error.unsupported
import java.util.*

open class Share protected constructor() {
	suspend open fun shareMessage(title: String, message: String) {
	}

	companion object {
		operator fun invoke() = ServiceLoader.load(Share::class.java).firstOrNull() ?: unsupported("Not ${Share::class.java.name} implementation found")
	}
}
