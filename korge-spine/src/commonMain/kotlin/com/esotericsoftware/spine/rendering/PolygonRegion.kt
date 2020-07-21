package com.esotericsoftware.spine.rendering

import com.badlogic.gdx.graphics.TextureRegion

class PolygonRegion {
    val triangles: ShortArray
        get() = ShortArray(0)
    val vertices: FloatArray
        get() = FloatArray(0)
    val region: TextureRegion?
        get() = null
    val textureCoords: FloatArray
        get() = FloatArray(0)
}
