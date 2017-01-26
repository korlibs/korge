package com.soywiz.korge.service

import com.soywiz.korio.error.unsupported
import java.util.*

open class Analytics protected constructor() {
	companion object {
		operator fun invoke() = ServiceLoader.load(Analytics::class.java).firstOrNull() ?: unsupported("Not ${Analytics::class.java.name} implementation found")
	}
}