package com.soywiz.korge.i18n

import com.soywiz.kds.getExtra
import com.soywiz.kds.setExtra
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.foreachDescendant
import com.soywiz.korio.util.i18n.Language

private var Views.extraLanguage: Language
    get() = getExtra("extraLanguage") as? Language ?: Language.CURRENT
    set(value) { setExtra("extraLanguage", value) }

var Views.language: Language
	get() = this.extraLanguage
	set(value) {
		this.extraLanguage = value
		this.stage.foreachDescendant {
			if (it is TextContainer) it.updateText(value)
		}
	}
