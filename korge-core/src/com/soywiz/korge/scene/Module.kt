package com.soywiz.korge.scene

import com.soywiz.korim.color.Colors
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korma.geom.SizeInt

open class Module {
	open val bgcolor: Int = Colors.BLACK
	open val title: String = "Game"
	open val icon: String? = null
	open val size: SizeInt = SizeInt(640, 480)
	open val windowSize: SizeInt = size

	open val mainScene: Class<out Scene> = EmptyScene::class.java
	open val clearEachFrame = true

	open suspend fun init(injector: AsyncInjector) {
	}
}
