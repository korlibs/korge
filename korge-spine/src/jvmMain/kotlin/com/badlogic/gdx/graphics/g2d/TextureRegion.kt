package com.badlogic.gdx.graphics.g2d

import com.badlogic.gdx.graphics.*

open class TextureRegion {
    val u: Float
        get() {
            throw NotImplementedError()
        }
    val v: Float
        get() {
            throw NotImplementedError()
        }
    val u2: Float
        get() {
            throw NotImplementedError()
        }
    val v2: Float
        get() {
            throw NotImplementedError()
        }
    val texture: Texture
        get() {
            throw NotImplementedError()
        }
    val regionWidth: Float
        get() = 0f
    val regionHeight: Float
        get() = 0f
}
