package com.soywiz.korio.lang

import com.soywiz.korio.*
import com.soywiz.korio.net.*
import com.soywiz.korio.util.*
import kotlin.browser.*

actual object Environment {
	val allEnvs: Map<String, String> = when {
		OS.isJsNodeJs -> jsObjectKeysArray(process.env).associate { it to process.env[it] }
		else -> QueryString.decode((document.location?.search ?: "").trimStart('?')).map { it.key to (it.value.firstOrNull() ?: it.key) }.toMap()
	}
	val allEnvsUpperCase = allEnvs.map { it.key.toUpperCase() to it.value }.toMap()

	actual operator fun get(key: String): String? = allEnvsUpperCase[key.toUpperCase()]
	actual fun getAll(): Map<String, String> = allEnvs
}
