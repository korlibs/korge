package com.soywiz.korge3d

import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import kotlin.math.*

private val logger = Logger("BatchBuilder3D")

/**
 * Allows to draw quads and sprites buffering the geometry to limit the draw batches executed calling [AG] (Accelerated Graphics).
 * This class handles a vertex structure of: x, y, u, v, colorMul, colorAdd. Allowing to draw texturized and tinted primitives.
 *
 * You should call: [drawQuad], [drawQuadFast], [drawNinePatch], [drawVertices] for buffering the geometries.
 *
 * For performance the actual drawing/rendering doesn't happen until the [flush] method is called (normally that happens automatically).
 * Then the engine will call [flush] when required, automatically once the buffer is filled, at the end of the frame, or when [RenderContext.flush] is executed
 * by other renderers.
 */
class BatchBuilder3D(
    @KorgeInternal
    val ctx: RenderContext3D,
    /** Maximum number of quads that could be drawn in a single batch.
     * Bigger numbers will increase memory usage, bug might reduce the number of batches per frame when using the same texture and properties.
     */
    val maxQuads: Int = 4096
) {
    val ag: AG = ctx.ag
    init {
        logger.trace { "BatchBuilder3D[0]" }
        ctx.rctx.flushers.add { flush() }
    }

    var flipRenderTexture = true
    //var flipRenderTexture = false
    val maxQuadsMargin = maxQuads + 9

    /** Maximum number of vertices that can be buffered here in a single batch. It depens on the [maxQuads] parameter */
    val maxVertices = maxQuads * 4
    /** Maximum number of indices that can be buffered here in a single batch. It depens on the [maxQuads] parameter */
    val maxIndices = maxQuads * 6

    init { logger.trace { "BatchBuilder3D[1]" } }

    internal val vertices = FBuffer.alloc(6 * 4 * maxVertices)
    internal val indices = FBuffer.alloc(2 * maxIndices)

    init { logger.trace { "BatchBuilder3D[2]" } }

    private var vertexCount = 0
    private var vertexPos = 0
    private var indexPos = 0
    private var currentTex: AG.Texture? = null
    private var currentSmoothing: Boolean = false
    private var currentBlendFactors: AG.Blending = BlendMode.NORMAL.factors
    private var currentProgram: Program? = null

    init { logger.trace { "BatchBuilder3D[3]" } }

    private val vertexBuffer = ag.createVertexBuffer()
    private val indexBuffer = ag.createIndexBuffer()

    init { logger.trace { "BatchBuilder3D[4]" } }

    /** The current stencil state. If you change it, you must call the [flush] method to ensure everything has been drawn. */
    var stencil = AG.StencilState()

    init { logger.trace { "BatchBuilder3D[5]" } }

    /** The current color mask state. If you change it, you must call the [flush] method to ensure everything has been drawn. */
    var colorMask = AG.ColorMaskState()

    init { logger.trace { "BatchBuilder3D[6]" } }

    /** The current scissor state. If you change it, you must call the [flush] method to ensure everything has been drawn. */
    var scissor: AG.Scissor? = null

    private val identity = Matrix()

    init { logger.trace { "BatchBuilder3D[7]" } }

    private val ptt1 = Point()
    private val ptt2 = Point()

    private val pt1 = Point()
    private val pt2 = Point()
    private val pt3 = Point()
    private val pt4 = Point()
    private val pt5 = Point()

    private val pt6 = Point()
    private val pt7 = Point()
    private val pt8 = Point()

    init { logger.trace { "BatchBuilder3D[8]" } }

    private val projMat = Matrix3D()

    @KorgeInternal
    val viewMat = Matrix3D()

    init { logger.trace { "BatchBuilder3D[9]" } }

    private val textureUnit = AG.TextureUnit(null, linear = false)

    init { logger.trace { "BatchBuilder3D[10]" } }

    // @TODO: kotlin-native crash: [1]    80122 segmentation fault  ./sample1-native.kexe
    //private val uniforms = mapOf<Uniform, Any>(
    //	DefaultShaders.u_ProjMat to projMat,
    //	DefaultShaders.u_Tex to textureUnit
    //)
    @PublishedApi
    internal val uniforms by lazy {
        AG.UniformValues(
            DefaultShaders.u_ProjMat to projMat,
            DefaultShaders.u_ViewMat to viewMat,
            DefaultShaders.u_Tex to textureUnit
        )
    }

    init { logger.trace { "BatchBuilder3D[11]" } }

    fun readVertices(): List<VertexInfo> = (0 until vertexCount).map { readVertex(it) }

    fun readVertex(n: Int, out: VertexInfo = VertexInfo()): VertexInfo {
        out.read(this.vertices, n)
        val source = textureUnit.texture?.source
        out.texWidth = source?.width ?: -1
        out.texHeight = source?.height ?: -1
        return out
    }

    // @TODO: copy data from TexturedVertexArray
    private fun addVertex(x: Float, y: Float, u: Float, v: Float, colorMul: RGBA, colorAdd: Int) {
        vertices.setAlignedFloat32(vertexPos++, x)
        vertices.setAlignedFloat32(vertexPos++, y)
        vertices.setAlignedFloat32(vertexPos++, u)
        vertices.setAlignedFloat32(vertexPos++, v)
        vertices.setAlignedInt32(vertexPos++, colorMul.value)
        vertices.setAlignedInt32(vertexPos++, colorAdd)
        vertexCount++
    }

    private fun addIndex(idx: Int) {
        indices.setAlignedInt16(indexPos++, idx.toShort())
    }

    private fun addIndices(i0: Int, i1: Int, i2: Int, i3: Int, i4: Int, i5: Int) {
        addIndex(i0)
        addIndex(i1)
        addIndex(i2)
        addIndex(i3)
        addIndex(i4)
        addIndex(i5)
    }

    /**
     * Draws/buffers a textured ([tex]) and colored ([colorMul] and [colorAdd]) quad with this shape:
     *
     * 0..1
     * |  |
     * 3..2
     *
     * Vertices:
     * 0: [x0], [y0] (top left)
     * 1: [x1], [y1] (top right)
     * 2: [x2], [y2] (bottom right)
     * 3: [x3], [y3] (bottom left)
     */
    fun drawQuadFast(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        tex: Texture,
        colorMul: RGBA, colorAdd: Int,
        /** Not working right now */
        rotated: Boolean = false
    ) {
        ensure(6, 4)

        addIndex(vertexCount + 0)
        addIndex(vertexCount + 1)
        addIndex(vertexCount + 2)

        addIndex(vertexCount + 3)
        addIndex(vertexCount + 0)
        addIndex(vertexCount + 2)

        if (rotated) {
            // @TODO:
            addVertex(x0, y0, tex.x0, tex.y0, colorMul, colorAdd)
            addVertex(x1, y1, tex.x1, tex.y0, colorMul, colorAdd)
            addVertex(x2, y2, tex.x1, tex.y1, colorMul, colorAdd)
            addVertex(x3, y3, tex.x0, tex.y1, colorMul, colorAdd)
        } else {
            addVertex(x0, y0, tex.x0, tex.y0, colorMul, colorAdd)
            addVertex(x1, y1, tex.x1, tex.y0, colorMul, colorAdd)
            addVertex(x2, y2, tex.x1, tex.y1, colorMul, colorAdd)
            addVertex(x3, y3, tex.x0, tex.y1, colorMul, colorAdd)
        }
    }

    /**
     * Draws/buffers a set of textured and colorized array of vertices [array] with the current state previously set by calling [setStateFast].
     */
    fun drawVertices(array: TexturedVertexArray, vcount: Int = array.vcount, icount: Int = array.isize) {
        ensure(icount, vcount)

        for (idx in 0 until min(icount, array.isize)) addIndex(vertexCount + array.indices[idx])
        //for (p in array.points) addVertex(p.x, p.y, p.tx, p.ty, p.colMul, p.colAdd)

        FBuffer.copy(array._data, 0, vertices, vertexPos * 4, vcount * 6 * 4)
        //vertices.setAlignedArrayInt32(vertexPos, array.data, 0, vcount * 6)
        vertexCount += vcount
        vertexPos += vcount * 6
    }

    /**
     * Draws/buffers a set of textured and colorized array of vertices [array] with the specified texture [tex] and optionally [smoothing] it and an optional [program].
     */
    fun drawVertices(array: TexturedVertexArray, tex: Texture.Base, smoothing: Boolean, blendFactors: AG.Blending, vcount: Int = array.vcount, icount: Int = array.isize, program: Program? = null) {
        setStateFast(tex, smoothing, blendFactors, program)
        drawVertices(array, vcount, icount)
    }

    private fun checkAvailable(indices: Int, vertices: Int): Boolean {
        return (this.indexPos + indices < maxIndices) || (this.vertexPos + vertices < maxVertices)
    }

    private fun ensure(indices: Int, vertices: Int) {
        if (!checkAvailable(indices, vertices)) flush()
        if (!checkAvailable(indices, vertices)) error("Too much vertices")
    }

    /**
     * Sets the current texture [tex], [smoothing], [blendFactors] and [program] that will be used by the following drawing calls not specifying these attributes.
     */
    fun setStateFast(tex: Texture.Base, smoothing: Boolean, blendFactors: AG.Blending, program: Program?) =
        setStateFast(tex.base, smoothing, blendFactors, program)

    /**
     * Sets the current texture [tex], [smoothing], [blendFactors] and [program] that will be used by the following drawing calls not specifying these attributes.
     */
    fun setStateFast(tex: AG.Texture?, smoothing: Boolean, blendFactors: AG.Blending, program: Program?) {
        if (tex != currentTex || currentSmoothing != smoothing || currentBlendFactors != blendFactors || currentProgram != program) {
            flush()
            currentTex = tex
            currentSmoothing = smoothing
            currentBlendFactors = if (tex != null && tex.isFbo) blendFactors.toRenderFboIntoBack() else blendFactors
            currentProgram = program
        }
    }


    /**
     * Draws a textured [tex] quad at [x], [y] and size [width]x[height].
     *
     * It uses [m] transform matrix, an optional [filtering] and [colorMul], [colorAdd], [blendFactors] and [program] as state for drawing it.
     *
     * Note: To draw solid quads, you can use [Bitmaps.white] + [AgBitmapTextureManager] as texture and the [colorMul] as quad color.
     */
    fun drawQuad(
        tex: Texture,
        x: Float = 0f,
        y: Float = 0f,
        width: Float = tex.width.toFloat(),
        height: Float = tex.height.toFloat(),
        m: Matrix = identity,
        filtering: Boolean = true,
        colorMul: RGBA = Colors.WHITE,
        colorAdd: Int = 0x7f7f7f7f,
        blendFactors: AG.Blending = BlendMode.NORMAL.factors,
        /** Not working right now */
        rotated: Boolean = false,
        program: Program? = null
    ) {
        val x0 = x.toDouble()
        val x1 = (x + width).toDouble()
        val y0 = y.toDouble()
        val y1 = (y + height).toDouble()

        setStateFast(tex.base, filtering, blendFactors, program)

        drawQuadFast(
            m.fastTransformXf(x0, y0), m.fastTransformYf(x0, y0),
            m.fastTransformXf(x1, y0), m.fastTransformYf(x1, y0),
            m.fastTransformXf(x1, y1), m.fastTransformYf(x1, y1),
            m.fastTransformXf(x0, y1), m.fastTransformYf(x0, y1),
            tex, colorMul, colorAdd, rotated
        )
    }

    companion object {
        init { logger.trace { "BatchBuilder3D.Companion[0]" } }

        @KorgeInternal
        val a_ColMul = DefaultShaders.a_Col
        @KorgeInternal
        val a_ColAdd = Attribute("a_Col2", VarType.Byte4, normalized = true)

        init { logger.trace { "BatchBuilder3D.Companion[1]" } }

        @KorgeInternal
        val v_ColMul = DefaultShaders.v_Col
        @KorgeInternal
        val v_ColAdd = Varying("v_Col2", VarType.Byte4)

        init { logger.trace { "BatchBuilder3D.Companion[2]" } }

        @KorgeInternal
        val LAYOUT = VertexLayout(DefaultShaders.a_Pos, DefaultShaders.a_Tex, a_ColMul, a_ColAdd)
        @KorgeInternal
        val VERTEX = VertexShader {
            DefaultShaders.apply {
                SET(v_Tex, a_Tex)
                SET(v_ColMul, a_ColMul)
                SET(v_ColAdd, a_ColAdd)
                SET(out, (u_ProjMat * u_ViewMat) * vec4(DefaultShaders.a_Pos, 0f.lit, 1f.lit))
            }
        }

        init { logger.trace { "BatchBuilder3D.Companion[3]" } }

        @KorgeInternal
        val FRAGMENT_PRE = buildTextureLookupFragment(premultiplied = true)

        @KorgeInternal
        val FRAGMENT_NOPRE = buildTextureLookupFragment(premultiplied = false)

        @KorgeInternal
        val PROGRAM_PRE = Program(
            vertex = VERTEX,
            fragment = FRAGMENT_PRE,
            name = "BatchBuilder3D.Premultiplied.Tinted"
        )

        @KorgeInternal
        val PROGRAM_NOPRE = Program(
            vertex = VERTEX,
            fragment = FRAGMENT_NOPRE,
            name = "BatchBuilder3D.NoPremultiplied.Tinted"
        )

        init { logger.trace { "BatchBuilder3D.Companion[4]" } }

        @KorgeInternal
        fun getTextureLookupProgram(premultiplied: Boolean) = if (premultiplied) PROGRAM_PRE else PROGRAM_NOPRE

        //val PROGRAM_NORMAL = Program(
        //	vertex = VERTEX,
        //	fragment = FragmentShader {
        //		SET(out, texture2D(DefaultShaders.u_Tex, DefaultShaders.v_Tex["xy"])["rgba"] * v_Col2["rgba"])
        //		SET(out, out + v_Col2)
        //		// Required for shape masks:
        //		IF(out["a"] le 0f.lit) { DISCARD() }
        //	},
        //	name = "BatchBuilder2D.Tinted"
        //)

        fun getTextureLookupFragment(premultiplied: Boolean) = if (premultiplied) FRAGMENT_PRE else FRAGMENT_NOPRE

        /**
         * Builds a [FragmentShader] for textured and colored drawing that works matching if the texture is [premultiplied]
         */
        @KorgeInternal
        fun buildTextureLookupFragment(premultiplied: Boolean) = FragmentShader {
            DefaultShaders.apply {
                SET(out, texture2D(u_Tex, v_Tex["xy"]))
                if (premultiplied) {
                    SET(out["rgb"], out["rgb"] / out["a"])
                }

                // @TODO: Kotlin.JS bug?
                //SET(out, (out["rgba"] * v_ColMul["rgba"]) + ((v_ColAdd["rgba"] - vec4(.5f, .5f, .5f, .5f)) * 2f))
                SET(out, (out["rgba"] * v_ColMul["rgba"]) + ((v_ColAdd["rgba"] - vec4(.5f.lit, .5f.lit, .5f.lit, .5f.lit)) * 2f.lit))

                //SET(out, t_Temp1)
                // Required for shape masks:
                if (premultiplied) {
                    IF(out["a"] le 0f.lit) { DISCARD() }
                }
            }
        }

        //init { println(PROGRAM_PRE.fragment.toGlSl()) }
    }

    private val tempRect = Rectangle()
    val beforeFlush = Signal<BatchBuilder3D>()

    /** When there are vertices pending, this performs a [AG.draw] call flushing all the buffered geometry pending to draw */
    fun flush() {
        if (vertexCount > 0) {

            val factors = currentBlendFactors

            vertexBuffer.upload(vertices, 0, vertexPos * 4)
            indexBuffer.upload(indices, 0, indexPos * 2)

            textureUnit.texture = currentTex
            textureUnit.linear = currentSmoothing

            //println("MyUniforms: $uniforms")

            val realFactors = if (ag.renderingToTexture) factors.toRenderImageIntoFbo() else factors

            //println("RENDER: $realFactors")

            ag.draw(
                vertices = vertexBuffer,
                indices = indexBuffer,
                program = currentProgram ?: (if (currentTex?.premultiplied == true) PROGRAM_PRE else PROGRAM_NOPRE),
                //program = PROGRAM_PRE,
                type = AG.DrawType.TRIANGLES,
                vertexLayout = LAYOUT,
                vertexCount = indexPos,
                blending = realFactors,
                uniforms = uniforms,
                stencil = stencil,
                colorMask = colorMask,
                scissor = scissor
            )
            beforeFlush(this)
        }

        vertexCount = 0
        vertexPos = 0
        indexPos = 0
        currentTex = null
    }

    //private fun AG.Blending.toTextureRender(): AG.Blending {
    //	//println("toTextureRender")
    //	return when (this) {
    //		BlendMode.NORMAL.factors -> BlendMode.ToTexture.NORMAL
    //		//BlendMode.NORMAL.factors -> BlendMode.NORMAL.factors
    //		else -> this
    //	}
    //}

    /**
     * Executes [callback] while setting temporarily the view matrix to [matrix]
     */
    inline fun setViewMatrixTemp(matrix: Matrix, temp: Matrix3D = Matrix3D(), callback: () -> Unit) {
        flush()
        temp.copyFrom(this.viewMat)
        this.viewMat.copyFrom(matrix)
        //println("viewMat: $viewMat, matrix: $matrix")
        try {
            callback()
        } finally {
            flush()
            this.viewMat.copyFrom(temp)
        }
    }

    /**
     * Executes [callback] while setting temporarily an [uniform] to a [value]
     */
    inline fun setTemporalUniform(uniform: Uniform, value: Any?, callback: () -> Unit) {
        val old = this.uniforms[uniform]
        this.uniforms.putOrRemove(uniform, value)
        try {
            callback()
        } finally {
            this.uniforms.putOrRemove(uniform, old)
        }
    }

    @PublishedApi
    internal val tempOldUniforms = AG.UniformValues()

    /**
     * Executes [callback] while setting temporarily a set of [uniforms]
     */
    inline fun setTemporalUniforms(uniforms: AG.UniformValues, callback: () -> Unit) {
        flush()
        tempOldUniforms.setTo(this.uniforms)
        this.uniforms.put(uniforms)
        try {
            callback()
        } finally {
            flush()
            this.uniforms.setTo(tempOldUniforms)
        }
    }
}
