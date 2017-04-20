package com.soywiz.korge.scene

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Views
import com.soywiz.korio.async.go

class SceneContainer(views: Views) : Container(views) {
	var currentScene: Scene? = null
	suspend inline fun <reified T : Scene> changeTo() = changeTo(T::class.java)
	suspend fun changeTo(clazz: Class<out Scene>) {
		val oldScene = currentScene
		val instance = views.injector.child().map(this@SceneContainer).create(clazz)
		currentScene = instance!!
		instance.sceneInit(instance.sceneView)
		this.removeChildren()

		oldScene?.sceneDestroy()

		this += instance.sceneView
		go {
			instance.sceneAfterDestroy()
		}
		go {
			instance.sceneAfterInit()
		}
	}
}

fun Views.sceneContainer() = SceneContainer(this)
