---
permalink: /io/env/
group: io
layout: default
title: Environment
title_prefix: KorIO
description: "Getting Environment variables, Properties, Operating System, user Language..."
fa-icon: fa-sync-alt
priority: 7
---

KorIO has some tools for accessing the environment.



## Environment

Supports accessing environment variables, or the JS querystring.

```kotlin
// Uses querystring on JS/Browser, and proper env vars on the rest of the targets
expect object Environment {
	operator fun get(key: String): String?
	fun getAll(): Map<String, String>
}
```

## Properties

Allows accessing java properties and read `.properties` files.

```kotlin
object SystemProperties : Properties

open class Properties {
    companion object {
        fun parseString(data: String): Properties
    }

    open operator fun contains(key: String): Boolean
    open operator fun get(key: String): String?
    open operator fun set(key: String, value: String): Unit
    open fun setAll(values: Map<String, String>): Unit
    open fun remove(key: String): Unit
    open fun getAll(): Map<String, String> 
}

suspend fun VfsFile.readProperties(charset: Charset = Charsets.UTF8): Properties
```

## OS

Allows getting information about the current Operating System and Runtime.

```kotlin
object OS {
	val rawName: String
	val rawNameLC: String

	val platformName: String
	val platformNameLC: String

	val isWindows: Boolean
	val isUnix: Boolean
	val isPosix: Boolean
	val isLinux: Boolean
	val isMac: Boolean

	val isIos: Boolean
	val isAndroid: Boolean

	val isJs: Boolean
	val isNative: Boolean
	val isJvm: Boolean

	val isJsShell: Boolean
	val isJsNodeJs: Boolean
	val isJsBrowser: Boolean
	val isJsWorker: Boolean
	val isJsBrowserOrWorker: Boolean
}

```

## Language

Allows to represent a language for localization.

```kotlin
// https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
enum class Language(val iso6391: String, val iso6392: String) {
	JAPANESE("ja", "jpn"),
	ENGLISH("en", "eng"),
	FRENCH("fr", "fra"),
	SPANISH("es", "spa"),
	GERMAN("de", "deu"),
	ITALIAN("it", "ita"),
	DUTCH("nl", "nld"),
	PORTUGUESE("pt", "por"),
	RUSSIAN("ru", "rus"),
	KOREAN("ko", "kor"),
	CHINESE("zh", "zho"),
	;

	companion object {
		val BY_ID: Map<String, Language>
		operator fun get(id: String): Language?

		val SYSTEM_LANGS: List<Language>
		val SYSTEM: Language

		var CURRENT: Language
	}
}
```

### Getting current user language

To get the language of the current user / the language of the system:

```kotlin
val language = Language.CURRENT
```
