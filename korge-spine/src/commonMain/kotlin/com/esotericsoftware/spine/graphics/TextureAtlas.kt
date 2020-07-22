package com.esotericsoftware.spine.graphics

import com.soywiz.korim.atlas.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*

class TextureAtlas(val atlas: Atlas) {
    private val regions = HashMap<String, AtlasRegion>()
    private val textures = HashMap<BmpSlice, Texture>()

    fun findRegion(path: String): AtlasRegion? {
        return regions.getOrPut(path) {
            val entry = atlas.tryGetEntryByName(path) ?: error("Can't find '$path' in atlas")
            AtlasRegion(entry)
        }
    }

    inner class AtlasRegion(val entry: Atlas.Entry) {
        val bmpSlice = entry.slice
        val texture = textures.getOrPut(bmpSlice) { Texture(bmpSlice) }

        val u: Float = bmpSlice.tl_x
        val v: Float = bmpSlice.tl_y
        val u2: Float = bmpSlice.br_x
        val v2: Float = bmpSlice.br_y
        val regionWidth: Float get() = bmpSlice.width.toFloat()
        val regionHeight: Float get() = bmpSlice.height.toFloat()
        var offsetX = 0f
        var offsetY = 0f
        var originalWidth = bmpSlice.width.toFloat()
        var originalHeight = bmpSlice.height.toFloat()
        var rotate = bmpSlice.rotated
        var packedHeight = 0f
        var packedWidth = 0f
        var degrees = bmpSlice.rotatedAngle
    }
}
