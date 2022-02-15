package com.soywiz.korge.view.tiles

import com.soywiz.kds.*
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.SmoothedBmpSlice
import com.soywiz.korge.view.ViewDslMarker
import com.soywiz.korge.view.addTo
import com.soywiz.korim.bitmap.BitmapCoords
import com.soywiz.korim.bitmap.BmpSlice

inline fun Container.repeatedImageView(
    bitmap: BmpSlice,
    repeatX: Boolean = false,
    repeatY: Boolean = false,
    smoothing: Boolean = true,
    block: @ViewDslMarker SingleTile.() -> Unit = {}
) = SingleTile(bitmap, smoothing).repeat(if (repeatX) BaseTileMap.Repeat.REPEAT else BaseTileMap.Repeat.NONE,
        if (repeatY) BaseTileMap.Repeat.REPEAT else BaseTileMap.Repeat.NONE).addTo(this, block)

open class SingleTile(
    bitmap: BmpSlice,
    smoothing: Boolean = true
) : BaseTileMap(IntArray2(1, 1, 0), smoothing), SmoothedBmpSlice {
    override val tilesetTextures = Array<BitmapCoords?>(1) { bitmap }

    override var width = 0.0
    override var height = 0.0

    override var bitmap: BitmapCoords = bitmap
        set(value) {
            if (field !== value) {
                field = value
                tilesetTextures[0] = value
                this.width = value.width.toDouble()
                this.height = value.height.toDouble()
                tileWidth = width
                tileHeight = height
                tileSize.width = width
                tileSize.height = height
                contentVersion++
            }
        }
}
