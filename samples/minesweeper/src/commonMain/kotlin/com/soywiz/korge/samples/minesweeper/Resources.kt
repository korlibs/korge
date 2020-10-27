package com.soywiz.korge.samples.minesweeper

import com.soywiz.korim.bitmap.effect.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.paint.*
import com.soywiz.korio.resources.*

val ResourcesContainer.minesweeperFont by resourceGlobal {
    DefaultTtfFont.toBitmapFont(
        fontSize = 32.0,
        paint = LinearGradientPaint(0.0, 0.0, 0.0, 32.0)
            .add(0.0, Colors["#ecfff8"])
            .add(1.0, Colors["#90c5ff"]),
        effect = BitmapEffect(
            //borderSize = 1,
            //borderColor = Colors.BLACK.withAd(0.2),
            //blurRadius = 0,
            dropShadowY = 1,
            dropShadowX = 1,
            dropShadowRadius = 1,
            dropShadowColor = Colors.BLACK.withAd(0.8),
        )
    )
}
