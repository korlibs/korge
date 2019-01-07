package com.soywiz.korge.service.storage

import com.soywiz.korio.lang.*

interface IStorage {
	operator fun set(key: String, value: String): Unit
	fun getOrNull(key: String): String?
	fun remove(key: String): Unit
	fun removeAll(): Unit
}

operator fun IStorage.contains(key: String): Boolean {
	return getOrNull(key) != null
}

operator fun IStorage.get(key: String): String {
	return getOrNull(key) ?: throw KeyNotFoundException(key)
}
