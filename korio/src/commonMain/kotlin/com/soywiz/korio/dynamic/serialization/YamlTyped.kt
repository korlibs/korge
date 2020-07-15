package com.soywiz.korio.dynamic.serialization

import com.soywiz.korio.dynamic.mapper.*
import com.soywiz.korio.serialization.yaml.*
import kotlin.reflect.*

inline fun <reified T : Any> Yaml.decodeToType(s: String, mapper: ObjectMapper): T =
	decodeToType(s, T::class, mapper)

@Suppress("UNCHECKED_CAST")
fun <T : Any> Yaml.decodeToType(s: String, clazz: KClass<T>, mapper: ObjectMapper): T =
	mapper.toTyped(clazz, decode(s))

