package com.soywiz.korge

import com.soywiz.klock.TimeProvider
import com.soywiz.korag.AG
import com.soywiz.korag.log.DummyAG
import com.soywiz.korag.software.AGSoftware
import com.soywiz.korge.internal.DefaultViewport
import com.soywiz.korge.view.Stage
import com.soywiz.korgw.GameWindow
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.ImageFormat
import com.soywiz.korim.format.ImageFormats
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.ScaleMode

object KorgeHeadless {
    class HeadlessGameWindow(override val width: Int = 640, override val height: Int = 480, val draw: Boolean = false) : GameWindow() {
        override val ag: AG = if (draw) AGSoftware(width, height) else DummyAG(width, height)
        val agSoftware: AGSoftware get() = ag as AGSoftware
        val bitmap: Bitmap32 get() = agSoftware.bitmap
    }

    suspend operator fun invoke(config: Korge.Config) = Korge(config.copy(gameWindow = HeadlessGameWindow()))

    suspend operator fun invoke(
        title: String = "Korge",
        width: Int = DefaultViewport.WIDTH, height: Int = DefaultViewport.HEIGHT,
        virtualWidth: Int = width, virtualHeight: Int = height,
        icon: Bitmap? = null,
        iconPath: String? = null,
        //iconDrawable: SizedDrawable? = null,
        //imageFormats: ImageFormat = ImageFormats(PNG),
        imageFormats: ImageFormat = ImageFormats(),
        quality: GameWindow.Quality = GameWindow.Quality.AUTOMATIC,
        targetFps: Double = 0.0,
        scaleAnchor: Anchor = Anchor.MIDDLE_CENTER,
        scaleMode: ScaleMode = ScaleMode.SHOW_ALL,
        clipBorders: Boolean = true,
        bgcolor: RGBA? = Colors.BLACK,
        debug: Boolean = false,
        debugFontExtraScale: Double = 1.0,
        debugFontColor: RGBA = Colors.WHITE,
        fullscreen: Boolean? = null,
        args: Array<String> = arrayOf(),
        timeProvider: TimeProvider = TimeProvider,
        injector: AsyncInjector = AsyncInjector(),
        blocking:Boolean = true,
        debugAg: Boolean = false,
        draw: Boolean = false,
        entry: suspend Stage.() -> Unit
    ): HeadlessGameWindow {
        val gameWindow = HeadlessGameWindow(width, height, draw = draw)
        Korge(
            title, width, height, virtualWidth, virtualHeight, icon, iconPath, /*iconDrawable,*/ imageFormats, quality,
            targetFps, scaleAnchor, scaleMode, clipBorders, bgcolor, debug, debugFontExtraScale, debugFontColor,
            fullscreen, args, gameWindow, timeProvider, injector,
            blocking = blocking,debugAg = debugAg, entry = entry
        )
        return gameWindow
    }
}
