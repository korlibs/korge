package com.soywiz.korge.service

import com.soywiz.korio.error.unsupported
import java.util.*

open class Store protected constructor() {
	companion object {
		operator fun invoke() = ServiceLoader.load(Store::class.java).firstOrNull() ?: unsupported("Not ${Store::class.java.name} implementation found")
	}
}
