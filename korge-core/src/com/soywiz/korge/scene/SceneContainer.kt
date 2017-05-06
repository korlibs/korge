package com.soywiz.korge.scene

import com.soywiz.korge.time.TimeSpan
import com.soywiz.korge.time.seconds
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Views
import com.soywiz.korio.async.go

class SceneContainer(views: Views) : Container(views) {
	val transitionView = TransitionView(views)
	var currentScene: Scene? = null

	init {
		this += transitionView
	}

	suspend inline fun <reified T : Scene> changeTo(vararg injects: Any, time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition) = changeTo(T::class.java, *injects, time = time, transition = transition)
	suspend inline fun <reified T : Scene> pushTo(vararg injects: Any, time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition) = pushTo(T::class.java, *injects, time = time, transition = transition)

	private data class VisitEntry(val clazz: Class<out Scene>, val injects: List<Any>)

	companion object {
		private val EMPTY_VISIT_ENTRY = VisitEntry(EmptyScene::class.java, listOf())
	}

	private val visitStack = arrayListOf<VisitEntry>(EMPTY_VISIT_ENTRY)
	private var visitPos = 0

	// https://developer.mozilla.org/en/docs/Web/API/History

	suspend fun back(time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition) {
		visitPos--
		_changeTo(visitStack.getOrNull(visitPos) ?: EMPTY_VISIT_ENTRY, time = time, transition = transition)
	}

	suspend fun forward(time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition) {
		visitPos++
		_changeTo(visitStack.getOrNull(visitPos) ?: EMPTY_VISIT_ENTRY, time = time, transition = transition)
	}

	private fun setCurrent(entry: VisitEntry) {
		while (visitStack.size <= visitPos) visitStack.add(EMPTY_VISIT_ENTRY)
		visitStack[visitPos] = entry
	}

	suspend fun pushTo(clazz: Class<out Scene>, vararg injects: Any, time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition) {
		visitPos++
		setCurrent(VisitEntry(clazz, injects.toList()))
		_changeTo(clazz, *injects, time = time, transition = transition)
	}

	suspend fun changeTo(clazz: Class<out Scene>, vararg injects: Any, time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition) {
		setCurrent(VisitEntry(clazz, injects.toList()))
		_changeTo(clazz, *injects, time = time, transition = transition)
	}

	suspend private fun _changeTo(entry: VisitEntry, time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition) {
		_changeTo(entry.clazz, *entry.injects.toTypedArray(), time = time, transition = transition)
	}

	suspend private fun _changeTo(clazz: Class<out Scene>, vararg injects: Any, time: TimeSpan = 0.seconds, transition: Transition = AlphaTransition) {
		val oldScene = currentScene
		val sceneInjector = views.injector.child().mapTyped(this@SceneContainer)
		for (inject in injects) sceneInjector.map(inject)
		val instance = sceneInjector.create(clazz)
		currentScene = instance!!

		transitionView.transition = transition
		transitionView.startNewTransition(instance.sceneView)

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
	}
}

fun Views.sceneContainer() = SceneContainer(this)
