package com.soywiz.korge.i18n

interface TextSource {
	fun getText(language: Language): String
}
