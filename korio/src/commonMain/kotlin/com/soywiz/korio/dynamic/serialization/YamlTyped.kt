package com.soywiz.korio.dynamic.serialization

import com.soywiz.korio.dynamic.mapper.ObjectMapper
import com.soywiz.korio.serialization.yaml.Yaml
import kotlin.reflect.KClass

inline fun <reified T : Any> Yaml.decodeToType(s: String, mapper: ObjectMapper): T =
	decodeToType(s, T::class, mapper)

@Suppress("UNCHECKED_CAST")
fun <T : Any> Yaml.decodeToType(s: String, clazz: KClass<T>, mapper: ObjectMapper): T =
	mapper.toTyped(clazz, decode(s))

