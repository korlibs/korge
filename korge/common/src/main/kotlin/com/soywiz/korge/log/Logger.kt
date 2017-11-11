package com.soywiz.korge.log

import com.soywiz.korinject.Singleton

@Singleton
open class Logger {
	enum class Level {
		DEBUG, INFO, WARNING, ERROR
	}

	fun debug(msg: String) = log(Level.DEBUG, msg)
	fun info(msg: String) = log(Level.INFO, msg)
	fun warning(msg: String) = log(Level.WARNING, msg)
	fun error(msg: String) = log(Level.ERROR, msg)

	open fun log(level: Level, msg: String) {
	}
}

fun Logger(handler: (level: Logger.Level, msg: String) -> Unit ) = object : Logger() {
	override fun log(level: Level, msg: String) = handler(level, msg)
}

fun Logger(handler: (msg: String) -> Unit ) = object : Logger() {
	override fun log(level: Level, msg: String) = handler(msg)
}
