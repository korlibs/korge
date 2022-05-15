package com.soywiz.korge.i18n

import com.soywiz.korio.util.i18n.Language

class ConstantTextSource(val text: String) : TextSource {
	override fun getText(language: Language): String = text
}
