package com.soywiz.korio.lang

expect object Environment {
	// Uses querystring on JS/Browser, and proper env vars in the rest
	operator fun get(key: String): String?

	fun getAll(): Map<String, String>
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
