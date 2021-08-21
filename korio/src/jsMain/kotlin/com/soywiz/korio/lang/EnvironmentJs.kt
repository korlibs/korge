package com.soywiz.korio.lang

import com.soywiz.korio.*
import com.soywiz.korio.net.*
import com.soywiz.korio.util.*
import kotlinx.browser.*

internal actual object EnvironmentInternal {
	actual operator fun get(key: String): String? = jsRuntime.env(key)
	actual fun getAll(): Map<String, String> = jsRuntime.envs()
}
