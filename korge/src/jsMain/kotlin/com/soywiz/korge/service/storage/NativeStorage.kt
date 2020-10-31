package com.soywiz.korge.service.storage

import com.soywiz.korge.view.*
import kotlinx.browser.*

@JsName("Array")
private external class JsArray<T> {
    operator fun get(key: Int): T
    val length: Int
}

@JsName("Object")
private external object JsObject {
    fun keys(): JsArray<dynamic>
}

actual class NativeStorage actual constructor(val views: Views) : IStorage {
    override fun toString(): String = "NativeStorage(${toMap()})"

    actual fun keys(): List<String> {
        val keys = JsObject.keys()
        return (0 until keys.length).map { keys[it] }
    }

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
