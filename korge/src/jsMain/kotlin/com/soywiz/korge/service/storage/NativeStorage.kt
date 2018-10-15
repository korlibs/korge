package com.soywiz.korge.service.storage

import kotlin.browser.*

actual object NativeStorage : IStorage {
	actual override fun set(key: String, value: String) {
		localStorage.setItem(key, value)
	}

	actual override fun getOrNull(key: String): String? {
		return localStorage.getItem(key)
	}

	actual override fun remove(key: String) {
		localStorage.removeItem(key)
	}

	actual override fun removeAll() {
		localStorage.clear()
	}
}
