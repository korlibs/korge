package com.soywiz.korio.util.i18n

import com.soywiz.korio.*
import kotlinx.browser.*

internal actual val systemLanguageStrings: List<String> by lazy {
	if (isNodeJs) {
		val env = process.env
		listOf<String>(env.LANG ?: env.LANGUAGE ?: env.LC_ALL ?: env.LC_MESSAGES ?: "english")
	} else {
		window.navigator.languages.asList()
	}
}

