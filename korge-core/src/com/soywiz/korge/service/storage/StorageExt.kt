package com.soywiz.korge.service.storage

import com.soywiz.korio.serialization.json.Json
import kotlin.reflect.KProperty

class StorageItem<T>(val storage: Storage, val clazz: Class<T>, val key: String, val gen: () -> T) {
	var value: T; set(value) = run { storage[key] = Json.encode(value) }; get () {
		if (key !in storage) storage[key] = Json.encode(gen())
		return Json.decodeToType(storage[key], clazz)
	}

	fun remove() = storage.remove(key)

	inline operator fun getValue(thisRef: Any, property: KProperty<*>): T = value
	inline operator fun setValue(thisRef: Any, property: KProperty<*>, value: T): Unit = run { this.value = value }
}

inline fun <reified T> Storage.item(key: String, noinline gen: () -> T) = StorageItem(this, T::class.java, key, gen)

class StorageDelegate {

}
