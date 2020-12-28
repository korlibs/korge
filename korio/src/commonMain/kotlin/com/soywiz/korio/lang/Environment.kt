package com.soywiz.korio.lang

import kotlin.native.concurrent.*

internal expect object EnvironmentInternal {
	// Uses querystring on JS/Browser, and proper env vars in the rest
	operator fun get(key: String): String?

	fun getAll(): Map<String, String>
}

@ThreadLocal
private var customEnvironments = LinkedHashMap<String, String>()

object Environment {
    // Uses querystring on JS/Browser, and proper env vars in the rest
    operator fun get(key: String): String? = customEnvironments[key] ?: EnvironmentInternal[key]
    operator fun set(key: String, value: String) {
        customEnvironments[key] = value
    }

    fun getAll(): Map<String, String> = customEnvironments + EnvironmentInternal.getAll()
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
