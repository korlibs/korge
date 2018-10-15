package com.soywiz.korge.i18n

import com.soywiz.kds.*
import com.soywiz.korge.view.*
import com.soywiz.korio.i18n.*

private var Views.extraLanguage by Extra.Property { Language.CURRENT }

var Views.language: Language
	get() = this.extraLanguage
	set(value) {
		this.extraLanguage = value
		this.stage.foreachDescendant {
			if (it is TextContainer) it.updateText(value)
		}
	}
