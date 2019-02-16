package com.soywiz.korge.scene

import com.soywiz.klock.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlin.coroutines.*
import kotlin.reflect.*

inline fun Container.sceneContainer(
	views: Views,
	callback: SceneContainer.() -> Unit = {}
): SceneContainer = SceneContainer(views).addTo(this).apply(callback)

//typealias Sprite = Image
class SceneContainer(val views: Views) : Container(), CoroutineScope by views {
	val transitionView = TransitionView()
	var currentScene: Scene? = null

	init {
		this += transitionView
	}

	// Async versions
	inline fun <reified T : Scene> changeToAsync(vararg injects: Any, time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition): Deferred<T> = CoroutineScope(coroutineContext).async { changeTo<T>(*injects, time = time, transition = transition) }
	inline fun <reified T : Scene> pushToAsync(vararg injects: Any, time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition): Deferred<T> = CoroutineScope(coroutineContext).async { pushTo<T>(*injects, time = time, transition = transition) }
	suspend fun backAsync(time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition): Deferred<Scene> =  CoroutineScope(coroutineContext).async { back(time, transition) }
	suspend fun forwardAsync(time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition): Deferred<Scene> =  CoroutineScope(coroutineContext).async { forward(time, transition) }

	suspend inline fun <reified T : Scene> changeTo(
		vararg injects: Any,
		time: TimeSpan = 0.seconds,
		transition: Transition = AlphaTransition
	) = changeTo(T::class, *injects, time = time, transition = transition)

	suspend inline fun <reified T : Scene> pushTo(
		vararg injects: Any,
		time: TimeSpan = 0.seconds,
		transition: Transition = AlphaTransition
	) = pushTo(T::class, *injects, time = time, transition = transition)

	suspend fun back(time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition): Scene {
		visitPos--
		return _changeTo(visitStack.getOrNull(visitPos) ?: EMPTY_VISIT_ENTRY, time = time, transition = transition)
	}

	suspend fun forward(time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition): Scene {
		visitPos++
		return _changeTo(visitStack.getOrNull(visitPos) ?: EMPTY_VISIT_ENTRY, time = time, transition = transition)
	}

	private data class VisitEntry(val clazz: KClass<out Scene>, val injects: List<Any>)

	companion object {
		private val EMPTY_VISIT_ENTRY = VisitEntry(EmptyScene::class, listOf())
	}

	private val visitStack = arrayListOf<VisitEntry>(EMPTY_VISIT_ENTRY)
	private var visitPos = 0

	// https://developer.mozilla.org/en/docs/Web/API/History

	private fun setCurrent(entry: VisitEntry) {
		while (visitStack.size <= visitPos) visitStack.add(EMPTY_VISIT_ENTRY)
		visitStack[visitPos] = entry
	}

	suspend fun <T : Scene> pushTo(
		clazz: KClass<T>,
		vararg injects: Any,
		time: TimeSpan = 0.seconds,
		transition: Transition = AlphaTransition
	): T {
		visitPos++
		setCurrent(VisitEntry(clazz, injects.toList()))
		return _changeTo(clazz, *injects, time = time, transition = transition)
	}

	suspend fun <T : Scene> changeTo(
		clazz: KClass<T>,
		vararg injects: Any,
		time: TimeSpan = 0.seconds,
		transition: Transition = AlphaTransition
	): T {
		setCurrent(VisitEntry(clazz, injects.toList()))
		return _changeTo(clazz, *injects, time = time, transition = transition)
	}

	private suspend fun _changeTo(
		entry: VisitEntry,
		time: TimeSpan = 0.seconds,
		transition: Transition = AlphaTransition
	): Scene = _changeTo(entry.clazz, *entry.injects.toTypedArray(), time = time, transition = transition) as Scene

	private suspend fun <T : Scene> _changeTo(
		clazz: KClass<T>,
		vararg injects: Any,
		time: TimeSpan = 0.seconds,
		transition: Transition = AlphaTransition
	): T = coroutineScope {
		val oldScene = currentScene
		val sceneInjector: AsyncInjector =
			views.injector.child().mapInstance(SceneContainer::class, this@SceneContainer)
		for (inject in injects) sceneInjector.mapInstance(inject::class as KClass<Any>, inject)
		val instance = sceneInjector.get(clazz)
		currentScene = instance

		transitionView.transition = transition
		transitionView.startNewTransition(instance._sceneViewContainer)

		instance.sceneView.apply { instance.apply { sceneInit() } }

		oldScene?.sceneBeforeLeaving()

		if (time > 0.seconds) {
			transitionView.tween(transitionView::ratio[0.0, 1.0], time = time)
		} else {
			transitionView.ratio = 1.0
		}

		oldScene?.sceneDestroy()

		launchImmediately(kotlin.coroutines.coroutineContext) {
			instance.sceneAfterDestroy()
		}
		launchImmediately(kotlin.coroutines.coroutineContext) {
			instance.sceneAfterInit()
		}
		instance
	}
}
