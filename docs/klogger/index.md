---
layout: default
title: "Klogger"
fa-icon: fa-bell
priority: 60
---

[https://github.com/korlibs/klogger](https://github.com/korlibs/klogger)

Klogger is a logger library for multiplatform Kotlin.

This library provides a simple interface to do logging into suitable outputs like javascript's console or stdout/stderr.

{% include stars.html project="klogger" %}

{% include toc_include.md %}

{% include using_with_gradle.md name="klogger" %}

## Console

Klogger provides a common way for logging into JavaScript console with fallback to stdout and stderr.

```kotlin
object Console {
    fun error(vararg msg: Any?): Unit
    fun log(vararg msg: Any?): Unit
}
```

## Logging

Klogger provides a simple yet configurable interface for logging.

```kotlin
// Logger construction (returns the same instance for the same name)
fun Logger(name: String)

// Default Level and output for loggers that do not define this
var Logger.Companion.defaultLevel: Level?
var Logger.Companion.defaultOutput: Output

class Logger {
    // Output for this logger
    var output: Output
    fun setOutput(output: Logger.Output): Logger
    val isLocalOutputSet: Boolean

    // Log level for this logger
    var level: Level
    val isLocalLevelSet: Boolean
    fun setLevel(level: Logger.Level): Logger
    fun isEnabled(level: Level): Boolean
    inline val isFatalEnabled: Boolean
    inline val isErrorEnabled: Boolean
    inline val isWarnEnabled: Boolean
    inline val isInfoEnabled: Boolean
    inline val isDebugEnabled: Boolean
    inline val isTraceEnabled: Boolean

    // Emit logs
    inline fun log(level: Level, msg: () -> Any?)
    inline fun fatal(msg: () -> Any?)
    inline fun error(msg: () -> Any?)
    inline fun warn(msg: () -> Any?)
    inline fun info(msg: () -> Any?)
    inline fun debug(msg: () -> Any?)
    inline fun trace(msg: () -> Any?)
}

enum class Logger.Level(val index: Int) {
    NONE(0), FATAL(1), ERROR(2),
    WARN(3), INFO(4), DEBUG(5), TRACE(6)
}

interface Logger.Output {
    fun output(logger: Logger, level: Logger.Level, msg: Any?)
}

object Logger.ConsoleLogOutput : Logger.Output

```

### Externally configuring loggers

Since klogger does not have dependencies, by default it doesn't configure the loggers from any config file or environment. But here you have some ideas for configuring the loggers without recompiling:

```kotlin
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
	println("configureLoggerFromProperties:")
	try {
		configureLoggerFromProperties(file.readString())
	} catch (e: Throwable) {
		println("Couldn't load Klogger configuration $file : ${e.message}")
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
```
