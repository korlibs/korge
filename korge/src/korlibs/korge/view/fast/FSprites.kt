package korlibs.korge.view.fast
import korlibs.datastructure.*
import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.korge.render.*
import korlibs.korge.view.*
import korlibs.korge.view.BlendMode
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.memory.*

@PublishedApi
internal const val FSPRITES_STRIDE = 8

open class FSprites(val maxSize: Int) {
    var size = 0
    val available: Int get() = maxSize - size
    val data = Buffer(maxSize * FSPRITES_STRIDE * 4)
    val dataColorMul = Buffer(maxSize * 4)
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
        item.colorMul = Colors.TRANSPARENT // Hide this
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
        val u_i_texSizeDataN: Array<Vector2F> = Array(texs.size) { Vector2F() }
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
            render(ctx, sprites, info, smoothing, globalMatrix, renderBlendMode)
        }
    }

    object FspritesUB : UniformBlock(fixedLocation = 6) {
        val u_i_texSizeN = Array(MAX_SUPPORTED_TEXTURES + 1) { vec2("u_texSize$it").uniform }
    }

    private val textureUnits = AGTextureUnits()

    companion object {
        const val MAX_SUPPORTED_TEXTURES = 4

        fun render(
            ctx: RenderContext,
            sprites: FSprites,
            info: FViewInfo,
            smoothing: Boolean,
            globalMatrix: Matrix,
            blending: BlendMode,
        ) {
            if (!ctx.isInstancedSupported) {
                println("WARNING: FSprites without instanced rendering support not implemented yet.")
                println("         Please, if you are reading this message, let us know")
                return
            }

            val textureUnits = sprites.textureUnits

            val texs = info.texs
            val u_i_texSizeDataN = info.u_i_texSizeDataN
            val program = info.program
            ctx.flush()
            ctx.useBatcher { batch ->
                batch.updateStandardUniforms()
                //batch.setTemporalUniform(u_i_texSizeN[0], u_i_texSizeDataN[0]) {
                for (n in 0 until texs.size) {
                    val tex = texs[n]
                    val ttex = ctx.agBitmapTextureManager.getTextureBase(tex)
                    u_i_texSizeDataN[n] = Vector2F(1f / ttex.width.toFloat(), 1f / ttex.height.toFloat())
                    textureUnits.set(BatchBuilder2D.u_TexN[n], ttex.base, AGTextureUnitInfo(linear = smoothing))
                    //println(ttex.base)
                }
                ctx[FspritesUB].push {
                    for (n in 0 until texs.size) it[u_i_texSizeN[n]] = u_i_texSizeDataN[n]
                }
                batch.setViewMatrixTemp(globalMatrix) {
                    //ctx.batch.setStateFast()
                    sprites.uploadVertices(ctx)
                    ctx.xyBuffer.buffer.upload(xyData)
                    ctx.ag.draw(
                        ctx.currentFrameBuffer,
                        vertexData = ctx.buffers,
                        program = program,
                        drawType = AGDrawType.TRIANGLE_FAN,
                        vertexCount = 4,
                        instances = sprites.size,
                        uniformBlocks = ctx.createCurrentUniformsRef(program),
                        textureUnits = textureUnits.clone(),
                        //renderState = AGRenderState(depthFunc = AGCompareMode.LESS),
                        blending = blending.factors
                    )
                    sprites.unloadVertices(ctx)
                }
                batch.onInstanceCount(sprites.size)
            }
        }

        //const val STRIDE = 8

        private val xyData = floatArrayOf(0f, 0f, /**/ 1f, 0f, /**/ 1f, 1f, /**/ 0f, 1f)

        val u_i_texSizeN = Array(MAX_SUPPORTED_TEXTURES + 1) { FspritesUB.u_i_texSizeN[it].uniform }

        val a_xy = Attribute("a_xy", VarType.Float2, false, fixedLocation = 0)

        val a_pos = Attribute("a_rxy", VarType.Float2, false, fixedLocation = 1).withDivisor(1)
        val a_scale = Attribute("a_scale", VarType.Float2, true, fixedLocation = 2).withDivisor(1)
        val a_angle = Attribute("a_rangle", VarType.Float1, false, fixedLocation = 3).withDivisor(1)
        val a_anchor = Attribute("a_axy", VarType.UShort2, true, fixedLocation = 4).withDivisor(1)
        val a_uv0 = Attribute("a_uv0", VarType.UShort2, false, fixedLocation = 5).withDivisor(1)
        val a_uv1 = Attribute("a_uv1", VarType.UShort2, false, fixedLocation = 6).withDivisor(1)
        val a_colMul = Attribute("a_colMul", VarType.Byte4, normalized = true, precision = Precision.LOW, fixedLocation = 7).withDivisor(1)

        val a_texId = Attribute("a_texId", VarType.UByte1, normalized = false, precision = Precision.LOW, fixedLocation = 8).withDivisor(1)
        val v_TexId = Varying("v_TexId", VarType.Float1, precision = Precision.LOW)

        val RenderContext.xyBuffer by Extra.PropertyThis<RenderContext, AGVertexData> {
            AGVertexData(a_xy)
        }
        val RenderContext.fastSpriteBuffer by Extra.PropertyThis<RenderContext, AGVertexData> {
            AGVertexData(a_pos, a_scale, a_angle, a_anchor, a_uv0, a_uv1)
        }
        val RenderContext.fastSpriteBufferMul by Extra.PropertyThis<RenderContext, AGVertexData> {
            AGVertexData(a_colMul)
        }
        val RenderContext.fastSpriteBufferTexId by Extra.PropertyThis<RenderContext, AGVertexData> {
            AGVertexData(a_texId)
        }
        val RenderContext.buffers by Extra.PropertyThis<RenderContext, AGVertexArrayObject> {
            AGVertexArrayObject(xyBuffer, fastSpriteBuffer, fastSpriteBufferMul, fastSpriteBufferTexId, isDynamic = false)
        }

        fun createProgram(maxTexs: Int = 4): Program {
            fun Program.Builder.blockN(ref: Operand, block: Program.Builder.(Int) -> Unit) {
                //IF_ELSE_LIST(a_texId, 0, maxTexs - 1, block)
                IF_ELSE_BINARY_LOOKUP(ref, 0, maxTexs - 1, block)
            }

            return Program(VertexShaderDefault {
                //SET(out, (u_ProjMat * u_ViewMat) * vec4(vec2(a_x, a_y), 0f.lit, 1f.lit))
                //SET(v_color, texture2D(u_Tex, vec2(vec1(id) / 4f.lit, 0f.lit)))
                val baseSize = TEMP(VarType.Float2)
                val texSize = TEMP(VarType.Float2)
                SET(baseSize, a_uv1 - a_uv0)
                SET(v_Col, vec4(a_colMul["rgb"] * a_colMul["a"], a_colMul["a"])) // Pre-multiply color here
                SET(v_TexId, a_texId)

                //SET(texSize, u_i_texSizeN[0])
                blockN(a_texId) { SET(texSize, FspritesUB.u_i_texSizeN[it]) }

                SET(v_Tex, vec2(
                    mix(a_uv0.x, a_uv1.x, a_xy.x),
                    mix(a_uv0.y, a_uv1.y, a_xy.y),
                ) * texSize)

                val cos = TEMP(VarType.Float1)
                val sin = TEMP(VarType.Float1)
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
            }, FragmentShaderDefault {
                blockN(v_TexId) { SET(out, texture2D(BatchBuilder2D.u_TexN[it], v_Tex["xy"])) }
                SET(out, out * v_Col)
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
