package com.soywiz.korge.scene

import com.soywiz.korge.plugin.KorgePlugin
import com.soywiz.korim.color.Colors
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korma.geom.SizeInt
import kotlin.reflect.KClass

open class Module {
	open val bgcolor: Int = Colors.BLACK
	open val title: String = "Game"
	open val icon: String? = null
	open val size: SizeInt = SizeInt(640, 480)
	open val windowSize: SizeInt = size
	open val plugins: List<KorgePlugin> = listOf()

	open val mainScene: KClass<out Scene> = EmptyScene::class
	open val clearEachFrame = true

	open suspend fun init(injector: AsyncInjector) {
	}
}
