package com.soywiz.korge.service

import com.soywiz.korio.error.unsupported
import java.util.*

open class Ads protected constructor() {
	companion object {
		operator fun invoke() = ServiceLoader.load(Ads::class.java).firstOrNull() ?: unsupported("Not ${Ads::class.java.name} implementation found")
	}
}