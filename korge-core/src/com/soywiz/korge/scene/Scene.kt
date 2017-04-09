package com.soywiz.korge.scene

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.ViewsContainer
import com.soywiz.korio.inject.AsyncDependency
import com.soywiz.korio.inject.Inject

open class Scene : AsyncDependency, ViewsContainer {
    @Inject override lateinit var views: Views
	@Inject lateinit var sceneContainer: SceneContainer
    lateinit var sceneView: Container; private set
	val root get() = sceneView

    suspend override fun init() {
        sceneView = views.container()
    }
}
