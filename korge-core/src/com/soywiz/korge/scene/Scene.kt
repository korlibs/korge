package com.soywiz.korge.scene

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Views
import com.soywiz.korio.inject.AsyncDependency
import com.soywiz.korio.inject.Inject

open class Scene : AsyncDependency {
    @Inject lateinit var views: Views
    lateinit var root: Container; private set

    suspend override fun init() {
        root = views.container()
    }
}