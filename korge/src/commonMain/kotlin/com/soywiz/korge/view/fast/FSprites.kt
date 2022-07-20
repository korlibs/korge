package com.soywiz.korge.view.fast
import com.soywiz.kds.Extra
import com.soywiz.kds.FastArrayList
import com.soywiz.kds.IntArrayList
import com.soywiz.kds.fastArrayListOf
import com.soywiz.kmem.*
import com.soywiz.korag.AG
import com.soywiz.korag.DefaultShaders
import com.soywiz.korag.FragmentShaderDefault
import com.soywiz.korag.shader.Attribute
import com.soywiz.korag.shader.FragmentShader
import com.soywiz.korag.shader.Operand
import com.soywiz.korag.shader.Precision
import com.soywiz.korag.shader.Program
import com.soywiz.korag.shader.Uniform
import com.soywiz.korag.shader.VarType
import com.soywiz.korag.shader.Varying
import com.soywiz.korag.shader.VertexShader
import com.soywiz.korge.render.BatchBuilder2D
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.View
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.radians

@PublishedApi
internal const val FSPRITES_STRIDE = 8

open class FSprites(val maxSize: Int) {
    var size = 0
    val available get() = maxSize - size
    val data = FBuffer(maxSize * FSPRITES_STRIDE * 4)
    val dataColorMul = FBuffer(maxSize * 4)
    private val freeItems = IntArrayList()
    private val i32 = data.i32
    private val f32 = data.f32
    private val icolorMul32 = dataColorMul.i32

    fun uploadVertices(ctx: RenderContext) {
        ctx.fastSpriteBuffer.buffer.upload(data, 0, size * FSPRITES_STRIDE * 4)
        ctx.fastSpriteBufferMul.buffer.upload(dataColorMul, 0, size * 4)
        ctx.fastSpriteBufferTexId.buffer.upload(texIds, 0, size)
    }

    fun unloadVertices(ctx: RenderContext) {
        ctx.fastSpriteBuffer.buffer.upload(data, 0, 0)
    }

    fun FSprite.reset() {
        x = 0f
        y = 0f
        scaleX = 1f
        scaleY = 1f
        radiansf = 0f
        colorMul = Colors.WHITE
    }

    fun alloc(): FSprite =
        FSprite(when {
            freeItems.isNotEmpty() -> freeItems.removeAt(freeItems.size - 1)
            else -> size++ * FSPRITES_STRIDE
        }).also { it.reset() }

    fun free(item: FSprite) {
        freeItems.add(item.id)
        item.colorMul = Colors.TRANSPARENT_BLACK // Hide this
    }

    var FSprite.x: Float get() = f32[offset + 0] ; set(value) { f32[offset + 0] = value }
    var FSprite.y: Float get() = f32[offset + 1] ; set(value) { f32[offset + 1] = value }
    var FSprite.scaleX: Float get() = f32[offset + 2] ; set(value) { f32[offset + 2] = value }
    var FSprite.scaleY: Float get() = f32[offset + 3] ; set(value) { f32[offset + 3] = value }
    var FSprite.radiansf: Float get() = f32[offset + 4] ; set(value) { f32[offset + 4] = value }
    var FSprite.anchorRaw: Int get() = i32[offset + 5] ; set(value) { i32[offset + 5] = value }
    var FSprite.tex0Raw: Int get() = i32[offset + 6] ; set(value) { i32[offset + 6] = value }
    var FSprite.tex1Raw: Int get() = i32[offset + 7] ; set(value) { i32[offset + 7] = value }

    var FSprite.colorMul: RGBA get() = RGBA(icolorMul32[index]) ; set(value) { icolorMul32[index] = value.value }

    var FSprite.angle: Angle get() = radiansf.radians ; set(value) { radiansf = value.radians.toFloat() }

    var FSprite.anchorX: Float get() = unpackAnchorComponent(anchorRaw ushr 0) ; set(value) { anchorRaw = packAnchorComponent(value) or (anchorRaw and 0xFFFF.inv()) }
    var FSprite.anchorY: Float get() = unpackAnchorComponent(anchorRaw ushr 16) ; set(value) { anchorRaw = (packAnchorComponent(value) shl 16) or (anchorRaw and 0xFFFF) }

    fun FSprite.setAnchor(x: Float, y: Float) {
        anchorRaw = packAnchor(x, y)
    }

    fun FSprite.setPos(x: Float, y: Float) = xy(x, y)

