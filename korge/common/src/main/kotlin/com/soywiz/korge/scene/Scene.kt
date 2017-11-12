package com.soywiz.korge.scene

import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.time.TimeSpan
import com.soywiz.korge.time.sleep
import com.soywiz.korge.util.CancellableGroup
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.ViewsContainer
import com.soywiz.korge.view.scaleView
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korinject.InjectorAsyncDependency
import com.soywiz.korio.async.CoroutineContextHolder
import com.soywiz.korio.coroutine.CoroutineContext
import com.soywiz.korma.geom.ISize

abstract class Scene : InjectorAsyncDependency, ViewsContainer, CoroutineContextHolder {
	lateinit var injector: AsyncInjector
	override lateinit var views: Views
	lateinit var sceneContainer: SceneContainer
	lateinit var resourcesRoot: ResourcesRoot
	//protected lateinit var bus: Bus
	lateinit internal var _sceneViewContainer: Container; private set
	lateinit var sceneView: Container; private set
	val root get() = _sceneViewContainer
	protected val cancellables = CancellableGroup()
	override val coroutineContext: CoroutineContext get() = views.coroutineContext

	open protected fun createSceneView(): Container = views.container()

	suspend override fun init(injector: AsyncInjector): Unit {
		//this.injector = injector
		//this.views = injector.get() // @TODO: Bug in Kotlin.JS (no suspension point!)
		//this.sceneContainer = injector.get() // @TODO: Bug in Kotlin.JS (no suspension point!)
		//this.resourcesRoot = injector.get() // @TODO: Bug in Kotlin.JS (no suspension point!)

		this.injector = injector
		this.views = injector.get(Views::class)
		this.sceneContainer = injector.get(SceneContainer::class)
		this.resourcesRoot = injector.get(ResourcesRoot::class)

		//Console.log(injector)
		//println("Scene.init:ResourcesRoot[1]:" + injector.get<ResourcesRoot>())
		//println("Scene.init:ResourcesRoot[2]:" + injector.get(ResourcesRoot::class))
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
		cancellables.cancel()
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
	open val name: String = "LogScene"
	open val log = arrayListOf<String>()

	open fun log(msg: String) {
		this.log += msg
	}

	suspend override fun init(injector: AsyncInjector) {
		super.init(injector)
	}

	suspend override fun sceneInit(sceneView: Container) {
		log("$name.sceneInit")
		super.sceneAfterInit()
	}

	suspend override fun sceneAfterInit() {
		log("$name.sceneAfterInit")
		super.sceneAfterInit()
	}

	suspend override fun sceneDestroy() {
		log("$name.sceneDestroy")
		super.sceneDestroy()
	}

	suspend override fun sceneAfterDestroy() {
		log("$name.sceneAfterDestroy")
		super.sceneAfterDestroy()
	}
}

suspend fun Scene.sleep(time: TimeSpan) = sceneView.sleep(time)
