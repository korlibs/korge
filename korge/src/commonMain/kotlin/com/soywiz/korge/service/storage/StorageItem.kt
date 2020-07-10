package com.soywiz.korge.service.storage

import com.soywiz.korio.dynamic.mapper.*
import com.soywiz.korio.dynamic.serialization.*
import com.soywiz.korio.serialization.json.*
import kotlin.reflect.*

class StorageItem<T : Any>(val storage: IStorage, val clazz: KClass<T>, val key: String, val mapper: ObjectMapper, val gen: (() -> T)?) {
    val isDefined: Boolean get() = key in storage
    @Suppress("UNCHECKED_CAST")
    val realGen: (() -> T)? = when {
        gen != null -> gen
        else -> when (clazz) {
            Boolean::class -> ({ false } as (() -> T))
            Int::class -> ({ 0 } as (() -> T))
            Long::class -> ({ 0L } as (() -> T))
            Float::class -> ({ 0f } as (() -> T))
            Double::class -> ({ 0.0 } as (() -> T))
            String::class -> ({ "" } as (() -> T))
            else -> null
        }
    }
	var value: T
		set(value) { storage[key] = Json.stringify(mapper.toUntyped(clazz, value)) }
		get () {
			if (!isDefined) storage[key] = Json.stringify(mapper.toUntyped(clazz,
                realGen?.invoke()
                    ?: error("Can't find '$key' and no default generator was defined")
            ))
			return Json.parseTyped(clazz, storage[key], mapper)
		}

	fun remove() = storage.remove(key)

	inline operator fun getValue(thisRef: Any, property: KProperty<*>): T = value
	inline operator fun setValue(thisRef: Any, property: KProperty<*>, value: T): Unit { this.value = value }
}

inline fun <reified T : Any> IStorage.item(key: String, mapper: ObjectMapper = Mapper, noinline gen: (() -> T)? = null) = StorageItem(this, T::class, key, mapper, gen)
