package com.soywiz.korio.lang

import kotlinx.cinterop.*
import platform.posix.*

internal actual object EnvironmentInternal {
	private fun getEnvs(): Map<String, String> {
		val out = LinkedHashMap<String, String>()
		val env = platform.posix.__environ
		var n = 0
		while (true) {
			val line = env?.get(n++)?.toKString()
			if (line == null || line.isNullOrBlank()) break
			val parts = line.split('=', limit = 2)
			out[parts[0]] = parts.getOrElse(1) { parts[0] }
		}
		return out
	}

	private val allEnvs: Map<String, String> by lazy { getEnvs() }
	private val allEnvsUpper: Map<String, String> by lazy { allEnvs.entries.associate { it.key.toUpperCase() to it.value } }

	//actual operator fun get(key: String): String? = platform.posix.getenv(key)?.toKString()
	actual operator fun get(key: String): String? = allEnvsUpper[key.toUpperCase()]
	actual fun getAll() = allEnvs
}
