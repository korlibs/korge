package com.soywiz.korge.service.storage.js

import com.jtransc.JTranscSystem
import com.jtransc.js.call
import com.jtransc.js.get
import com.jtransc.js.toJavaStringOrNull
import com.jtransc.js.window
import com.soywiz.korge.service.storage.StorageBase

class JsStorage : StorageBase() {
	override val available: Boolean = JTranscSystem.isJs()

	val localStorage get() = window["localStorage"]

	override fun set(key: String, value: String) {
		localStorage.call("setItem", key, value)
	}

	override fun getOrNull(key: String): String? {
		return localStorage.call("getItem", key)?.toJavaStringOrNull()
	}

	override fun remove(key: String) {
		localStorage.call("removeItem", key)
	}

	override fun removeAll() {
		localStorage.call("clear")
	}
}
