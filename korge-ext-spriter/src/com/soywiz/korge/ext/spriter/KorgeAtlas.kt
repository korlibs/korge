package com.soywiz.korge.ext.spriter

import com.soywiz.korio.serialization.json.Json
import org.intellij.lang.annotations.Language

object KorgeAtlas {
	fun loadJsonSpriter(@Language("json") json: String) {
		val info = Json.decode(json) as Map<String, Any?>
		val meta = info["meta"] as Map<String, Any?>
		//val format = meta["format"] as String
		println(info)
	}
}
