package com.soywiz.korio.lang

import com.soywiz.kds.*
import com.soywiz.korio.file.*

expect object SystemProperties : Properties

open class Properties(map: Map<String, String>? = null) {
    //private val map = FastStringMap<String>()
    // This is required to work with K/N memory model
    private val map = CopyOnWriteFrozenMap<String, String>().also {
        if (map != null) it.putAll(map)
    }

    open operator fun contains(key: String): Boolean = get(key) != null
    open operator fun get(key: String): String? = map[key]
    open operator fun set(key: String, value: String): Unit { map[key] = value }
    open fun setAll(values: Map<String, String>): Unit {
        for ((key, value) in values) set(key, value)
    }
    open fun remove(key: String): Unit { map.remove(key) }
    open fun getAll(): Map<String, String> = map.toMap()

    companion object {
        fun parseString(data: String): Properties {
            val props = LinkedHashMap<String, String>()
            for (line in data.lines()) {
                val (rline) = line.trim().split('#')
                if (rline.isEmpty()) continue
                val key = rline.substringBefore('=', "").trim()
                val value = rline.substringAfter('=', "").trim()
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    props[key] = value
                }
            }
            return Properties(props)
        }
    }
}

suspend fun VfsFile.readProperties(charset: Charset = Charsets.UTF8) = Properties.parseString(readString(charset))
