package com.soywiz.korge.ui

import com.soywiz.korge.resources.getPath
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korinject.InjectorAsyncDependency
import com.soywiz.korinject.Singleton

@Singleton
class UIFactory : InjectorAsyncDependency {
	lateinit var skin: UISkin
	val views get() = skin.views

	suspend override fun init(injector: AsyncInjector) {
		this.skin = injector.getPath(UISkin::class, "korge-ui.png")
	}
}
