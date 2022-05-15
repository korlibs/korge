package com.soywiz.korge.scene

import com.soywiz.klock.TimeSpan
import com.soywiz.korag.AG
import com.soywiz.korge.debug.KorgeDebugNode
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.time.delay
import com.soywiz.korge.util.CancellableGroup
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.ScaleView
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.ViewsContainer
import com.soywiz.korge.view.views
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korinject.AsyncInjectorContext
import com.soywiz.korinject.InjectorAsyncDependency
import com.soywiz.korio.lang.cancel
import com.soywiz.korio.resources.Resources
import com.soywiz.korio.resources.ResourcesContainer
import com.soywiz.korma.geom.ISize
import com.soywiz.korui.UiContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * Acts as a controller. Subclasses must override at least one of: [sceneInit] or [sceneMain].
 *
 * The lifecycle of the [Scene] for ([SceneContainer.pushTo], [SceneContainer.changeTo], [SceneContainer.back], [SceneContainer.forward]):
 *
 * - ## OLD Scene -> NEW Scene -- Scene is changed
 * - NEW: [sceneInit] - BLOCK - Called when this scene enters. Waits until completed
 * - NEW: [sceneMain] - DO NOT BLOCK - Called after [sceneInit]. Doesn't wait for completion. Its underlying Job is closed automatically on [sceneAfterDestroy]
 * - OLD: [sceneBeforeLeaving] - BLOCK - Called on the old scene. Waits until completed.
 * - Transition from the old view and the new view is performed the animation transition set values for [View.ratio] in the [SceneContainer.transitionView] in the range 0..1
 * - OLD: [sceneDestroy] - BLOCK - Called when this scene exits. Waits until completed.
 * - OLD: [sceneAfterDestroy] - DO NOT BLOCK - Cancels the [Job] of the old scene. Do not wait
 * - NEW: [sceneAfterInit] - DO NOT BLOCK - Similar to [sceneMain] but after the transition.
 * - ## New scene is returned
 */
abstract class Scene : InjectorAsyncDependency, ViewsContainer, CoroutineScope, KorgeDebugNode, ResourcesContainer {
    /** A child [AsyncInjector] for this instance. Set by the [init] method. */
	lateinit var injector: AsyncInjector
    /** The [Views] singleton of the application. Set by the [init] method. */
	override lateinit var views: Views
    /** The [SceneContainer] [View] that contains this [Scene]. Set by the [init] method. */
	lateinit var sceneContainer: SceneContainer
    /** The [ResourcesRoot] singleton. Set by the [init] method. */
	lateinit var resourcesRoot: ResourcesRoot

    /** A reference to the [AG] from the [Views] */
    val ag: AG get() = views.ag

    val stage get() = views.stage

    /** A [Container] view that will wrap the Scene view */
    internal val _sceneViewContainer: Container = Container()

	val root get() = _sceneViewContainer

	protected val cancellables = CancellableGroup()
    override val coroutineContext by lazy { views.coroutineContext + AsyncInjectorContext(injector) + Job(views.coroutineContext[Job.Key]) }
	val sceneView: Container = createSceneView().apply {
		_sceneViewContainer += this
	}
    override val resources: Resources by lazy { injector.getSync() }
	protected open fun createSceneView(): Container = Container()

    /**
     * This method will be called by the [SceneContainer] that will display this [Scene].
     * This will set the [injector], [views], [sceneContainer] and [resourcesRoot].
     */
    final override suspend fun init(injector: AsyncInjector) {
		this.injector = injector
		this.views = injector.get()
		this.sceneContainer = injector.get()
		this.resourcesRoot = injector.get()
	}

    /**
     * BLOCK. This is called to setup the scene.
     * **Nothing will be shown until this method completes**.
     * Here you can read and wait for resources.
     * No need to call super.
     **/
	open suspend fun Container.sceneInit() {
    }

    /**
     * DO NOT BLOCK. This is called as a main method of the scene.
     * This is called after [sceneInit].
     * This method doesn't need to complete as long as it suspends.
     * Its underlying job will be automatically closed on the [sceneAfterDestroy].
     * No need to call super.
     */
    open suspend fun Container.sceneMain() {

    }

    /**
     * DO NOT BLOCK. Called after the old scene has been destroyed
     * and the transition has been completed.
     **/
	open suspend fun sceneAfterInit() {
	}

    /**
     * BLOCK. Called on the old scene after the new scene has been
     * initialized, and before the transition is performed.
     */
	open suspend fun sceneBeforeLeaving() {
	}

    /**
     * BLOCK. Called on the old scene after the transition
     * has been performed, and the old scene is not visible anymore.
     */
	open suspend fun sceneDestroy() {
	}

    internal suspend fun sceneDestroyInternal() {
        cancellables.cancel()
        injector.deinit()
    }

    /**
     * DO NOT BLOCK. Called on the old scene after the transition
     * has been performed, and the old scene is not visible anymore.
     *
     * At this stage the scene [coroutineContext] [Job] will be cancelled.
     * Stopping [sceneMain] and other [launch] methods using this scene [CoroutineScope].
     */
	open suspend fun sceneAfterDestroy() {
	}

    internal suspend fun sceneAfterDestroyInternal() {
        sceneAfterDestroy()
        try {
            coroutineContext.cancel() // cancelAndJoin was being used when hanged on native?
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            e.printStackTrace()
        }
    }

    override fun buildDebugComponent(views: Views, container: UiContainer) {
    }
}

abstract class ScaledScene : Scene() {
	open val sceneSize: ISize = ISize(320, 240)
	open val sceneScale: Double = 2.0
	open val sceneFiltering: Boolean = false

	override fun createSceneView(): Container = ScaleView(
		sceneSize.width.toInt(),
		sceneSize.height.toInt(),
		scale = sceneScale,
		filtering = sceneFiltering
	)
}

class EmptyScene : Scene() {
	override suspend fun Container.sceneInit() {
	}
}

abstract class LogScene : Scene() {
	open val sceneName: String get() = "LogScene"
	open val log = arrayListOf<String>()

	open fun log(msg: String) {
		this.log += msg
	}

	override suspend fun Container.sceneInit() {
		log("$sceneName.sceneInit")
	}

	override suspend fun sceneAfterInit() {
		log("$sceneName.sceneAfterInit")
		super.sceneAfterInit()
	}

	override suspend fun sceneDestroy() {
		log("$sceneName.sceneDestroy")
		super.sceneDestroy()
	}

	override suspend fun sceneAfterDestroy() {
		log("$sceneName.sceneAfterDestroy")
		super.sceneAfterDestroy()
	}
}

suspend fun Scene.delay(time: TimeSpan) = sceneView.delay(time)
