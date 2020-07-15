package com.soywiz.korio.lang

import kotlinx.cinterop.*
import platform.Foundation.*

actual object Environment {
	val allEnvs: Map<String, String> by lazy { kotlinx.cinterop.autoreleasepool { platform.Foundation.NSProcessInfo.processInfo.environment.map { it.key.toString() to it.value.toString() }.toMap() } }
	val allEnvsUpper: Map<String, String> by lazy { allEnvs.map { it.key.toUpperCase() to it.value }.toMap() }

	//actual operator fun get(key: String): String? = platform.posix.getenv(key)?.toKString()
	actual operator fun get(key: String): String? = allEnvsUpper[key.toUpperCase()]
	actual fun getAll() = allEnvs
}
