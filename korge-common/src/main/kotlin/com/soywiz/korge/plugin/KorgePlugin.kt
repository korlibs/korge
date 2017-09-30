package com.soywiz.korge.plugin

import com.soywiz.korge.view.Views
import com.soywiz.korio.inject.Singleton

open class KorgePlugin {
	open suspend fun register(views: Views): Unit {
	}
}

val defaultKorgePlugins = KorgePlugins()

@Singleton
open class KorgePlugins {
	val plugins = LinkedHashSet<KorgePlugin>()

	fun register(vararg plugins: KorgePlugin) = this.apply { this@KorgePlugins.plugins += plugins }
}
