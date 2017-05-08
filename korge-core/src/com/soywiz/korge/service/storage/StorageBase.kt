package com.soywiz.korge.service.storage

import com.soywiz.korio.error.KeyNotFoundException
import com.soywiz.korio.service.Services

open class StorageBase protected constructor() : Services.Impl(), IStorage {
	val data = hashMapOf<String, String>()

	override operator fun set(key: String, value: String): Unit {
		data[key] = value
	}
	override fun getOrNull(key: String): String? {
		return data[key]
	}

	override fun remove(key: String): Unit {
		data.remove(key)
	}
	override fun removeAll(): Unit {
		data.clear()
	}
}
