package com.esotericsoftware.spine.rendering

import com.esotericsoftware.spine.graphics.*
import com.esotericsoftware.spine.utils.Affine2
import com.esotericsoftware.spine.utils.Matrix4

interface PolygonBatch : Batch {

    fun setColor(r: Float, g: Float, b: Float, a: Float) {}

    var color: Color?
        get() = null
        set(tint) {}

    var packedColor: Float
        get() = 0f
        set(packedColor) {}


    fun draw(texture: Texture?, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean) {}

    fun draw(texture: Texture?, x: Float, y: Float, width: Float, height: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean) {}

    fun draw(texture: Texture?, x: Float, y: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int) {}

    fun draw(texture: Texture?, x: Float, y: Float, width: Float, height: Float, u: Float, v: Float, u2: Float, v2: Float) {}

    fun draw(texture: Texture?, x: Float, y: Float) {}

    fun draw(texture: Texture?, x: Float, y: Float, width: Float, height: Float) {}

    fun draw(region: TextureAtlas.AtlasRegion?, x: Float, y: Float) {}

    fun draw(region: TextureAtlas.AtlasRegion?, x: Float, y: Float, width: Float, height: Float) {}

    fun draw(region: TextureAtlas.AtlasRegion?, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float) {}

    fun draw(region: TextureAtlas.AtlasRegion?, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float, clockwise: Boolean) {}

    fun draw(region: TextureAtlas.AtlasRegion?, width: Float, height: Float, transform: Affine2?) {}

    fun flush() {}

    fun disableBlending() {}

    fun enableBlending() {}

    fun dispose() {}

    var projectionMatrix: Matrix4?
        get() = null
        set(projection) {}

    var transformMatrix: Matrix4?
        get() = null
        set(transform) {}

    var shader: ShaderProgram?
        get() = null
        set(newShader) {}

    val isBlendingEnabled: Boolean
        get() = false

    val isDrawing: Boolean
        get() = false
}
