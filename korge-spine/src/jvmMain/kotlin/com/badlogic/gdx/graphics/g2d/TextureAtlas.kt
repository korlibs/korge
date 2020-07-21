package com.badlogic.gdx.graphics.g2d

import com.badlogic.gdx.files.*
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import java.lang.RuntimeException
import com.badlogic.gdx.graphics.g2d.TextureRegion

class TextureAtlas(val handle: FileHandle) {
    private val dummyTexture = Texture()
    private val dummyRegions = LinkedHashMap<String?, AtlasRegion>()

    fun findRegion(path: String?): AtlasRegion? {
        return dummyRegions.getOrPut(path) { AtlasRegion() }
    }

    inner class AtlasRegion : TextureRegion() {
        @JvmField
        var offsetX = 0f
        @JvmField
        var offsetY = 0f
        @JvmField
        var originalWidth = 0f
        @JvmField
        var originalHeight = 0f
        @JvmField
        var rotate = false
        @JvmField
        var packedHeight = 0f
        @JvmField
        var packedWidth = 0f
        @JvmField
        var degrees = 0

        override val texture: Texture get() = this@TextureAtlas.dummyTexture
    }
}
