package com.soywiz.korge.i18n

import com.soywiz.korge.view.Views
import com.soywiz.korge.view.foreachDescendant
import com.soywiz.kds.Extra

private var Views.extraLanguage by Extra.Property { Language.CURRENT }

var Views.language: Language
	get() = this.extraLanguage
	set(value) {
		this.extraLanguage = value
		this.stage.foreachDescendant {
			if (it is TextContainer) it.updateText(value)
		}
	}
