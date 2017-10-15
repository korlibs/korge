package com.soywiz.korge.scene

import com.soywiz.korge.time.TimeSpan
import com.soywiz.korge.time.seconds
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Views
import com.soywiz.korio.async.go
import kotlin.reflect.KClass

class SceneContainer(views: Views) : Container(views) {
	val transitionView = TransitionView(views)
	var currentScene: Scene? = null

	init {
		this += transitionView
	}

	suspend inline fun <reified T : Scene> changeTo(vararg injects: Any, time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition) = changeTo(T::class, *injects, time = time, transition = transition)
	suspend inline fun <reified T : Scene> pushTo(vararg injects: Any, time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition) = pushTo(T::class, *injects, time = time, transition = transition)

	private data class VisitEntry(val clazz: KClass<out Scene>, val injects: List<Any>)

	companion object {
		private val EMPTY_VISIT_ENTRY = VisitEntry(EmptyScene::class, listOf())
	}

	private val visitStack = arrayListOf<VisitEntry>(EMPTY_VISIT_ENTRY)
	private var visitPos = 0

	// https://developer.mozilla.org/en/docs/Web/API/History

	suspend fun back(time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition): Scene {
		visitPos--
		return _changeTo(visitStack.getOrNull(visitPos) ?: EMPTY_VISIT_ENTRY, time = time, transition = transition)
	}

	suspend fun forward(time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition): Scene {
		visitPos++
		return _changeTo(visitStack.getOrNull(visitPos) ?: EMPTY_VISIT_ENTRY, time = time, transition = transition)
	}

	private fun setCurrent(entry: VisitEntry) {
		while (visitStack.size <= visitPos) visitStack.add(EMPTY_VISIT_ENTRY)
		visitStack[visitPos] = entry
	}

	suspend fun <T : Scene> pushTo(clazz: KClass<T>, vararg injects: Any, time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition): T {
		visitPos++
		setCurrent(VisitEntry(clazz, injects.toList()))
		return _changeTo(clazz, *injects, time = time, transition = transition)
	}

	suspend fun <T : Scene> changeTo(clazz: KClass<T>, vararg injects: Any, time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition): T {
		setCurrent(VisitEntry(clazz, injects.toList()))
		return _changeTo(clazz, *injects, time = time, transition = transition)
	}

	suspend private fun _changeTo(entry: VisitEntry, time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition): Scene {
		return _changeTo(entry.clazz, *entry.injects.toTypedArray(), time = time, transition = transition) as Scene
	}

	suspend private fun <T : Scene> _changeTo(clazz: KClass<T>, vararg injects: Any, time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition): T {
		val oldScene = currentScene
		val sceneInjector = views.injector.child().mapInstance<SceneContainer>(this@SceneContainer)
		for (inject in injects) sceneInjector.mapInstance(inject)
		val instance = sceneInjector.get(clazz)
		currentScene = instance!!

		transitionView.transition = transition
		transitionView.startNewTransition(instance._sceneViewContainer)

		instance.sceneInit(instance.sceneView)

		oldScene?.sceneBeforeLeaving()

		if (time > 0.seconds) {
			transitionView.tween(transitionView::ratio[0.0, 1.0], time = time)
		} else {
			transitionView.ratio = 1.0
		}

		oldScene?.sceneDestroy()

		go {
			instance.sceneAfterDestroy()
		}
		go {
			instance.sceneAfterInit()
		}

		return instance
	}
}

fun Views.sceneContainer() = SceneContainer(this)
