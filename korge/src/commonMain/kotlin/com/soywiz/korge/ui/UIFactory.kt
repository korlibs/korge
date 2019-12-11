package com.soywiz.korge.ui

import com.soywiz.korge.resources.*
import com.soywiz.korinject.*

//@Singleton
@Deprecated("Use new UI")
class UIFactory : InjectorAsyncDependency {
	lateinit var skin: UISkin
	val views get() = skin.views

	override suspend fun init(injector: AsyncInjector) {
		this.skin = injector.getPath(UISkin::class, "korge-ui.png")
	}
}
