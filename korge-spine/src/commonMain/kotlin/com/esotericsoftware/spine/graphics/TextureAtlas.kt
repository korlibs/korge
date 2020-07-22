package com.esotericsoftware.spine.graphics

import com.soywiz.korim.atlas.*
import com.soywiz.korim.bitmap.*

class TextureAtlas(val atlas: Atlas) {
    private val regions = HashMap<String, AtlasRegion>()
    private val textures = HashMap<Bitmap, Texture>()

    fun findRegion(path: String): AtlasRegion? {
        return regions.getOrPut(path) {
            val entry = atlas.tryGetEntryByName(path) ?: error("Can't find '$path' in atlas")
            AtlasRegion(entry)
        }
    }

    inner class AtlasRegion(val entry: Atlas.Entry) {
        val bmpSlice = entry.slice
        val bmp = bmpSlice.bmp
        val texture = textures.getOrPut(bmp) { Texture(bmp) }

        var rotate = entry.info.rotated

        val u: Float = if (rotate) bmpSlice.br_x else bmpSlice.tl_x
        val u2: Float = if (rotate) bmpSlice.tl_x else bmpSlice.br_x

        val v: Float = bmpSlice.tl_y
        val v2: Float = bmpSlice.br_y
        //val regionWidth: Float get() = bmpSlice.width.toFloat()
        //val regionHeight: Float get() = bmpSlice.height.toFloat()
        var offsetX = entry.info.offset.x.toFloat()
        var offsetY = entry.info.offset.y.toFloat()
        var originalWidth = entry.info.orig.width.toFloat()
        var originalHeight = entry.info.orig.height.toFloat()
        var packedWidth = entry.info.sourceSize.width.toFloat()
        var packedHeight = entry.info.sourceSize.height.toFloat()
        var degrees = if (rotate) 90 else 0
    }
}
