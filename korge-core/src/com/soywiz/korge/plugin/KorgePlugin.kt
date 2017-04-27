package com.soywiz.korge.plugin

import com.soywiz.korge.view.Views
import com.soywiz.korio.service.Services

open class KorgePlugin : Services.Impl() {
	open suspend fun register(views: Views): Unit {
	}
}
