package korlibs.korge.scene

import korlibs.datastructure.iterators.*
import korlibs.inject.*
import korlibs.io.async.*
import korlibs.io.async.async
import korlibs.io.resources.*
import korlibs.korge.*
import korlibs.korge.tween.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.property.*
import korlibs.logger.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.time.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlin.reflect.*

/**
 * Creates a new [SceneContainer], allowing to configure with [callback], and attaches the newly created container to the receiver this [Container]
 * It requires to specify the [Views] singleton instance, and allows to specify a [defaultTransition] [Transition].
 **/
inline fun Container.sceneContainer(
	views: Views,
    defaultTransition: Transition = AlphaTransition.withEasing(Easing.EASE_IN_OUT_QUAD),
    name: String = "sceneContainer",
    size: Size = Size(0f, 0f),
	callback: SceneContainer.() -> Unit = {}
): SceneContainer {
    val (width, height) = size
    var rwidth = width
    var rheight = height
    if (width == 0f && height == 0f) {
        val base = this.findFirstAscendant { it is FixedSizeContainer } as? FixedSizeContainer?
        rwidth = base?.width ?: views.stage.width
        rheight = base?.height ?: views.stage.height
    }
    return SceneContainer(views, defaultTransition, name, Size(rwidth, rheight)).addTo(this, callback)
}

suspend inline fun Container.sceneContainer(
    defaultTransition: Transition = AlphaTransition.withEasing(Easing.EASE_IN_OUT_QUAD),
    name: String = "sceneContainer",
    size: Size = Size.ZERO,
    callback: SceneContainer.() -> Unit = {}
): SceneContainer = sceneContainer(views(), defaultTransition, name, size, callback)

/**
 * A [Container] [View] that can hold [Scene]s controllers and contains a history.
 * It changes between scene objects by using the [AsyncInjector] and allow to use [Transition]s for the change.
 * You can apply a easing to the [Transition] by calling [Transition.withEasing].
 */
