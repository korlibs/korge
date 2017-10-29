package com.soywiz.korge.ui

import com.soywiz.korge.resources.getPath
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.inject.InjectorAsyncDependency
import com.soywiz.korio.inject.Singleton

@Singleton
class UIFactory : InjectorAsyncDependency {
	lateinit var skin: UISkin
	val views get() = skin.views

	suspend override fun init(injector: AsyncInjector) {
		this.skin = injector.getPath(UISkin::class, "korge-ui.png")
	}
}
