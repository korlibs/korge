package korlibs.korge.gradle.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement

operator fun JsonElement.get(key: String): JsonElement = asJsonObject.get(key)
val JsonElement.list: JsonArray get() = asJsonArray

private val prettyGson by lazy { GsonBuilder().setPrettyPrinting().create() }
fun JsonElement.toStringPretty() = prettyGson.toJson(this)
