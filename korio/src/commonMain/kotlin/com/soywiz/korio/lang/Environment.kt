package com.soywiz.korio.lang

import com.soywiz.kds.*
import kotlin.native.concurrent.*

internal expect object EnvironmentInternal {
	// Uses querystring on JS/Browser, and proper env vars in the rest
	operator fun get(key: String): String?

	fun getAll(): Map<String, String>
}

@ThreadLocal
private var customEnvironments: LinkedHashMap<String, String>? = null

interface Environment {
    operator fun get(key: String): String?
    operator fun set(key: String, value: String)
    fun getAll(): Map<String, String>
    companion object : Environment {
        // Uses querystring on JS/Browser, and proper env vars in the rest
        override operator fun get(key: String): String? = customEnvironments?.get(key) ?: EnvironmentInternal[key]
        operator override fun set(key: String, value: String) {
            if (customEnvironments != null) {
                customEnvironments = LinkedHashMap()
            }
            customEnvironments?.set(key, value)
        }

        override fun getAll(): Map<String, String> = (customEnvironments ?: mapOf()) + EnvironmentInternal.getAll()
    }
}

open class EnvironmentCustom(customEnvironments: Map<String, String> = LinkedHashMap()) : Environment {
    var customEnvironments = when (customEnvironments) {
        is MutableMap<*, *> -> customEnvironments as MutableMap<String, String>
        else -> customEnvironments.toMutableMap()
    }
    private val customEnvironmentsNormalized = customEnvironments.map { it.key.uppercase() to it.value }.toLinkedMap()
    fun String.normalized() = this.uppercase().trim()
    override operator fun get(key: String): String? = customEnvironmentsNormalized[key.normalized()]
    operator override fun set(key: String, value: String) {
        customEnvironments[key] = value
        customEnvironmentsNormalized[key.normalized()] = value
    }
    override fun getAll(): Map<String, String> = customEnvironments
}

fun Environment(envs: Map<String, String> = mapOf()) = EnvironmentCustom(envs)
fun Environment(vararg envs: Pair<String, String>) = EnvironmentCustom(envs.toMap())

fun Environment.expand(str: String): String {
    return str.replace(Regex("(~|%(\\w+)%)")) {
        val key = it.value.trim('%')
        when (key) {
            "~" -> {
                if (this["HOMEDRIVE"] != null && this["HOMEPATH"] != null) {
                    "${this["HOMEDRIVE"]}${this["HOMEPATH"]}"
                } else {
                    this["HOMEPATH"] ?: this["HOME"] ?: this["TEMP"] ?: this["TMP"] ?: "/tmp"
                }
            }
            else -> this[key]
        } ?: ""
    }
}
