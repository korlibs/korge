package com.soywiz.korge.scene

import com.soywiz.korim.color.Colors
import com.soywiz.korio.inject.AsyncInjector

open class Module {
    open val bgcolor: Int = Colors.BLACK
    open val title: String = "Game"
    open val icon: String? = null
    open val width: Int = 640
    open val height: Int = 480
	open val virtualWidth: Int get() = width
	open val virtualHeight: Int get() = height
    open val mainScene: Class<out Scene> = Scene::class.java

	open suspend fun init(injector: AsyncInjector) {
	}
}
