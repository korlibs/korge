package com.soywiz.korio.util.i18n

import com.soywiz.korio.concurrent.atomic.*

internal expect val systemLanguageStrings: List<String>

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
		val BY_ID = ((values().map { it.name.toLowerCase() to it } + values().map { it.iso6391 to it } + values().map { it.iso6392 to it })).toMap()
		operator fun get(id: String): Language? = BY_ID[id]

		val SYSTEM_LANGS: List<Language> by lazy { systemLanguageStrings.mapNotNull { BY_ID[it.substringBefore('-')] } }
		val SYSTEM: Language by lazy { SYSTEM_LANGS.firstOrNull() ?: ENGLISH }

		var CURRENT: Language
			set(value) = run { Language_CURRENT.value = value }
			get() = Language_CURRENT.value
	}
}

private val Language_CURRENT: KorAtomicRef<Language> by lazy { KorAtomicRef(Language.SYSTEM) }
