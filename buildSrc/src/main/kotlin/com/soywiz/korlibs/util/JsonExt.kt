package com.soywiz.korlibs.util

import com.google.gson.*

operator fun JsonElement.get(key: String): JsonElement = asJsonObject.get(key)
val JsonElement.list: JsonArray get() = asJsonArray

private val prettyGson by lazy { GsonBuilder().setPrettyPrinting().create() }
fun JsonElement.toStringPretty() = prettyGson.toJson(this)
