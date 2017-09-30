package com.soywiz.korge.scene

import com.soywiz.korge.log.Logger
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.time.TimeSpan
import com.soywiz.korge.time.sleep
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.ViewsContainer
import com.soywiz.korge.view.scaleView
import com.soywiz.korio.async.CoroutineContextHolder
import com.soywiz.korio.coroutine.CoroutineContext
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.inject.Inject
import com.soywiz.korio.inject.InjectorAsyncDependency
import com.soywiz.korio.util.Cancellable
import com.soywiz.korma.geom.ISize

abstract class Scene : InjectorAsyncDependency, ViewsContainer, CoroutineContextHolder {
	override lateinit var views: Views
	lateinit var sceneContainer: SceneContainer
	lateinit var resourcesRoot: ResourcesRoot
	//protected lateinit var bus: Bus
	lateinit internal var _sceneViewContainer: Container; private set
	lateinit var sceneView: Container; private set
	val root get() = _sceneViewContainer
	protected val destroyCancellables = arrayListOf<Cancellable>()
	override val coroutineContext: CoroutineContext get() = views.coroutineContext

	open protected fun createSceneView(): Container = views.container()

	suspend override fun init(injector: AsyncInjector): Unit {
		this.views = injector.get()
		this.sceneContainer = injector.get()
		this.resourcesRoot = injector.get()
		//this.bus = injector.get()
		_sceneViewContainer = views.container()
		sceneView = createSceneView()
		_sceneViewContainer += sceneView
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

abstract class ScaledScene() : Scene() {
	open val sceneSize: ISize = ISize(320, 240)
	open val sceneScale: Double = 2.0
	open val sceneFiltering: Boolean = false

	override fun createSceneView(): Container = views.scaleView(sceneSize.width.toInt(), sceneSize.height.toInt(), scale = sceneScale, filtering = sceneFiltering)
}

class EmptyScene : Scene() {
	suspend override fun sceneInit(sceneView: Container) {
	}
}

abstract class LogScene : Scene() {
	val name: String = this::class.simpleName ?: "LogSceneUnknown"
	lateinit var logger: Logger

	suspend override fun init(injector: AsyncInjector) {
		super.init(injector)
		logger = injector.get()
	}

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
