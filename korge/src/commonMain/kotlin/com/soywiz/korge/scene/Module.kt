package com.soywiz.korge.scene

import com.soywiz.korge.internal.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korim.vector.*
import com.soywiz.korinject.*
import com.soywiz.korma.geom.*
import kotlin.reflect.*

open class Module {
	open val imageFormats: List<ImageFormat> = listOf(PNG)

	open val bgcolor: RGBA = Colors.BLACK
	open val title: String = "Game"
	open val icon: String? = null
	open val iconImage: Context2d.SizedDrawable? = null

	open val quality: GameWindow.Quality = GameWindow.Quality.PERFORMANCE

	open val size: SizeInt get() = SizeInt(DefaultViewport.WIDTH, DefaultViewport.HEIGHT)
	open val windowSize: SizeInt get() = size

	open val mainScene: KClass<out Scene> = EmptyScene::class
	open val clearEachFrame = true

	open val targetFps: Double = 0.0
	open val scaleAnchor: Anchor = Anchor.MIDDLE_CENTER
	open val scaleMode: ScaleMode = ScaleMode.SHOW_ALL
	open val clipBorders: Boolean = true

	open suspend fun init(injector: AsyncInjector) {
	}
}
