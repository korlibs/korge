package com.soywiz.korge.ui

import com.soywiz.korge.view.*
import kotlin.reflect.*

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
class defaultElement<E>(val defaultValue: E) {

    fun View.getExtra(name: String): E? = extra?.get(name) as? E ?: parent?.getExtra(name)

    inline operator fun getValue(thisRef: View, property: KProperty<*>): E {
        val name = property.name
        return thisRef.getExtra(name) ?: defaultValue
    }

    inline operator fun setValue(thisRef: View, property: KProperty<*>, value: E) {
        if (thisRef.extra == null) thisRef.extra = LinkedHashMap()
        thisRef.extra?.set(property.name, value as Any?)
    }
}
