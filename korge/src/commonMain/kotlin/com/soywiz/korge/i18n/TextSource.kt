package com.soywiz.korge.i18n

import com.soywiz.korio.util.i18n.Language

interface TextSource {
	fun getText(language: Language): String
}
