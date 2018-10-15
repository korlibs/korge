package com.soywiz.korge.service.storage

expect object NativeStorage : IStorage {
	override fun set(key: String, value: String)
	override fun getOrNull(key: String): String?
	override fun remove(key: String)
	override fun removeAll()
}
