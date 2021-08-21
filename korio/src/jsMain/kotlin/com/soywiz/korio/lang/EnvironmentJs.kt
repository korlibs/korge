package com.soywiz.korio.lang

import com.soywiz.korio.*
import com.soywiz.korio.net.*
import com.soywiz.korio.util.*
import kotlinx.browser.*

internal actual object EnvironmentInternal {
	val allEnvs: Map<String, String> = when {
		NodeDeno.available -> {
            val envs = NodeDeno.envs()
            jsObjectKeysArray(envs).associate { it to envs[it] }
        }
		else -> QueryString.decode((document.location?.search ?: "").trimStart('?')).map { it.key to (it.value.firstOrNull() ?: it.key) }.toMap()
	}
	val allEnvsUpperCase = allEnvs.map { it.key.toUpperCase() to it.value }.toMap()

	actual operator fun get(key: String): String? = allEnvsUpperCase[key.toUpperCase()]
	actual fun getAll(): Map<String, String> = allEnvs
}
