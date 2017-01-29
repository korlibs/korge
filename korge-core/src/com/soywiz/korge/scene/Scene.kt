package com.soywiz.korge.scene

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Views
import com.soywiz.korio.inject.AsyncDependency
import com.soywiz.korio.inject.AsyncInjector

open class Scene(
	val injector: AsyncInjector
) : AsyncDependency {
	lateinit var views: Views
	lateinit var root: Container

	suspend override fun init() {
		views = injector.get()
		root = views.container()
	}
}