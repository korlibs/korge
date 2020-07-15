package com.soywiz.korio.lang

import kotlinx.cinterop.*
import platform.posix.*
import platform.windows.*

actual object Environment {
	private fun readStringsz(ptr: CPointer<WCHARVar>?): List<String> {
		if (ptr == null) return listOf()
		var n = 0
		var lastP = 0
		val out = arrayListOf<String>()
		while (true) {
			val c: Int = ptr[n++].toInt()
			if (c == 0) {
				val startPtr: CPointer<WCHARVar> = (ptr + lastP)!!
				val str = startPtr.toKString()
				if (str.isEmpty()) break
				out += str
				lastP = n
			}
		}
		return out
	}

	private fun getEnvs(): Map<String, String> {
		val envs = GetEnvironmentStringsW() ?: return mapOf()
		val lines = readStringsz(envs)
		FreeEnvironmentStringsW(envs)
		return lines.map {
			val parts = it.split('=', limit = 2)
			parts[0] to parts.getOrElse(1) { parts[0] }
		}.toMap()
	}
	private val allEnvs: Map<String, String> by lazy { getEnvs() }
	private val allEnvsUpper: Map<String, String> by lazy { allEnvs.entries.associate { it.key.toUpperCase() to it.value } }

	//actual operator fun get(key: String): String? = platform.posix.getenv(key)?.toKString()
	actual operator fun get(key: String): String? = allEnvsUpper[key.toUpperCase()]
	actual fun getAll() = allEnvs
}