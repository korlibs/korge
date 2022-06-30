package com.soywiz.korim.tiles

import com.soywiz.kds.IntArray2
import com.soywiz.kds.each
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.BitmapSlice

data class TileMapData(
    val data: IntArray2,
    val tileSet: TileSet? = null,
)

fun TileMapData.renderTo(out: Bitmap32, x: Int, y: Int) {
    val tileSet = this.tileSet ?: return
    data.each { tx, ty, v ->
        val info = tileSet.getInfo(v)
        if (info != null) {
            val px = x + tx * tileSet.width
            val py = y + ty * tileSet.height
            out.put(info.slice as BitmapSlice<Bitmap32>, px, py)
        }
    }
}

fun TileMapData.render(): Bitmap32 {
    val tileSet = this.tileSet ?: return Bitmap32(1, 1)
    val out = Bitmap32(data.width * tileSet.width, data.height * tileSet.height)
    renderTo(out, 0, 0)
    return out
}
