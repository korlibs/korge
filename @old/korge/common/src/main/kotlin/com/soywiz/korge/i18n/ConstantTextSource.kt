package com.soywiz.korge.i18n

class ConstantTextSource(val text: String) : TextSource {
	override fun getText(language: Language): String = text
}
