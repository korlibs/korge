package com.soywiz.korge.plugin

import com.soywiz.korge.view.Views

open class KorgePlugin {
	open suspend fun register(views: Views): Unit {
	}
}
