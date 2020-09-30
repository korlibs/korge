package com.soywiz.korge.gradle.util

import java.util.*
import kotlin.reflect.*

class WeakPropertyThis<T : Any, V>(val gen: T.() -> V) {
    val map = WeakHashMap<T, V>()

    operator fun getValue(obj: T, property: KProperty<*>): V = map.getOrPut(obj) { gen(obj) }
    operator fun setValue(obj: T, property: KProperty<*>, value: V) = run { map[obj] = value }
}
