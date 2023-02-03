package com.soywiz.korge.view.tiles

import com.soywiz.kds.IntArray2
import com.soywiz.kds.toStacked
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.SmoothedBmpSlice
import com.soywiz.korge.view.ViewDslMarker
import com.soywiz.korge.view.addTo
import com.soywiz.korim.bitmap.*

inline fun Container.repeatedImageView(
    bitmap: BmpSlice,
    repeatX: Boolean = false,
    repeatY: Boolean = false,
    smoothing: Boolean = true,
    block: @ViewDslMarker SingleTile.() -> Unit = {}
) = SingleTile(bitmap, smoothing).repeat(if (repeatX) TileMapRepeat.REPEAT else TileMapRepeat.NONE,
        if (repeatY) TileMapRepeat.REPEAT else TileMapRepeat.NONE).addTo(this, block)

open class SingleTile(
    bitmap: BmpSlice,
    smoothing: Boolean = true
) : BaseTileMap(IntArray2(1, 1, 0).toStacked(), smoothing), SmoothedBmpSlice {
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
