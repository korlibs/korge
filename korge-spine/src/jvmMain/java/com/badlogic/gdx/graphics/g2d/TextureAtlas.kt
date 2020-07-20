package com.badlogic.gdx.graphics.g2d

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import java.lang.RuntimeException
import com.badlogic.gdx.graphics.g2d.TextureRegion

class TextureAtlas {
    fun findRegion(path: String?): AtlasRegion {
        throw RuntimeException()
    }

    class AtlasRegion : TextureRegion() {
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
    }
}
