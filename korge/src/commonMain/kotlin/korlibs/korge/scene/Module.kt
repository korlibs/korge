package korlibs.korge.scene

import korlibs.korge.internal.DefaultViewport
import korlibs.korge.view.Stage
import korlibs.korge.view.Views
import korlibs.render.GameWindow
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.format.ImageFormat
import korlibs.inject.AsyncInjector
import korlibs.math.geom.*
import kotlin.reflect.KClass

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

	open val virtualSize: SizeInt get() = DefaultViewport.SIZE
	open val windowSize: SizeInt get() = virtualSize

	open val mainScene: KClass<out Scene>? = null
    open val main: (suspend (Stage) -> Unit)? = null

    open val constructedScene: Scene.(Views) -> Unit = {}
    open val constructedViews: (Views) -> Unit = {}

    open val clearEachFrame = true

    @Deprecated("")
	open val targetFps: Double = 0.0
	open val scaleAnchor: Anchor = Anchor.MIDDLE_CENTER
	open val scaleMode: ScaleMode = ScaleMode.SHOW_ALL
	open val clipBorders: Boolean = true

	open val fullscreen: Boolean? = null

	@Suppress("DEPRECATION")
	open suspend fun AsyncInjector.configure() = Unit
}