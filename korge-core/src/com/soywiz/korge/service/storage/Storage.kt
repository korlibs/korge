package com.soywiz.korge.service.storage

import com.soywiz.korio.error.unsupported
import java.util.*

open class Storage protected constructor() {
	companion object {
		operator fun invoke() = ServiceLoader.load(Storage::class.java).firstOrNull() ?: unsupported("Not ${Storage::class.java.name} implementation found")
	}

	open operator fun set(key: String, value: String): Unit = TODO()
	open operator fun get(key: String): String = TODO()
	open fun remove(key: String): Unit = TODO()
	open fun removeAll(): Unit = TODO()
}
