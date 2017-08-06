package com.soywiz.korge.scene

import com.soywiz.korim.color.Colors
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korma.geom.SizeInt

open class Module {
	open val bgcolor: Int = Colors.BLACK
	open val title: String = "Game"
	open val icon: String? = null
	open val windowSize: SizeInt = SizeInt(640, 480)
	open val virtualSize: SizeInt get() = windowSize

	@Deprecated("Use virtualSize", ReplaceWith("windowSize.width"))
	open val width: Int get() = windowSize.width
	@Deprecated("Use virtualSize", ReplaceWith("windowSize.height"))
	open val height: Int get() = windowSize.height
	@Deprecated("Use virtualSize", ReplaceWith("virtualSize.width"))
	open val virtualWidth: Int get() = virtualSize.width
	@Deprecated("Use virtualSize", ReplaceWith("virtualSize.height"))
	open val virtualHeight: Int get() = virtualSize.height
	open val mainScene: Class<out Scene> = EmptyScene::class.java
	open val clearEachFrame = true

	open suspend fun init(injector: AsyncInjector) {
	}
}
