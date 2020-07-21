package com.badlogic.gdx.graphics

import com.badlogic.gdx.files.*
import com.esotericsoftware.spine.rendering.*

class TextureAtlas(val handle: FileHandle) {
    private val dummyTexture = Texture()
    private val dummyRegions = LinkedHashMap<String?, AtlasRegion>()

    fun findRegion(path: String?): AtlasRegion? {
        return dummyRegions.getOrPut(path) { AtlasRegion() }
    }

    inner class AtlasRegion : TextureRegion() {

        var offsetX = 0f

        var offsetY = 0f

        var originalWidth = 0f

        var originalHeight = 0f

        var rotate = false

        var packedHeight = 0f

        var packedWidth = 0f

        var degrees = 0

        override val texture: Texture get() = this@TextureAtlas.dummyTexture
    }
}
