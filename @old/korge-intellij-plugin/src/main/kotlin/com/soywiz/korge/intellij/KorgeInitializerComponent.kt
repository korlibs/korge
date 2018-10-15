package com.soywiz.korge.intellij

import com.soywiz.korge.build.KorgeManualServiceRegistration

class KorgeInitializerComponent {
	init {
		KorgeManualServiceRegistration.register()
	}
}
