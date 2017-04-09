package com.soywiz.korge.service

import com.soywiz.korio.error.unsupported
import com.soywiz.korio.inject.AsyncDependency
import java.util.*

open class Ads protected constructor() : AsyncDependency {
	suspend override fun init() {
	}

	suspend open fun preload() {
	}

	suspend open fun showInterstial() {
	}

	companion object {
        operator fun invoke() = ServiceLoader.load(Ads::class.java).firstOrNull() ?: unsupported("Not ${Ads::class.java.name} implementation found")
    }
}
