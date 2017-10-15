package com.soywiz.korge.ui

import com.soywiz.korge.plugin.KorgePlugin
import com.soywiz.korge.resources.Path
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.resources.VPath
import com.soywiz.korge.resources.getPath
import com.soywiz.korge.view.Views

object UIPlugin : KorgePlugin() {
	suspend override fun register(views: Views) {
		views.injector.mapSingleton { UIFactory(getPath(UISkin::class, "korge-ui.png")) }
		views.injector.mapFactory(UISkin::class) {
			UISkin.Factory(
				getOrNull(Path::class),
				getOrNull(VPath::class),
				get(ResourcesRoot::class),
				get(Views::class)
			)
		}
	}
}
