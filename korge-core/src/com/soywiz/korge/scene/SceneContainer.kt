package com.soywiz.korge.scene

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Views

class SceneContainer(views: Views) : Container(views) {
	suspend fun changeToScene(clazz: Class<out Scene>) {
		val instance = views.injector.create(clazz)
		this += instance.root
	}
}

fun Views.sceneContainer() = SceneContainer(this)