package com.soywiz.korge.service.storage

/** Cross-platform way of synchronously storing small data */
expect object NativeStorage : IStorage {
	override fun set(key: String, value: String)
	override fun getOrNull(key: String): String?
	override fun remove(key: String)
	override fun removeAll()
}
