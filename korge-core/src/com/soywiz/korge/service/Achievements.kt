package com.soywiz.korge.service

import com.soywiz.korio.error.unsupported
import java.util.*

open class Achievements protected constructor() {
	companion object {
		operator fun invoke() = ServiceLoader.load(Achievements::class.java).firstOrNull() ?: unsupported("Not ${Achievements::class.java.name} implementation found")
	}
}