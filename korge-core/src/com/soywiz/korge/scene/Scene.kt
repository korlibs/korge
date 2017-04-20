package com.soywiz.korge.scene

import com.soywiz.korge.log.Logger
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.ViewsContainer
import com.soywiz.korio.inject.AsyncDependency
import com.soywiz.korio.inject.Inject
import com.soywiz.korio.util.Cancellable

abstract class Scene : AsyncDependency, ViewsContainer {
	@Inject override lateinit var views: Views
	@Inject lateinit var sceneContainer: SceneContainer
	lateinit var sceneView: Container; private set
	val root get() = sceneView
	protected val destroyCancellables = arrayListOf<Cancellable>()

	suspend final override fun init() {
		sceneView = views.container()
	}

	suspend open fun sceneInit(sceneView: Container) {
	}

	suspend open fun sceneAfterInit() {
	}

	suspend open fun sceneDestroy() {
		for (cancellable in destroyCancellables) cancellable.cancel()
	}

	suspend open fun sceneAfterDestroy() {
	}
}

abstract class LogScene : Scene() {
	val name: String = this.javaClass.simpleName
	@Inject lateinit var logger: Logger

	suspend override fun sceneInit(sceneView: Container) {
		logger.info("$name.sceneInit")
		super.sceneAfterInit()
	}

	suspend override fun sceneAfterInit() {
		logger.info("$name.sceneAfterInit")
		super.sceneAfterInit()
	}

	suspend override fun sceneDestroy() {
		logger.info("$name.sceneDestroy")
		super.sceneDestroy()
	}

	suspend override fun sceneAfterDestroy() {
		logger.info("$name.sceneAfterDestroy")
		super.sceneAfterDestroy()
	}
}
