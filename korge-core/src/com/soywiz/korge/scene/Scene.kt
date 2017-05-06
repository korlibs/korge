package com.soywiz.korge.scene

import com.soywiz.korge.bus.Bus
import com.soywiz.korge.log.Logger
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.time.TimeSpan
import com.soywiz.korge.time.sleep
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.ViewsContainer
import com.soywiz.korio.async.CoroutineContextHolder
import com.soywiz.korio.coroutine.CoroutineContext
import com.soywiz.korio.inject.AsyncDependency
import com.soywiz.korio.inject.Inject
import com.soywiz.korio.util.Cancellable

abstract class Scene : AsyncDependency, ViewsContainer, CoroutineContextHolder {
	@Inject override lateinit var views: Views
	@Inject lateinit var sceneContainer: SceneContainer
	@Inject lateinit var resourcesRoot: ResourcesRoot
	@Inject protected lateinit var bus: Bus
	lateinit var sceneView: Container; private set
	val root get() = sceneView
	protected val destroyCancellables = arrayListOf<Cancellable>()
	override val coroutineContext: CoroutineContext get() = views.coroutineContext

	suspend final override fun init() {
		sceneView = views.container()
	}

	suspend abstract fun sceneInit(sceneView: Container): Unit

	suspend open fun sceneAfterInit() {
	}

	suspend open fun sceneBeforeLeaving() {
	}

	suspend open fun sceneDestroy() {
		for (cancellable in destroyCancellables) cancellable.cancel()
	}

	suspend open fun sceneAfterDestroy() {
	}
}

class EmptyScene : Scene() {
	suspend override fun sceneInit(sceneView: Container) {
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

suspend fun Scene.sleep(time: TimeSpan) = sceneView.sleep(time)
