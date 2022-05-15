package com.soywiz.korge.logger

import com.soywiz.klogger.Logger
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.lang.Charset
import com.soywiz.korio.lang.UTF8
import kotlin.collections.LinkedHashMap
import kotlin.collections.MutableMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.getOrElse
import kotlin.collections.iterator
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.set

fun configureLoggerFromProperties(str: String) {
	val props = Props.load(str)
	for ((key, value) in props) {
		try {
			Logger(key).level = Logger.Level.valueOf(value.toUpperCase())
		} catch (e: Throwable) {
			e.printStackTrace()
		}
	}
}

suspend fun configureLoggerFromProperties(file: VfsFile) {
	//println("configureLoggerFromProperties:")
	try {
		configureLoggerFromProperties(file.readString())
	} catch (e: Throwable) {
		//println("Couldn't load Klogger configuration $file : ${e.message}")
	}
}


private class Props(private val props: LinkedHashMap<String, String> = LinkedHashMap<String, String>()) : MutableMap<String, String> by props {
	companion object {
		fun load(str: String) = Props().apply { deserializeNew(str) }
	}

	fun deserializeAdd(str: String) {
		for (line in str.split("\n")) {
			if (line.startsWith('#')) continue
			if (line.isBlank()) continue
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

private suspend fun VfsFile.loadProperties(charset: Charset = UTF8) = Props.load(this.readString(charset))
private suspend fun VfsFile.saveProperties(props: Props, charset: Charset = UTF8) = this.writeString(props.serialize(), charset = charset)
