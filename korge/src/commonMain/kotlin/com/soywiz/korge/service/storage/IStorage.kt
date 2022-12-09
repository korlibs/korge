package com.soywiz.korge.service.storage

import com.soywiz.korio.lang.*

/** Defines a way of synchronously set and get persistent small values */
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

interface IStorageKey<T> {
    val isDefined: Boolean
    var value: T
}

class StorageKey<T>(val storage: IStorage, val key: String, val serialize: (T) -> String, val deserialize: (String) -> T) : IStorageKey<T> {
    override val isDefined: Boolean get() = storage.contains(key)
    override var value: T
        get() = deserialize(storage[key])
        set(value) {
            storage[key] = serialize(value)
        }
}

fun <T> IStorage.item(key: String, serialize: (T) -> String, deserialize: (String) -> T): StorageKey<T> = StorageKey(this, key, serialize, deserialize)
fun IStorage.itemBool(key: String): StorageKey<Boolean> = item(key, { "$it" }, { it.toBoolean() })
fun IStorage.itemInt(key: String): StorageKey<Int> = item(key, { "$it" }, { it.toInt() })
fun IStorage.itemDouble(key: String): StorageKey<Double> = item(key, { "$it" }, { it.toDouble() })
