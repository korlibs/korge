package com.soywiz.korge.scene

import com.soywiz.korge.internal.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korim.vector.*
import com.soywiz.korinject.*
import com.soywiz.korma.geom.*
import kotlin.jvm.*
import kotlin.reflect.*

open class KorgeModule(mainScene: KClass<out Scene>) : Module() {
	final override val mainScene: KClass<out Scene> = mainScene
}

open class Module {
	open val imageFormats: List<ImageFormat> = listOf()

	open val bgcolor: RGBA = Colors.BLACK
	open val title: String = "Game"
	open val icon: String? = null
	//open val iconImage: SizedDrawable? = null

	open val quality: GameWindow.Quality = GameWindow.Quality.PERFORMANCE

	open val size: SizeInt get() = SizeInt(DefaultViewport.WIDTH, DefaultViewport.HEIGHT)
	open val windowSize: SizeInt get() = size

	open val mainScene: KClass<out Scene> = EmptyScene::class
	open val clearEachFrame = true

	open val targetFps: Double = 0.0
	open val scaleAnchor: Anchor = Anchor.MIDDLE_CENTER
	open val scaleMode: ScaleMode = ScaleMode.SHOW_ALL
	open val clipBorders: Boolean = true

	open val fullscreen: Boolean? = null

	@Suppress("DEPRECATION")
	open suspend fun AsyncInjector.configure() = Unit
}