    fun FSprite.xy(x: Float, y: Float) {
        this.x = x
        this.y = y
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
    fun createView(vararg texs: Bitmap) = FView(this, texs as Array<Bitmap>)

    private val texIds = ByteArray(maxSize)
    var FSprite.texId: Int get() = texIds[index].toInt() and 0xFF ; set(value) { texIds[index] = value.toByte() }
    fun FSprite.setTexIndex(id: Int) { texId = id }

    class FViewInfo(val nTexs: Int) {
        val texs: Array<Bitmap> = Array(nTexs) { Bitmaps.white.bmp }
        val u_i_texSizeDataN: Array<FloatArray> = Array(texs.size) { FloatArray(2) }
        val olds: Array<FloatArray?> = arrayOfNulls<FloatArray>(texs.size)
        val program = vprograms.getOrElse(texs.size) { error("Only supported up to $MAX_SUPPORTED_TEXTURES textures") }
    }

    class FView(val sprites: FSprites, val texs: Array<Bitmap>) : View() {
        //var tex: Bitmap get() = texs[0]; set(value) { texs[0] = value }

        constructor(sprites: FSprites, tex: Bitmap) : this(sprites, arrayOf(tex))
        var smoothing: Boolean = true
        private val info = FViewInfo(texs.size)

        // @TODO: fallback version when instanced rendering is not supported
        override fun renderInternal(ctx: RenderContext) {
            texs.copyInto(info.texs)
            FSprites.render(ctx, sprites, info, smoothing, globalMatrix, BlendMode.NORMAL.factors)
        }
    }

    companion object {
        const val MAX_SUPPORTED_TEXTURES = 4

        fun render(
            ctx: RenderContext,
            sprites: FSprites,
            info: FViewInfo,
            smoothing: Boolean,
            globalMatrix: Matrix,
            blending: AG.Blending
        ) {
            if (!ctx.isInstancedSupported) {
                println("WARNING: FSprites without instanced rendering support not implemented yet.")
                println("         Please, if you are reading this message, let us know")
                return
            }

            val texs = info.texs
            val u_i_texSizeDataN = info.u_i_texSizeDataN
            val olds = info.olds
            val program = info.program
            ctx.flush()
            ctx.useBatcher { batch ->
                batch.updateStandardUniforms()
                for (n in 0 until texs.size) {
                    val tex = texs[n]
                    val ttex = ctx.agBitmapTextureManager.getTextureBase(tex)
                    u_i_texSizeDataN[n][0] = 1f / ttex.width.toFloat()
                    u_i_texSizeDataN[n][1] = 1f / ttex.height.toFloat()
                    batch.textureUnitN[n].set(ttex.base, smoothing)
                    //println(ttex.base)
                }
                //batch.setTemporalUniform(u_i_texSizeN[0], u_i_texSizeDataN[0]) {
                batch.setTemporalUniforms(u_i_texSizeN, u_i_texSizeDataN, texs.size, olds) { uniforms ->
                    batch.setViewMatrixTemp(globalMatrix) {
                        //ctx.batch.setStateFast()
                        sprites.uploadVertices(ctx)
                        ctx.xyBuffer.buffer.upload(xyData)
                        ctx.ag.drawV2(
                            vertexData = ctx.buffers,
                            program = program,
                            type = AG.DrawType.TRIANGLE_FAN,
                            vertexCount = 4,
                            instances = sprites.size,
                            uniforms = uniforms,
                            //renderState = AG.RenderState(depthFunc = AG.CompareMode.LESS),
                            blending = blending
                        )
                        sprites.unloadVertices(ctx)

                    }
                }
                batch.onInstanceCount(sprites.size)
            }
        }

        //const val STRIDE = 8

        private val xyData = floatArrayOf(0f, 0f, /**/ 1f, 0f, /**/ 1f, 1f, /**/ 0f, 1f)

        val u_i_texSizeN = Array(MAX_SUPPORTED_TEXTURES + 1) { Uniform("u_texSize$it", VarType.Float2) }
        val a_xy = Attribute("a_xy", VarType.Float2, false)

        val a_pos = Attribute("a_rxy", VarType.Float2, false).withDivisor(1)
        val a_scale = Attribute("a_scale", VarType.Float2, true).withDivisor(1)
        val a_angle = Attribute("a_rangle", VarType.Float1, false).withDivisor(1)
        val a_anchor = Attribute("a_axy", VarType.UShort2, true).withDivisor(1)
        val a_uv0 = Attribute("a_uv0", VarType.UShort2, false).withDivisor(1)
        val a_uv1 = Attribute("a_uv1", VarType.UShort2, false).withDivisor(1)
        val a_colMul = Attribute("a_colMul", VarType.Byte4, normalized = true, precision = Precision.LOW).withDivisor(1)

        val a_texId = Attribute("a_texId", VarType.UByte1, normalized = false, precision = Precision.LOW).withDivisor(1)
        val v_TexId = Varying("v_TexId", VarType.Float1, precision = Precision.LOW)

        val RenderContext.xyBuffer by Extra.PropertyThis<RenderContext, AG.VertexData> {
            ag.createVertexData(a_xy)
        }
        val RenderContext.fastSpriteBuffer by Extra.PropertyThis<RenderContext, AG.VertexData> {
            ag.createVertexData(a_pos, a_scale, a_angle, a_anchor, a_uv0, a_uv1)
        }
        val RenderContext.fastSpriteBufferMul by Extra.PropertyThis<RenderContext, AG.VertexData> {
            ag.createVertexData(a_colMul)
        }
        val RenderContext.fastSpriteBufferTexId by Extra.PropertyThis<RenderContext, AG.VertexData> {
            ag.createVertexData(a_texId)
        }
        val RenderContext.buffers by Extra.PropertyThis<RenderContext, FastArrayList<AG.VertexData>> {
            fastArrayListOf(xyBuffer, fastSpriteBuffer, fastSpriteBufferMul, fastSpriteBufferTexId)
        }

        fun createProgram(maxTexs: Int = 4): Program {
            fun Program.Builder.blockN(ref: Operand, block: Program.Builder.(Int) -> Unit) {
                //IF_ELSE_LIST(a_texId, 0, maxTexs - 1, block)
                IF_ELSE_BINARY_LOOKUP(ref, 0, maxTexs - 1, block)
            }

            return Program(VertexShader {
                DefaultShaders.apply {
                    //SET(out, (u_ProjMat * u_ViewMat) * vec4(vec2(a_x, a_y), 0f.lit, 1f.lit))
                    //SET(v_color, texture2D(u_Tex, vec2(vec1(id) / 4f.lit, 0f.lit)))
                    val baseSize = t_Temp1["xy"]
                    val texSize = t_Temp1["zw"]
                    SET(baseSize, a_uv1 - a_uv0)
                    SET(v_Col, a_colMul)
                    SET(v_TexId, a_texId)

                    //SET(texSize, u_i_texSizeN[0])
                    blockN(a_texId) { SET(texSize, u_i_texSizeN[it]) }

                    SET(v_Tex, vec2(
                        mix(a_uv0.x, a_uv1.x, a_xy.x),
                        mix(a_uv0.y, a_uv1.y, a_xy.y),
                    ) * texSize)
                    val cos = t_Temp0["x"]
                    val sin = t_Temp0["y"]
                    SET(cos, cos(a_angle))
                    SET(sin, sin(a_angle))
                    SET(t_TempMat2, mat2(
                        cos, -sin,
                        sin, cos,
                    ))
                    val size = t_Temp0["zw"]
                    val localPos = t_Temp0["xy"]

                    SET(size, baseSize * a_scale)
                    SET(localPos, t_TempMat2 * ((a_xy - a_anchor) * size))
                    SET(out, (u_ProjMat * u_ViewMat) * vec4(localPos + vec2(a_pos.x, a_pos.y), 0f.lit, 1f.lit))
                }
            }, FragmentShaderDefault {
                blockN(v_TexId) { SET(out, texture2D(BatchBuilder2D.u_TexN[it], v_Tex["xy"])) }
                IF(out["a"] le 0f.lit) { DISCARD() }
            }, name = "FSprites$maxTexs")
        }

        val vprograms = Array(MAX_SUPPORTED_TEXTURES + 1) { createProgram(it) }
        //val vprograms by lazy { Array(MAX_SUPPORTED_TEXTURES + 1) { createProgram(it) } }

        private fun unpackAnchorComponent(v: Int): Float = (v and 0xFFFF).toFloat() / 0xFFFF
        private fun packAnchorComponent(v: Float): Int = (v.clamp01() * 0xFFFF).toInt() and 0xFFFF
        fun packAnchor(x: Float, y: Float): Int = (packAnchorComponent(x) and 0xFFFF) or (packAnchorComponent(y) shl 16)
    }
}

inline class FSprite(val id: Int) {
    val offset get() = id
    val index get() = offset / FSPRITES_STRIDE
    //val offset get() = index * STRIDE
}

fun FSpriteFromIndex(index: Int) = FSprite(index * FSPRITES_STRIDE)

inline fun <T : FSprites> T.fastForEach(callback: T.(sprite: FSprite) -> Unit) {
    var m = 0
    for (n in 0 until size) {
        callback(FSprite(m))
        m += FSPRITES_STRIDE
    }
}
