package com.soywiz.korim.tiles

import com.soywiz.kds.IntArray2
import com.soywiz.kds.each
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.BitmapSlice

data class TileMapData(
    var data: IntArray2,
    var tileSet: TileSet? = null,
    val maskData: Int = 0x0fffffff,
    val maskFlipX: Int = 1 shl 31,
    val maskFlipY: Int = 1 shl 30,
    val maskRotate: Int = 1 shl 29,
)

fun TileMapData.renderTo(out: Bitmap32, x: Int, y: Int) {
    val tileSet = this.tileSet ?: return
    data.each { tx, ty, v ->
        val vv = v and maskData
        val isFlippedX = (v and maskFlipX) != 0 // @TODO: Take this into account
        val isFlippedY = (v and maskFlipY) != 0
        val isRotated = (v and maskRotate) != 0
        val info = tileSet.getInfo(vv)
        if (info != null) {
            val px = x + tx * tileSet.width
            val py = y + ty * tileSet.height
            out.put(info.slice as BitmapSlice<Bitmap32>, px, py)
        }
    }
}

fun TileMapData.render(): Bitmap32 {
    val tileSet = this.tileSet ?: return Bitmap32(1, 1, premultiplied = true)
    val out = Bitmap32(data.width * tileSet.width, data.height * tileSet.height, premultiplied = true)
    renderTo(out, 0, 0)
    return out
}
