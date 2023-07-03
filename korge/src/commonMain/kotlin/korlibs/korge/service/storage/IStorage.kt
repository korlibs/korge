package korlibs.korge.service.storage

import korlibs.io.lang.*
import kotlin.reflect.*

/** Defines a way of synchronously set and get persistent small values */
interface IStorage {
	operator fun set(key: String, value: String): Unit
	fun getOrNull(key: String): String?
	fun remove(key: String): Unit
	fun removeAll(): Unit
}

interface IStorageWithKeys : IStorage {
    fun keys(): List<String>
    fun toMap(): Map<String, String?> = keys().associateWith { getOrNull(it) }

    override fun removeAll(): Unit {
        for (key in keys().toList()) {
            remove(key)
        }
    }
}

operator fun IStorage.contains(key: String): Boolean {
	return getOrNull(key) != null
}

operator fun IStorage.get(key: String): String {
	return getOrNull(key) ?: throw KeyNotFoundException(key)
}

interface IStorageKey<T> {
    operator fun getValue(t: Any?, property: KProperty<*>): T = value
    operator fun setValue(t: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }

    val isDefined: Boolean
    var value: T
}

class StorageKey<T>(val storage: IStorage, val key: String, val serialize: (T) -> String, val deserialize: (String?) -> T) : IStorageKey<T> {
    override val isDefined: Boolean get() = storage.contains(key)
    override var value: T
        get() = deserialize(storage.getOrNull(key))
        set(value) {
            storage[key] = serialize(value)
        }
}

fun <T> IStorage.item(key: String, serialize: (T) -> String, deserialize: (String?) -> T): StorageKey<T> = StorageKey(this, key, serialize, deserialize)
fun IStorage.itemString(key: String, default: String = ""): StorageKey<String> = item(key, { it }, { it ?: default })
fun IStorage.itemBool(key: String, default: Boolean = false): StorageKey<Boolean> = item(key, { "$it" }, { if (it != null) it.toBoolean() else default })
fun IStorage.itemInt(key: String, default: Int = 0): StorageKey<Int> = item(key, { "$it" }, { it?.toInt() ?: default })
fun IStorage.itemDouble(key: String, default: Double = 0.0): StorageKey<Double> = item(key, { "$it" }, { it?.toDouble() ?: default })
