package com.soywiz.korio.dynamic.serialization

import com.soywiz.korio.dynamic.mapper.*
import com.soywiz.korio.serialization.json.*
import kotlin.reflect.*

inline fun <reified T : Any> Json.stringifyTyped(obj: T?, mapper: ObjectMapper, pretty: Boolean = false): String =
	stringify(mapper.toUntyped(T::class, obj), pretty = pretty)

inline fun <reified T : Any> Json.parseTyped(s: String, mapper: ObjectMapper): T = parseTyped(T::class, s, mapper)
inline fun <reified T : Any> String.fromJsonTyped(mapper: ObjectMapper): T = Json.parseTyped(T::class, this, mapper)

fun <T : Any> Json.parseTyped(clazz: KClass<T>, s: String, mapper: ObjectMapper): T = mapper.toTyped(clazz, parse(s))
fun Map<*, *>.toJsonTyped(mapper: ObjectMapper): String = Json.stringifyTyped(this, mapper)
