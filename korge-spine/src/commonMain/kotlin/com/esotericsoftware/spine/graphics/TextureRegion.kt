package com.esotericsoftware.spine.graphics

import com.esotericsoftware.spine.rendering.*

open class TextureRegion {
    open val u: Float
        get() = 0f
    open val v: Float
        get() = 0f
    open val u2: Float
        get() = 0f
    open val v2: Float
        get() = 0f
    open val texture: Texture
        get() = TODO()
    open val regionWidth: Float
        get() = 0f
    open val regionHeight: Float
        get() = 0f
}
