package com.soywiz.korge.view.fast

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*

open class FSprites(val maxSize: Int) {
    var size = 0
    val data = FBuffer(maxSize * 8/*STRIDE*/ * 4)
    private val i32 = data.i32
    private val f32 = data.f32

    fun uploadVertices(ctx: RenderContext) {
        ctx.fastSpriteBuffer.buffer.upload(data, 0, size * 8/*STRIDE*/ * 4)
    }

    fun unloadVertices(ctx: RenderContext) {
        ctx.fastSpriteBuffer.buffer.upload(data, 0, 0)
    }

    fun alloc() = FSprite(size++ * 8/*STRIDE*/)

    var FSprite.x: Float get() = f32[offset + 0] ; set(value) { f32[offset + 0] = value }
    var FSprite.y: Float get() = f32[offset + 1] ; set(value) { f32[offset + 1] = value }
    var FSprite.scaleX: Float get() = f32[offset + 2] ; set(value) { f32[offset + 2] = value }
    var FSprite.scaleY: Float get() = f32[offset + 3] ; set(value) { f32[offset + 3] = value }
    var FSprite.radiansf: Float get() = f32[offset + 4] ; set(value) { f32[offset + 4] = value }
    var FSprite.anchorRaw: Int get() = i32[offset + 5] ; set(value) { i32[offset + 5] = value }
    var FSprite.tex0Raw: Int get() = i32[offset + 6] ; set(value) { i32[offset + 6] = value }
    var FSprite.tex1Raw: Int get() = i32[offset + 7] ; set(value) { i32[offset + 7] = value }

    var FSprite.angle: Angle get() = radiansf.radians ; set(value) { radiansf = value.radians.toFloat() }

    fun FSprite.setAnchor(x: Float, y: Float) {
        anchorRaw = packAnchor(x, y)
    }

    fun FSprite.scale(sx: Float, sy: Float = sx) {
        this.scaleX = sx
        this.scaleY = sy
    }

    fun FSprite.setTex(tex: BmpSlice) {
        tex0Raw = tex.left or (tex.top shl 16)
        tex1Raw = tex.right or (tex.bottom shl 16)
    }

    fun createView(tex: Bitmap) = FView(this, tex)

    class FView(val sprites: FSprites, var tex: Bitmap) : View() {
        var smoothing: Boolean = true
        private val xyData = floatArrayOf(0f, 0f, /**/ 1f, 0f, /**/ 1f, 1f, /**/ 0f, 1f)
        private val u_i_texSizeData = FloatArray(2)

        // @TODO: fallback version when instanced rendering is not supported
        override fun renderInternal(ctx: RenderContext) {
            ctx.flush()
            val ttex = ctx.agBitmapTextureManager.getTextureBase(tex)
            u_i_texSizeData[0] = 1f / ttex.width.toFloat()
            u_i_texSizeData[1] = 1f / ttex.height.toFloat()
            ctx.batch.setTemporalUniform(u_i_texSize, u_i_texSizeData) {
                ctx.batch.updateStandardUniforms()
                ctx.batch.setViewMatrixTemp(globalMatrix) {
                    ctx.batch.textureUnit.texture = ttex.base
                    ctx.batch.textureUnit.linear = smoothing
                    sprites.uploadVertices(ctx)
                    ctx.xyBuffer.buffer.upload(xyData)
                    ctx.ag.drawV2(
                        vertexData = ctx.buffers,
                        program = vprogram,
                        type = AG.DrawType.TRIANGLE_FAN,
                        vertexCount = 4,
                        instances = sprites.size,
                        uniforms = ctx.batch.uniforms
                    )
                    sprites.unloadVertices(ctx)

                }
            }
            ctx.batch.onInstanceCount(sprites.size)
        }
    }

    companion object {
        //const val STRIDE = 8

        val u_i_texSize = Uniform("u_texSize", VarType.Float2)
        val v_color = Varying("v_color", VarType.Float4)
        val a_xy = Attribute("a_xy", VarType.Float2, false)

        val a_pos = Attribute("a_rxy", VarType.Float2, false).withDivisor(1)
        val a_scale = Attribute("a_scale", VarType.Float2, true).withDivisor(1)
        val a_angle = Attribute("a_rangle", VarType.Float1, false).withDivisor(1)
        val a_anchor = Attribute("a_axy", VarType.SShort2, true).withDivisor(1)
        val a_uv0 = Attribute("a_uv0", VarType.UShort2, false).withDivisor(1)
        val a_uv1 = Attribute("a_uv1", VarType.UShort2, false).withDivisor(1)

        val RenderContext.xyBuffer by Extra.PropertyThis<RenderContext, AG.VertexData> {
            ag.createVertexData(a_xy)
        }
        val RenderContext.fastSpriteBuffer by Extra.PropertyThis<RenderContext, AG.VertexData> {
            ag.createVertexData(a_pos, a_scale, a_angle, a_anchor, a_uv0, a_uv1)
        }
        val RenderContext.buffers by Extra.PropertyThis<RenderContext, List<AG.VertexData>> {
            listOf(xyBuffer, fastSpriteBuffer)
        }

        val vprogram = Program(VertexShader {
            DefaultShaders.apply {
                //SET(out, (u_ProjMat * u_ViewMat) * vec4(vec2(a_x, a_y), 0f.lit, 1f.lit))
                //SET(v_color, texture2D(u_Tex, vec2(vec1(id) / 4f.lit, 0f.lit)))
                val baseSize = t_Temp1["xy"]
                SET(baseSize, a_uv1 - a_uv0)

                SET(
                    v_Tex, vec2(
                    mix(a_uv0.x, a_uv1.x, a_xy.x),
                    mix(a_uv0.y, a_uv1.y, a_xy.y),
                ) * u_i_texSize)
                val cos = t_Temp0["x"]
                val sin = t_Temp0["y"]
                SET(cos, cos(a_angle))
                SET(sin, sin(a_angle))
                SET(
                    t_TempMat2, mat2(
                    cos, -sin,
                    sin, cos,
                ))
                val size = t_Temp0["zw"]
                val localPos = t_Temp0["xy"]

                //SET(size, (a_scale * ((a_uv1 - a_uv0) / u_texSize) * 10f.lit))
                SET(size, baseSize)
                SET(localPos, t_TempMat2 * (a_xy - a_anchor) * size)
                SET(out, (u_ProjMat * u_ViewMat) * vec4(localPos + vec2(a_pos.x, a_pos.y), 0f.lit, 1f.lit))
            }
        }, FragmentShader {
            DefaultShaders.apply {
                SET(out, texture2D(u_Tex, v_Tex["xy"]))
                //SET(out, vec4(1f.lit, 0f.lit, 1f.lit, .5f.lit))
                IF(out["a"] le 0f.lit) { DISCARD() }
                SET(out["rgb"], out["rgb"] / out["a"])
            }
        })

        private fun packAnchorComponent(v: Float): Int {
            return (((v + 1f) * .5f) * 0xFFFF).toInt() and 0xFFFF
        }

        fun packAnchor(x: Float, y: Float): Int {
            return (packAnchorComponent(x) and 0xFFFF) or (packAnchorComponent(y) shl 16)
        }
    }
}

inline class FSprite(val id: Int) {
    val offset get() = id
    val index get() = offset / 8/*STRIDE*/
    //val offset get() = index * 8/*STRIDE*/
}

inline fun <T : FSprites> T.fastForEach(callback: T.(sprite: FSprite) -> Unit) {
    var m = 0
    for (n in 0 until size) {
        callback(FSprite(m))
        m += 8/*STRIDE*/
    }
}
