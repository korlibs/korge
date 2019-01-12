package com.soywiz.korge.service.storage

import com.soywiz.korio.dynamic.mapper.*
import com.soywiz.korio.dynamic.serialization.*
import com.soywiz.korio.serialization.json.*
import kotlin.reflect.*

class StorageItem<T : Any>(val storage: IStorage, val clazz: KClass<T>, val key: String, val gen: () -> T) {
	var value: T
		set(value) = run { storage[key] = Json.stringify(Mapper.toUntyped(clazz, value)) }
		get () {
			if (key !in storage) storage[key] = Json.stringify(Mapper.toUntyped(clazz, gen()))
			return Json.parseTyped(clazz, storage[key], Mapper)
		}

	fun remove() = storage.remove(key)

	inline operator fun getValue(thisRef: Any, property: KProperty<*>): T = value
	inline operator fun setValue(thisRef: Any, property: KProperty<*>, value: T): Unit = run { this.value = value }
}

inline fun <reified T : Any> IStorage.item(key: String, noinline gen: () -> T) = StorageItem(this, T::class, key, gen)
