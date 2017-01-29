package com.soywiz.korge.service

import com.soywiz.korio.error.unsupported
import java.util.*

open class Browser protected constructor() {
	companion object {
		operator fun invoke() = ServiceLoader.load(Browser::class.java).firstOrNull() ?: unsupported("Not ${Browser::class.java.name} implementation found")
	}
}