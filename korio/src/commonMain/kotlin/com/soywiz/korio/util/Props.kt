package com.soywiz.korio.util

import com.soywiz.kds.iterators.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*

class Props(private val props: LinkedHashMap<String, String> = LinkedHashMap<String, String>()) : MutableMap<String, String> by props {
	companion object {
		fun load(str: String) = Props().apply { deserializeNew(str) }
	}

	/*
	operator fun contains(key: String): Boolean = key in props
	operator fun get(key: String): String? = props[key]
	operator fun set(key: String, value: String) = run { props[key] = value }

	fun clear() {
		props.clear()
	}
	*/

	fun deserializeAdd(str: String) {
		str.split("\n").fastForEach { line ->
			if (line.startsWith('#')) return@fastForEach
			if (line.isBlank()) return@fastForEach
			val parts = line.split('=', limit = 2)
			val key = parts[0].trim()
			val value = parts.getOrElse(1) { " " }.trim()
			props[key] = value
		}
	}

	fun deserializeNew(str: String) {
		clear()
		deserializeAdd(str)
	}

	fun serialize(): String = props.map { "${it.key}=${it.value}" }.joinToString("\n")
}

suspend fun VfsFile.loadProperties(charset: Charset = UTF8) = Props.load(this.readString(charset))
suspend fun VfsFile.saveProperties(props: Props, charset: Charset = UTF8) = this.writeString(props.serialize(), charset = charset)