class SceneContainer(
    val views: Views,
    /** Default [Transition] that will be used when no transition is specified */
    val defaultTransition: Transition = AlphaTransition.withEasing(Easing.EASE_IN_OUT_QUAD),
    name: String = "sceneContainer",
    size: Size = views.stage.size,
) : UIView(size), CoroutineScope by views {
    init {
        this.name = name
    }
    /** The [TransitionView] that will be in charge of rendering the old and new scenes during the transition using a specified [Transition] object */
	val transitionView = TransitionView().also { this += it }

    /** The [Scene] that is currently set or null */
    @ViewProperty
    @ViewPropertySubTree
	var currentScene: Scene? = null
    override fun onSizeChanged() {
        currentScene?.onSizeChanged(Size(width, height))
    }

    init {
        onEvent(ReloadEvent) { event ->
            val hasChildScenes = descendantsWith { it is SceneContainer && it != this }.isNotEmpty()
            if (hasChildScenes) {
                println("[ReloadEvent] Scene $currentScene not reloaded because has child scenes...")
                return@onEvent
            }
            launchImmediately {
                val scene = currentScene
                if (scene != null) {
                    println("[ReloadEvent] Reloading $currentScene . doFullReload=${event.doFullReload}")
                    val sceneClass: KClass<Scene> = if (event.doFullReload) {
                        event.getReloadedClass(scene::class, scene.injector)
                    } else {
                        scene::class
                    } as KClass<Scene>

                    val scene = changeTo(sceneClass, {
                        it.get(sceneClass)
                            .also { newScene ->
                                try {
                                    event.transferKeepProperties(scene, newScene)
                                } catch (e: Throwable) {
                                    e.printStackTrace()
                                }
                            }
                    })
                    scene.views.debugHightlightView(scene.sceneView)
                }
            }
        }
    }

    // Async versions
    /** Async variant returning a [Deferred] for [changeTo] */
	inline fun <reified T : Scene> changeToAsync(vararg injects: Any, time: TimeSpan = 0.seconds, transition: Transition = defaultTransition): Deferred<T>
        = CoroutineScope(coroutineContext).async { changeTo<T>(*injects, time = time, transition = transition) }

    /** Async variant returning a [Deferred] for [changeTo] */
    fun <T : Scene> changeToAsync(clazz: KClass<T>, vararg injects: Any, time: TimeSpan = 0.seconds, transition: Transition = defaultTransition): Deferred<T>
        = CoroutineScope(coroutineContext).async { changeTo(clazz, *injects, time = time, transition = transition) }

    /** Async variant returning a [Deferred] for [pushTo] */
	inline fun <reified T : Scene> pushToAsync(vararg injects: Any, time: TimeSpan = 0.seconds, transition: Transition = defaultTransition): Deferred<T>
        = CoroutineScope(coroutineContext).async { pushTo<T>(*injects, time = time, transition = transition) }

    /** Async variant returning a [Deferred] for [pushTo] */
    fun <T : Scene> pushToAsync(clazz: KClass<T>, vararg injects: Any, time: TimeSpan = 0.seconds, transition: Transition = defaultTransition): Deferred<T>
        = CoroutineScope(coroutineContext).async { pushTo(clazz, *injects, time = time, transition = transition) }

    /** Async variant returning a [Deferred] for [back] */
	suspend fun backAsync(time: TimeSpan = 0.seconds, transition: Transition = defaultTransition): Deferred<Scene> = CoroutineScope(coroutineContext).async { back(time, transition) }

    /** Async variant returning a [Deferred] for [forward] */
	suspend fun forwardAsync(time: TimeSpan = 0.seconds, transition: Transition = defaultTransition): Deferred<Scene> = CoroutineScope(coroutineContext).async { forward(time, transition) }

    /**
     * Changes to the [T] [Scene], with a set of optional [injects] instances during [time] time, and with [transition].
     * This method waits until the [transition] has been completed, and returns the [T] created instance.
     */
	suspend inline fun <reified T : Scene> changeTo(
        vararg injects: Any,
        time: TimeSpan = 0.seconds,
        transition: Transition = defaultTransition
	): T = changeTo(T::class, *injects, time = time, transition = transition)

    /**
     * Pushes the [T] [Scene], with a set of optional [injects] instances during [time] time, and with [transition].
     * It removes the old [currentScene] if any.
     * This method waits until the [transition] has been completed, and returns the [T] created instance.
     */
    suspend inline fun <reified T : Scene> pushTo(
        vararg injects: Any,
        time: TimeSpan = 0.seconds,
        transition: Transition = defaultTransition
	) = pushTo(T::class, *injects, time = time, transition = transition)

    /**
     * Returns to the previous pushed Scene with [pushTo] in [time] time, and with [transition].
     * This method waits until the [transition] has been completed, and returns the old [Scene] instance.
     */
	suspend fun back(time: TimeSpan = 0.seconds, transition: Transition = defaultTransition): Scene {
		visitPos--
		return _changeTo(visitStack.getOrNull(visitPos) ?: EMPTY_VISIT_ENTRY, time = time, transition = transition)
	}

    /**
     * Returns to the next pushed Scene with [pushTo] in [time] time, and with [transition].
     * This method waits until the [transition] has been completed, and returns the old [Scene] instance.
     */
	suspend fun forward(time: TimeSpan = 0.seconds, transition: Transition = defaultTransition): Scene {
		visitPos++
		return _changeTo(visitStack.getOrNull(visitPos) ?: EMPTY_VISIT_ENTRY, time = time, transition = transition)
	}

    /**
     * Pushes the [T] [clazz] [Scene], with a set of optional [injects] instances during [time] time, and with [transition].
     * It removes the old [currentScene] if any.
     * This method waits until the [transition] has been completed, and returns the [T] created instance.
     */
	suspend fun <T : Scene> pushTo(
        clazz: KClass<T>,
        vararg injects: Any,
        time: TimeSpan = 0.seconds,
        transition: Transition = defaultTransition
	): T {
		visitPos++
		setCurrent(VisitEntry(clazz, injects.toList()))
		return _changeTo(clazz, *injects, time = time, transition = transition)
	}

    /**
     * Changes to the [T] [clazz] [Scene], with a set of optional [injects] instances during [time] time, and with [transition].
     * This method waits until the [transition] has been completed, and returns the [T] created instance.
     */
	suspend fun <T : Scene> changeTo(
        clazz: KClass<T>,
        vararg injects: Any,
        time: TimeSpan = 0.seconds,
        transition: Transition = defaultTransition
	): T {
		setCurrent(VisitEntry(clazz, injects.toList()))
		return _changeTo(clazz, *injects, time = time, transition = transition)
	}

    suspend inline fun <reified T : Scene> changeTo(
        crossinline gen: suspend (AsyncInjector) -> T,
        vararg injects: Any,
        time: TimeSpan = 0.seconds,
        transition: Transition = defaultTransition
    ): T {
        return changeTo(T::class, gen, injects = injects, time = time, transition = transition, remap = true)
    }

    suspend inline fun <T : Scene> changeTo(
        clazz: KClass<T>,
        crossinline gen: suspend (AsyncInjector) -> T,
        vararg injects: Any,
        time: TimeSpan = 0.seconds,
        transition: Transition = defaultTransition,
        remap: Boolean = false
    ): T {
        val sceneInjector: AsyncInjector =
            views.injector.child()
                .mapInstance(SceneContainer::class, this@SceneContainer)
                .mapInstance(Resources::class, Resources(coroutineContext, views.globalResources.root, views.globalResources))
        injects.fastForEach { inject ->
            sceneInjector.mapInstance(inject::class as KClass<Any>, inject)
        }
        val newScene = gen(sceneInjector)
        SceneContainer.logger.info { "Changing scene to... $clazz ... $newScene" }
        if (remap) {
            newScene.init(sceneInjector)
            views.injector.mapPrototype(newScene::class as KClass<T>) { gen(sceneInjector) }
            //println("REMAPPED: $clazz")
        }
        return _changeTo(newScene, time, transition)
    }


    private suspend fun _changeTo(
        entry: VisitEntry,
        time: TimeSpan = 0.seconds,
        transition: Transition = defaultTransition
	): Scene = _changeTo(entry.clazz, *entry.injects.toTypedArray(), time = time, transition = transition) as Scene

    /** Check [Scene] for details of the lifecycle. */
    private suspend fun <T : Scene> _changeTo(
        clazz: KClass<T>,
        vararg injects: Any,
        time: TimeSpan = 0.seconds,
        transition: Transition = defaultTransition
    ): T {
        return changeTo(clazz, { it.get(clazz) }, *injects, time = time, transition = transition)
    }

    /** Check [Scene] for details of the lifecycle. */
    @PublishedApi internal suspend fun <T : Scene> _changeTo(
        newScene: T,
        time: TimeSpan = 0.seconds,
        transition: Transition = defaultTransition
    ): T {
        val oldScene = currentScene
        currentScene = newScene

        transitionView.startNewTransition(newScene._sceneViewContainer, transition)

        //println("SCENE PREINIT")
        try {
            newScene.coroutineContext.launchUnscopedAndWait {
                //println("coroutineContext=$coroutineContext")
                newScene.sceneView.apply { newScene.apply { sceneInit() } }
                //println("...")
            }
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            //println("WOOOPS!")
            e.printStackTrace()
        }
        //println("SCENE POSTINIT")

        newScene.launchUnscoped {
            newScene.sceneView.apply { newScene.apply { sceneMain() } }
        }

        if (oldScene != null) {
            oldScene.coroutineContext.launchUnscopedAndWait {
                oldScene.sceneBeforeLeaving()
            }
        }

        if (time > 0.seconds) {
            transitionView.tween(transitionView::ratio[0.0, 1.0], time = time)
        } else {
            transitionView.ratio = 1f
        }

        transitionView.endTransition()

        if (oldScene != null) {
            oldScene.coroutineContext.launchUnscopedAndWait {
                //println("sceneDestroy.coroutineContext=$coroutineContext")
                oldScene.sceneDestroy()
                oldScene.sceneDestroyInternal()
            }

            oldScene.launchUnscoped {
                //println("sceneAfterDestroyInternal.coroutineContext=$coroutineContext")

                oldScene.sceneAfterDestroyInternal()
            }
        }

        newScene.coroutineContext.launchUnscoped {
            newScene.sceneAfterInit()
        }

        return newScene
    }


    private data class VisitEntry(val clazz: KClass<out Scene>, val injects: List<Any>)

    companion object {
        val logger = Logger("SceneContainer")

        private val EMPTY_VISIT_ENTRY = VisitEntry(EmptyScene::class, listOf())
    }

    private val visitStack = arrayListOf<VisitEntry>(EMPTY_VISIT_ENTRY)
    private var visitPos = 0

    // https://developer.mozilla.org/en/docs/Web/API/History

    private fun setCurrent(entry: VisitEntry) {
        while (visitStack.size <= visitPos) visitStack.add(EMPTY_VISIT_ENTRY)
        visitStack[visitPos] = entry
    }
}
