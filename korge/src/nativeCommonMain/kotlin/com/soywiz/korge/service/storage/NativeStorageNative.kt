package com.soywiz.korge.service.storage

actual object NativeStorage : IStorage {
	actual override fun set(key: String, value: String) {
		TODO()
	}

	actual override fun getOrNull(key: String): String? {
		TODO()
	}

	actual override fun remove(key: String) {
		TODO()
	}

	actual override fun removeAll() {
		TODO()
	}
}
