package com.soywiz.korge.service.storage

import com.soywiz.korio.error.KeyNotFoundException
import com.soywiz.korio.error.unsupported
import java.util.*

open class Storage protected constructor() {
	companion object {
		operator fun invoke() = ServiceLoader.load(Storage::class.java).firstOrNull() ?: unsupported("Not ${Storage::class.java.name} implementation found")
	}

	val data = hashMapOf<String, String>()

	open operator fun set(key: String, value: String): Unit {
		data[key] = value
	}
	open operator fun contains(key: String): Boolean {
		return key in data
	}
	open operator fun get(key: String): String {
		return data[key] ?: throw KeyNotFoundException(key)
	}
	open fun remove(key: String): Unit {
		data.remove(key)
	}
	open fun removeAll(): Unit {
		data.clear()
	}
}
