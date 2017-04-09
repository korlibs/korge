package com.soywiz.korge.scene

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Views

class SceneContainer(views: Views) : Container(views) {
	suspend inline fun <reified T : Scene> changeTo() = changeTo(T::class.java)
	suspend fun changeTo(clazz: Class<out Scene>) {
		val instance = views.injector.child().map(this@SceneContainer).create(clazz)
		this += instance!!.sceneView
	}
}

fun Views.sceneContainer() = SceneContainer(this)
