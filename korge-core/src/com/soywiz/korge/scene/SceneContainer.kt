package com.soywiz.korge.scene

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Views

class SceneContainer(views: Views) : Container(views) {
	lateinit var currentScene: Scene
	suspend inline fun <reified T : Scene> changeTo() = changeTo(T::class.java)
	suspend fun changeTo(clazz: Class<out Scene>) {
		val instance = views.injector.child().map(this@SceneContainer).create(clazz)
		currentScene = instance!!
		this.removeChildren()
		this += instance.sceneView
	}
}

fun Views.sceneContainer() = SceneContainer(this)
