package com.soywiz.korio.lang

import kotlin.native.concurrent.*

internal expect object EnvironmentInternal {
	// Uses querystring on JS/Browser, and proper env vars in the rest
	operator fun get(key: String): String?

	fun getAll(): Map<String, String>
}

@ThreadLocal
private var customEnvironments: LinkedHashMap<String, String>? = null

object Environment {
    // Uses querystring on JS/Browser, and proper env vars in the rest
    operator fun get(key: String): String? = customEnvironments?.get(key) ?: EnvironmentInternal[key]
    operator fun set(key: String, value: String) {
        if (customEnvironments != null) {
            customEnvironments = LinkedHashMap()
        }
        customEnvironments?.set(key, value)
    }

    fun getAll(): Map<String, String> = (customEnvironments ?: mapOf()) + EnvironmentInternal.getAll()
}

fun Environment.expand(str: String): String {
    return str.replace(Regex("(~|%(\\w+)%)")) {
        val key = it.value.trim('%')
        when (key) {
            "~" -> this["HOMEPATH"] ?: this["HOME"] ?: this["TEMP"] ?: this["TMP"] ?: "/tmp"
            else -> this[key]
        } ?: ""
    }
}
