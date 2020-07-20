package com.badlogic.gdx.graphics.g2d

import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Affine2
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.graphics.glutils.ShaderProgram

interface PolygonBatch : Batch {
    @JvmDefault
    fun setColor(r: Float, g: Float, b: Float, a: Float) {}
    @JvmDefault
    var color: Color?
        get() = null
        set(tint) {}
    @JvmDefault
    var packedColor: Float
        get() = 0f
        set(packedColor) {}

    @JvmDefault
    fun draw(texture: Texture?, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean) {}
    @JvmDefault
    fun draw(texture: Texture?, x: Float, y: Float, width: Float, height: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean) {}
    @JvmDefault
    fun draw(texture: Texture?, x: Float, y: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int) {}
    @JvmDefault
    fun draw(texture: Texture?, x: Float, y: Float, width: Float, height: Float, u: Float, v: Float, u2: Float, v2: Float) {}
    @JvmDefault
    fun draw(texture: Texture?, x: Float, y: Float) {}
    @JvmDefault
    fun draw(texture: Texture?, x: Float, y: Float, width: Float, height: Float) {}
    @JvmDefault
    fun draw(region: TextureRegion?, x: Float, y: Float) {}
    @JvmDefault
    fun draw(region: TextureRegion?, x: Float, y: Float, width: Float, height: Float) {}
    @JvmDefault
    fun draw(region: TextureRegion?, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float) {}
    @JvmDefault
    fun draw(region: TextureRegion?, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float, clockwise: Boolean) {}
    @JvmDefault
    fun draw(region: TextureRegion?, width: Float, height: Float, transform: Affine2?) {}
    @JvmDefault
    fun flush() {}
    @JvmDefault
    fun disableBlending() {}
    @JvmDefault
    fun enableBlending() {}
    @JvmDefault
    fun dispose() {}
    @JvmDefault
    var projectionMatrix: Matrix4?
        get() = null
        set(projection) {}
    @JvmDefault
    var transformMatrix: Matrix4?
        get() = null
        set(transform) {}
    @JvmDefault
    var shader: ShaderProgram?
        get() = null
        set(newShader) {}
    @JvmDefault
    val isBlendingEnabled: Boolean
        get() = false
    @JvmDefault
    val isDrawing: Boolean
        get() = false
}
