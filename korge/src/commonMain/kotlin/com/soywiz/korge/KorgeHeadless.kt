package com.soywiz.korge

import com.soywiz.klock.TimeProvider
import com.soywiz.korag.*
import com.soywiz.korag.log.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korim.vector.*
import com.soywiz.korinject.*
import com.soywiz.korma.geom.*

object KorgeHeadless {
    class HeadlessGameWindow(override val width: Int = 640, override val height: Int = 480) : GameWindow() {
        override val ag: AG = DummyAG(width, height)
    }

    suspend operator fun invoke(config: Korge.Config) = Korge(config.copy(gameWindow = HeadlessGameWindow()))

    suspend operator fun invoke(
        title: String = "Korge",
        width: Int = DefaultViewport.WIDTH, height: Int = DefaultViewport.HEIGHT,
        virtualWidth: Int = width, virtualHeight: Int = height,
        icon: Bitmap? = null,
        iconPath: String? = null,
        iconDrawable: SizedDrawable? = null,
        imageFormats: ImageFormat = ImageFormats(PNG),
        quality: GameWindow.Quality = GameWindow.Quality.AUTOMATIC,
        targetFps: Double = 0.0,
        scaleAnchor: Anchor = Anchor.MIDDLE_CENTER,
        scaleMode: ScaleMode = ScaleMode.SHOW_ALL,
        clipBorders: Boolean = true,
        bgcolor: RGBA? = Colors.BLACK,
        debug: Boolean = false,
        fullscreen: Boolean? = null,
        args: Array<String> = arrayOf(),
        timeProvider: TimeProvider = TimeProvider,
        injector: AsyncInjector = AsyncInjector(),
        blocking:Boolean = true,
        debugAg: Boolean = false,
        entry: suspend Stage.() -> Unit
    ) = Korge(
        title, width, height, virtualWidth, virtualHeight, icon, iconPath, iconDrawable, imageFormats, quality,
        targetFps, scaleAnchor, scaleMode, clipBorders, bgcolor, debug, fullscreen, args, HeadlessGameWindow(), timeProvider, injector,
        blocking=blocking,debugAg = debugAg, entry = entry
    )
}
