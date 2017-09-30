package com.soywiz.korge.ui

import com.soywiz.korge.resources.Path
import com.soywiz.korio.inject.Singleton

@Singleton
class UIFactory(
	@Path("korge-ui.png") val skin: UISkin
) {
	val views = skin.views
}
