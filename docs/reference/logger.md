---
permalink: /logger/
group: reference
layout: default
title: "Logger"
fa-icon: fa-bell
priority: 60
artifact: 'com.soywiz.korge:korge-foundation'
package: korlibs.logger
---

This module provides a simple interface to do logging into suitable outputs like javascript's console or stdout/stderr.

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

## AnsiEscape

Klogger exposes functionality to generate AnsiEscape escape sequences:

### Bold, Underlined & reversed:

You can set the bold, underlined and reversed attributes:

```kotlin
val boldStr: String = AnsiEscape { "this is bold".bold } 
val underlineStr: String = AnsiEscape { "this is underlined".underline } 
val reversedStr: String = AnsiEscape { "this is reversed".colorReversed } 
val allStr: String = AnsiEscape { "this is ${"heavy".bold}".underline.colorReversed } 
```

### Simple colors:

You can access to the enum: `Color.BLACK`, `Color.RED`, `Color.GREEN`, `Color.YELLOW`, `Color.BLUE`, `Color.PURPLE`, `Color.CYAN`, `Color.WHITE`
Then use the method `String.color(Color.RED, bright = true)` or `String.bgColor(Color.GREEN, bright = false)`.
But AnsiEscape also provide property shortcuts like: `String.black`, `String.red`... or `String.bgYellow`, `String.bgCyan` etc.

```kotlin
val mixedStr: String = AnsiEscape { "this is red".red + ", blue".blue + ", green".green } 
val mixedStr2: String = AnsiEscape { "this is cyan with a yellow background".cyan.bgYellow } 
val mixedStr3: String = AnsiEscape { "today is brigther".color(AnsiEscape.Color.GREEN, bright = true).bgColor(AnsiEscape.Color.PURPLE, bright = true) } 
```

### Simple Usage

By calling `AnsiEscape {}` you have access inside to some extension properties for String:

```kotlin
println(AnsiEscape { ("hello".red.bold + " world".blue.underline) })
println(AnsiEscape { "hello".color256(33).bgColor256(185) })
```

### Generate a 256 color sequence:

You can generate 256 extra different colors by using `color256` and `bgColor256`:

```kotlin
for (i in 0 until 16) {
    for (j in 0 until 16) {
        val code = i * 16 + j
        print(AnsiEscape { "$code".padStart(4, ' ').color256(code).bgColor256((code + 10) % 256) })
    }
    println()
}
```
