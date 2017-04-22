package com.soywiz.korge.scene

import com.soywiz.korge.tween.rangeTo
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

	suspend inline fun <reified T : Scene> changeTo(vararg injects: Any, time: Int = 0, transition: Transition = AlphaTransition) = changeTo(T::class.java, *injects, time = time, transition = transition)

	suspend fun changeTo(clazz: Class<out Scene>, vararg injects: Any, time: Int = 0, transition: Transition = AlphaTransition) {
		val oldScene = currentScene
		val sceneInjector = views.injector.child().mapTyped(this@SceneContainer)
		for (inject in injects) sceneInjector.map(inject)
		val instance = sceneInjector.create(clazz)
		currentScene = instance!!
		instance.sceneInit(instance.sceneView)

		transitionView.transition = transition
		transitionView.startNewTransition(instance.sceneView)
		transitionView.tween(transitionView::ratio..1.0, time = time)

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
