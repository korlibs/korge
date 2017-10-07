package com.soywiz.korge.plugin

import com.soywiz.korge.atlas.AtlasPlugin
import com.soywiz.korge.audio.SoundPlugin
import com.soywiz.korge.view.Views
import com.soywiz.korio.inject.Singleton

abstract class KorgePlugin {
	abstract suspend fun register(views: Views): Unit
}

val defaultKorgePlugins = KorgePlugins().apply {
	register(AtlasPlugin, SoundPlugin)
}

@Singleton
open class KorgePlugins {
	val plugins = LinkedHashSet<KorgePlugin>()

	fun register(vararg plugins: KorgePlugin) = this.apply { this@KorgePlugins.plugins += plugins }
}
