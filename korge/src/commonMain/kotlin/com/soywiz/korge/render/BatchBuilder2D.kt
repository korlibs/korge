@file:OptIn(KorgeInternal::class)

package com.soywiz.korge.render

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import kotlin.jvm.*
import kotlin.math.*
import kotlin.native.concurrent.*

@SharedImmutable
private val logger = Logger("BatchBuilder2D")

/**
 * Allows to draw quads and sprites buffering the geometry to limit the draw batches executed calling [AG] (Accelerated Graphics).
 * This class handles a vertex structure of: x, y, u, v, colorMul. Allowing to draw texturized and tinted primitives.
 *
 * You should call: [drawQuad], [drawQuadFast], [drawNinePatch], [drawVertices] for buffering the geometries.
 *
 * For performance the actual drawing/rendering doesn't happen until the [flush] method is called (normally that happens automatically).
 * Then the engine will call [flush] when required, automatically once the buffer is filled, at the end of the frame, or when [RenderContext.flush] is executed
 * by other renderers.
 */
// @TODO: We could dynamically select a fragment shader based on the number of textures to reduce the numbers of IFs per pixel
class BatchBuilder2D constructor(
    @property:KorgeInternal
    val ctx: RenderContext,
    /** Maximum number of quads that could be drawn in a single batch.
     * Bigger numbers will increase memory usage, but might reduce the number of batches per frame when using the same texture and properties.
     */
    val reqMaxQuads: Int = DEFAULT_BATCH_QUADS
) {
    val maxTextures = BB_MAX_TEXTURES

    @KorgeInternal
    val viewMat: MMatrix3D get() = ctx.viewMat
    @KorgeInternal
    val viewMat2D: MMatrix get() = ctx.viewMat2D
    @KorgeInternal
    val uniforms: AGUniformValues get() = ctx.uniforms

    inline fun use(block: (BatchBuilder2D) -> Unit) = ctx.useBatcher(this, block)

    val maxQuads: Int = min(reqMaxQuads, MAX_BATCH_QUADS)

    val texManager = ctx.agBitmapTextureManager
    //constructor(ag: AG, maxQuads: Int = DEFAULT_BATCH_QUADS) : this(RenderContext(ag), maxQuads)
    val ag: AG = ctx.ag
	init {
        logger.trace { "BatchBuilder2D[0]" }
        ctx.flushers.add {
            when (it) {
                RenderContext.FlushKind.STATE -> createBatchIfRequired()
                RenderContext.FlushKind.FULL -> flush()
            }
        }
    }

    /** Maximum number of vertices that can be buffered here in a single batch. It depens on the [maxQuads] parameter */
	val maxVertices = maxQuads * 4
    /** Maximum number of indices that can be buffered here in a single batch. It depens on the [maxQuads] parameter */
	val maxIndices = maxQuads * 6

	init { logger.trace { "BatchBuilder2D[1]" } }

    @PublishedApi internal val vertices = Buffer(6 * 4 * maxVertices)
    @PublishedApi internal val verticesTexIndex = ByteArray(maxVertices)
    @PublishedApi internal val indices = Buffer(2 * maxIndices)
    //@PublishedApi internal val indices = ShortArray(maxIndices)
    //internal val vertices = Buffer.allocNoDirect(6 * 4 * maxVertices)
    //internal val indices = Buffer.allocNoDirect(2 * maxIndices)
    val indicesI16 = indices.u16
    //val indicesI16 = indices

	init { logger.trace { "BatchBuilder2D[2]" } }

    @PublishedApi internal var _vertexCount = 0

    var vertexCount: Int
        get() = _vertexCount
        //set(value) { _vertexCount = value }
        internal set(value) { _vertexCount = value }

    @PublishedApi internal var vertexPos = 0
    @PublishedApi internal var indexPos = 0
    @PublishedApi internal var currentTexIndex = 0

    @PublishedApi internal var currentTexN: Array<AGTexture?> = Array(maxTextures) { null }

	//@PublishedApi internal var currentTex0: AG.Texture? = null
    //@PublishedApi internal var currentTex1: AG.Texture? = null

    @PublishedApi internal var currentSmoothing: Boolean = false

    @PublishedApi internal var currentBlendMode: BlendMode = BlendMode.NORMAL
    @PublishedApi internal var currentProgram: Program = PROGRAM

	init { logger.trace { "BatchBuilder2D[3]" } }

    class BatchBuffers {
        val vertexBuffer = AGBuffer()
        val texIndexVertexBuffer = AGBuffer()
        val indexBuffer = AGBuffer()
        val vertexData = AGVertexArrayObject(
            AGVertexData(LAYOUT, vertexBuffer),
            AGVertexData(LAYOUT_TEX_INDEX, texIndexVertexBuffer),
        )
    }

    val buffers = ReturnablePool { BatchBuffers() }

    internal fun beforeRender() {
        for (n in 0 until maxTextures) currentTexN[n] = null
        currentTexIndex = 0
    }

    internal fun afterRender() {
        buffers.reset()
    }

	init { logger.trace { "BatchBuilder2D[4]" } }

    /** The current stencil state. If you change it, you must call the [flush] method to ensure everything has been drawn. */
	var stencilRef = AGStencilReference.DEFAULT
    var stencilOpFunc = AGStencilOpFunc.DEFAULT

	init { logger.trace { "BatchBuilder2D[5]" } }

    /** The current color mask state. If you change it, you must call the [flush] method to ensure everything has been drawn. */
	var colorMask = AGColorMask()

	init { logger.trace { "BatchBuilder2D[6]" } }

    /** The current scissor state. If you change it, you must call the [flush] method to ensure everything has been drawn. */
	@PublishedApi internal var _scissor: AGScissor = AGScissor.NIL

    val scissor: AGScissor get() = _scissor

    inline fun <T> scissor(scissor: AGScissor, block: () -> T): T {
        val temp = this._scissor
        flushPartial()
        this._scissor = scissor
        try {
            return block()
        } finally {
            flushPartial()
            this._scissor = temp
        }
    }


    private val identity = MMatrix()

	init { logger.trace { "BatchBuilder2D[7]" } }

    //@KorgeInternal val textureUnit0 = AG.TextureUnit(null, linear = false)
    //@KorgeInternal val textureUnit1 = AG.TextureUnit(null, linear = false)

	// @TODO: kotlin-native crash: [1]    80122 segmentation fault  ./sample1-native.kexe
	//private val uniforms = mapOf<Uniform, Any>(
	//	DefaultShaders.u_ProjMat to projMat,
	//	DefaultShaders.u_Tex to textureUnit
	//)

    init {
        for (n in 0 until maxTextures) ctx.uniforms.set(u_TexN[n], null, AGTextureUnitInfo())
    }

	init { logger.trace { "BatchBuilder2D[11]" } }

    fun readVertices(): List<VertexInfo> = (0 until vertexCount).map { readVertex(it) }

    fun readVertex(n: Int, out: VertexInfo = VertexInfo()): VertexInfo {
        out.read(this.vertices, n)
        val source = currentTexN[0]
        out.texWidth = source?.width ?: -1
        out.texHeight = source?.height ?: -1
        return out
    }

	// @TODO: copy data from TexturedVertexArray
	fun addVertex(x: Float, y: Float, u: Float, v: Float, colorMul: RGBA, texIndex: Int = currentTexIndex) {
        vertexPos += _addVertex(vertices, vertexPos, x, y, u, v, colorMul.value, texIndex)
        vertexCount++
	}

    fun _addVertex(vd: Buffer, vp: Int, x: Float, y: Float, u: Float, v: Float, colorMul: Int, texIndex: Int = currentTexIndex): Int {
        vd.setFloat32(vp + 0, x)
        vd.setFloat32(vp + 1, y)
        vd.setFloat32(vp + 2, u)
        vd.setFloat32(vp + 3, v)
        vd.setInt32(vp + 4, colorMul)
        vd.setInt32(vp + 5, 0)
        verticesTexIndex[vp / 6] = texIndex.toByte()
        //println("texIndex.toByte()=${texIndex.toByte()}")
        return TEXTURED_ARRAY_COMPONENTS_PER_VERTEX
    }

	inline fun addIndex(idx: Int) {
		indicesI16[indexPos++] = idx
	}

    inline fun addIndexRelative(idx: Int) {
        indicesI16[indexPos++] = (vertexCount + idx)
    }

	private fun _addIndices(indicesI16: Uint16Buffer, pos: Int, i0: Int, i1: Int, i2: Int, i3: Int, i4: Int, i5: Int): Int {
        indicesI16[pos + 0] = i0
        indicesI16[pos + 1] = i1
        indicesI16[pos + 2] = i2
        indicesI16[pos + 3] = i3
        indicesI16[pos + 4] = i4
        indicesI16[pos + 5] = i5
        return 6
	}

    /**
     * Draws/buffers a textured ([tex]) and colored [colorMul] quad with this shape:
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
		tex: BmpCoords,
		colorMul: RGBA,
        texIndex: Int = currentTexIndex,
	) {
        //println("drawQuadFast[${ag.currentRenderBuffer}, renderingToTexture=${ag.renderingToTexture}]: ($x0,$y0)-($x2,$y2) tex=$tex")
        //println("viewMat=$viewMat, projMat=$projMat")

        ensure(6, 4)
        addQuadIndices()
        var vp = vertexPos
        val vd = vertices
        vp += _addVertex(vd, vp, x0, y0, tex.tlX, tex.tlY, colorMul.value, texIndex)
        vp += _addVertex(vd, vp, x1, y1, tex.trX, tex.trY, colorMul.value, texIndex)
        vp += _addVertex(vd, vp, x2, y2, tex.brX, tex.brY, colorMul.value, texIndex)
        vp += _addVertex(vd, vp, x3, y3, tex.blX, tex.blY, colorMul.value, texIndex)
        vertexPos = vp
        vertexCount += 4
	}

    fun drawQuadFast(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        tx0: Float, ty0: Float,
        tx1: Float, ty1: Float,
        colorMul: RGBA,
        texIndex: Int = currentTexIndex,
    ) {
        ensure(6, 4)
        addQuadIndices()
        addQuadVerticesFastNormal(x0, y0, x1, y1, x2, y2, x3, y3, tx0, ty0, tx1, ty1, colorMul.value, texIndex)
    }

    @JvmOverloads
    fun addQuadIndices(vc: Int = vertexCount) {
        indexPos += _addIndices(indicesI16, indexPos, vc + 0, vc + 1, vc + 2, vc + 3, vc + 0, vc + 2)
    }

    fun addQuadIndicesBatch(batchSize: Int) {
        var vc = vertexCount
        var ip = indexPos
        val i16 = indicesI16
        for (n in 0 until batchSize) {
            ip += _addIndices(i16, ip, vc + 0, vc + 1, vc + 2, vc + 3, vc + 0, vc + 2)
            vc += 4
        }
        indexPos = ip
        vertexCount = vc
    }

    fun addQuadVerticesFastNormal(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        tx0: Float, ty0: Float,
        tx1: Float, ty1: Float,
        colorMul: Int,
        texIndex: Int = currentTexIndex,
    ) {
        vertexPos = _addQuadVerticesFastNormal(vertexPos, vertices, x0, y0, x1, y1, x2, y2, x3, y3, tx0, ty0, tx1, ty1, colorMul, texIndex)
        vertexCount += 4
    }

    fun _addQuadVerticesFastNormal(
        vp: Int,
        vd: Buffer,
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        tx0: Float, ty0: Float,
        tx1: Float, ty1: Float,
        colorMul: Int,
        texIndex: Int = currentTexIndex,
    ): Int {
        var vp = vp
        vp += _addVertex(vd, vp, x0, y0, tx0, ty0, colorMul, texIndex)
        vp += _addVertex(vd, vp, x1, y1, tx1, ty0, colorMul, texIndex)
        vp += _addVertex(vd, vp, x2, y2, tx1, ty1, colorMul, texIndex)
        vp += _addVertex(vd, vp, x3, y3, tx0, ty1, colorMul, texIndex)
        return vp
    }

    fun _addQuadVerticesFastNormalNonRotated(
        vp: Int,
        vd: Buffer,
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        tx0: Float, ty0: Float,
        tx1: Float, ty1: Float,
        colorMul: Int,
        texIndex: Int = currentTexIndex,
    ): Int = _addQuadVerticesFastNormal(vp, vd, x0, y0, x1, y1, x1, y1, x0, y1, tx0, ty0, tx1, ty1, colorMul, texIndex)

    /**
     * Draws/buffers a set of textured and colorized array of vertices [array] with the current state previously set by calling [setStateFast].
     */
    fun drawVertices(
        array: TexturedVertexArray,
        matrix: MMatrix?,
        vcount: Int = array.vcount,
        icount: Int = array.icount,
        texIndex: Int = currentTexIndex,
    ) {
        ensure(icount, vcount)
        //println("texIndex=$texIndex : icount=$icount, vcount=$vcount")

        val i16: Uint16Buffer = indicesI16
        val ip = indexPos
        val vc = vertexCount
        val arrayIndices: ShortArray = array.indices
        val icount = min(icount, array.icount)

        arraycopy(arrayIndices.asUShortArrayInt(), 0, i16, ip, icount)
        arrayadd(i16, vc.toShort(), ip, ip + icount)
        //println("added: $vc"); for (n in 0 until icount) print(",${i16[n]}") ; println()
        //for (n in 0 until icount) i16[ip + n] = (vc + arrayIndices[n]).toShort()

        val vp = vertexPos
        val src = array._data.i32
        val dst = vertices.i32
        arraycopy(src, 0, dst, vp, vcount * 6)
        //for (n in 0 until vcount * 6) dst[vp + n] = src[n]

        //println("texIndex=$texIndex")
        val vp6 = vertexPos / 6
        //println("texIndex=$texIndex")
        arrayfill(verticesTexIndex, texIndex.toByte(), vp6, vp6 + vcount)

        if (matrix != null) {
            applyMatrix(matrix, vertexPos, vcount)
        }

        _vertexCount += vcount
        vertexPos += vcount * 6
        indexPos += icount
    }

    private fun applyMatrix(matrix: MMatrix, idx: Int, vcount: Int) {
        val f32 = vertices.f32
        var idx = idx

        val ma = matrix.af
        val mb = matrix.bf
        val mc = matrix.cf
        val md = matrix.df
        val mtx = matrix.txf
        val mty = matrix.tyf

        for (n in 0 until vcount) {
            val p = Point(f32[idx + 0], f32[idx + 1])
            val pt = Matrix.transform(ma, mb, mc, md, mtx, mty, p)
            f32[idx + 0] = pt.x
            f32[idx + 1] = pt.y
            idx += VERTEX_INDEX_SIZE
        }
    }

    /**
     * Draws/buffers a set of textured and colorized array of vertices [array] with the specified texture [tex] and optionally [smoothing] it and an optional [program].
     */
    inline fun drawVertices(
        array: TexturedVertexArray, tex: TextureBase, smoothing: Boolean, blendMode: BlendMode,
        vcount: Int = array.vcount, icount: Int = array.icount, program: Program? = null, matrix: MMatrix? = null,
    ) {
        setStateFast(tex.base, smoothing, blendMode, program, icount, vcount)
        drawVertices(array, matrix, vcount, icount)
	}

	private fun checkAvailable(indices: Int, vertices: Int): Boolean {
		return (this.indexPos + indices < maxIndices) && (this.vertexPos + vertices < maxVertices)
	}

	fun ensure(indices: Int, vertices: Int): Boolean {
        if (indices == 0 && vertices == 0) return false
        val doFlush = !checkAvailable(indices, vertices)
		if (doFlush) flushPartial()
		if (!checkAvailable(indices, vertices)) error("Too much vertices: indices=$indices, vertices=$vertices")
        return doFlush
	}

    /**
     * Sets the current texture [tex], [smoothing], [blendMode] and [program] that will be used by the following drawing calls not specifying these attributes.
     */
	fun setStateFast(
        tex: TextureBase, smoothing: Boolean, blendMode: BlendMode, program: Program?, icount: Int, vcount: Int,
    ) {
        setStateFast(tex.base, smoothing, blendMode, program, icount, vcount)
    }

    /**
     * Sets the current texture [tex], [smoothing], [blendMode] and [program] that will be used by the following drawing calls not specifying these attributes.
     */
    inline fun setStateFast(
        tex: AGTexture?, smoothing: Boolean, blendMode: BlendMode, program: Program?, icount: Int, vcount: Int,
    ) {
        ensure(icount, vcount)
        val pprogram = program ?: PROGRAM
        if (isCurrentStateFast(tex, smoothing, blendMode, pprogram)) return
        createBatchIfRequired()
        if (!currentTexN.contains(tex)) {
            currentTexIndex = 0
            currentTexN[0] = tex
        }
        currentSmoothing = smoothing
        currentBlendMode = blendMode
        currentProgram = pprogram
    }

    fun hasTex(tex: AGTexture?): Boolean {
        currentTexN.fastForEach { if (it === tex) return true }
        return false
    }

    //@PublishedApi internal fun resetCachedState() {
    //    for (n in currentTexN.indices) {
    //        currentTexN[n] = null
    //    }
    //    currentTexIndex = 0
    //    currentSmoothing = false
    //    currentBlendMode = BlendMode.NORMAL
    //    currentProgram = null
    //}

    @PublishedApi internal fun isCurrentStateFast(
        tex: AGTexture?, smoothing: Boolean, blendMode: BlendMode, program: Program,
    ): Boolean {
        var hasTex = hasTex(tex)
        if (currentTexN[0] !== null && !hasTex) {
            for (n in 1 until currentTexN.size) {
                if (currentTexN[n] === null) {
                    currentTexN[n] = tex
                    hasTex = true
                    break
                }
            }
        }

        for (n in currentTexN.indices) {
            if (tex === currentTexN[n]) {
                currentTexIndex = n
                break
            }
        }

        return hasTex
            && (currentSmoothing == smoothing)
            && (currentBlendMode === blendMode)
            && (currentProgram === program)
    }

    fun setStateFast(tex: Bitmap, smoothing: Boolean, blendMode: BlendMode, program: Program?, icount: Int, vcount: Int) {
        setStateFast(texManager.getTextureBase(tex), smoothing, blendMode, program, icount, vcount)
    }

    fun setStateFast(tex: BmpSlice, smoothing: Boolean, blendMode: BlendMode, program: Program?, icount: Int, vcount: Int) {
        setStateFast(texManager.getTexture(tex).base, smoothing, blendMode, program, icount, vcount)
    }

    fun drawQuad(
        tex: TextureCoords,
        x: Float = 0f,
        y: Float = 0f,
        width: Float = tex.width.toFloat(),
        height: Float = tex.height.toFloat(),
        m: MMatrix = identity,
        filtering: Boolean = true,
        colorMul: RGBA = Colors.WHITE,
        blendMode: BlendMode = BlendMode.NORMAL,
        program: Program? = null,
    ): Unit = drawQuad(tex, x, y, width, height, m, filtering, colorMul, blendMode, program, Unit)

    /**
     * Draws a textured [tex] quad at [x], [y] and size [width]x[height].
     *
     * It uses [m] transform matrix, an optional [filtering] and [colorMul], [blendMode] and [program] as state for drawing it.
     *
     * Note: To draw solid quads, you can use [Bitmaps.white] + [AgBitmapTextureManager] as texture and the [colorMul] as quad color.
     */
	fun drawQuad(
        tex: TextureCoords,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        m: MMatrix = identity,
        filtering: Boolean = true,
        colorMul: RGBA = Colors.WHITE,
        blendMode: BlendMode = BlendMode.NORMAL,
        program: Program? = null,
        unit: Unit = Unit,
	) {
        setStateFast(tex.base, filtering, blendMode, program, icount = 6, vcount = 4)
        drawQuadFast(x, y, width, height, m, tex, colorMul)
	}

    fun drawQuadFast(
        x: Float, y: Float, width: Float, height: Float,
        m: MMatrix,
        tex: BmpCoords,
        colorMul: RGBA,
    ) {
        val x0 = x
        val x1 = (x + width)
        val y0 = y
        val y1 = (y + height)
        drawQuadFast(
            m.transformXf(x0, y0), m.transformYf(x0, y0),
            m.transformXf(x1, y0), m.transformYf(x1, y0),
            m.transformXf(x1, y1), m.transformYf(x1, y1),
            m.transformXf(x0, y1), m.transformYf(x0, y1),
            tex, colorMul,
        )
    }

    companion object {
        val MAX_BATCH_QUADS = 16383
        //val DEFAULT_BATCH_QUADS = 0x1000
        val DEFAULT_BATCH_QUADS = 0x2000
        //val DEFAULT_BATCH_QUADS = 0x100
        //val DEFAULT_BATCH_QUADS = MAX_BATCH_QUADS

        init { logger.trace { "BatchBuilder2D.Companion[0]" } }

        @KorgeInternal
		val a_ColMul: Attribute get() = DefaultShaders.a_Col
        @KorgeInternal
		val a_ColAdd: Attribute = Attribute("a_Col2", VarType.Byte4, normalized = true, fixedLocation = 3)

		init { logger.trace { "BatchBuilder2D.Companion[1]" } }

        @KorgeInternal
		val v_ColMul: Varying get() = DefaultShaders.v_Col
        @KorgeInternal
		val v_ColAdd: Varying = Varying("v_Col2", VarType.Float4)

        val a_TexIndex: Attribute = Attribute("a_TexIndex", VarType.UByte1, normalized = false, precision = Precision.LOW, fixedLocation = 4)

        val v_TexIndex: Varying = Varying("v_TexIndex", VarType.Float1, precision = Precision.LOW)
        //val u_Tex0 = Uniform("u_Tex0", VarType.TextureUnit)

        val u_TexN: Array<Uniform> = Array(BB_MAX_TEXTURES) { Uniform("u_Tex$it", VarType.Sampler2D) }

        //val u_Tex0 = DefaultShaders.u_Tex
        //val u_Tex1 = Uniform("u_Tex1", VarType.TextureUnit)

		init { logger.trace { "BatchBuilder2D.Companion[2]" } }

        @KorgeInternal
		val LAYOUT = VertexLayout(DefaultShaders.a_Pos, DefaultShaders.a_Tex, a_ColMul, a_ColAdd)
        @KorgeInternal
        val LAYOUT_TEX_INDEX = VertexLayout(a_TexIndex)

		init { logger.trace { "BatchBuilder2D.Companion[3]" } }

        @KorgeInternal
		val PROGRAM: Program = Program(
            vertex = VertexShaderDefault {
                SET(v_Tex, a_Tex)
                SET(v_TexIndex, a_TexIndex)
                SET(v_ColMul, vec4(a_ColMul["rgb"] * a_ColMul["a"], a_ColMul["a"]))
                SET(v_ColAdd, a_ColAdd)
                SET(out, (u_ProjMat * u_ViewMat) * vec4(a_Pos, 0f.lit, 1f.lit))
            },
            fragment = FragmentShaderDefault {
                createTextureLookup(this)
                SET(out, out * v_ColMul)
                IF(out["a"] le 0f.lit) { DISCARD() }
            },
            name = "BatchBuilder2D.Tinted"
        )

        fun createTextureLookup(builder: ProgramBuilderDefault) {
            builder.apply {
                IF_ELSE_BINARY_LOOKUP(v_TexIndex, 0, BB_MAX_TEXTURES - 1) { n ->
                    SET(out, texture2D(u_TexN[n], v_Tex["xy"]))
                }
            }
        }

        init { logger.trace { "BatchBuilder2D.Companion[4]" } }
	}

    val beforeFlush = Signal<BatchBuilder2D>()
    val onInstanceCount = Signal<Int>()

    fun uploadIndices() {
        buffers.current.indexBuffer.upload(indices, 0, indexPos * 2)
    }

    fun updateStandardUniforms() {
        //println("updateStandardUniforms: ag.currentSize(${ag.currentWidth}, ${ag.currentHeight}) : ${ag.currentRenderBuffer}")
        ctx.updateStandardUniforms()
        for (n in 0 until maxTextures) {
            uniforms[u_TexN[n]].set(currentTexN[n], AGTextureUnitInfo(linear = currentSmoothing))
        }
        //uniforms[u_InputPre] = currentTexN[0]?.premultiplied == true
        //uniforms[u_OutputPre] = ctx.isRenderingToTexture
    }

    private val batches = fastArrayListOf<AGBatch>()

    private var lastIndexPos = 0
    var batchCount = 0
    var fullBatchCount = 0

    fun createBatchIfRequired() {
        if (lastIndexPos == indexPos) return
        updateStandardUniforms()
        batchCount++

        //println("BATCH: currentBuffers.vertexData=${currentBuffers.vertexData}")
        batches += AGBatch(
            ctx.currentFrameBuffer.base,
            ctx.currentFrameBuffer.info,
            vertexData = buffers.current.vertexData,
            indices = buffers.current.indexBuffer,
            program = currentProgram,
            //program = PROGRAM_PRE,
            drawType = AGDrawType.TRIANGLES,
            blending = currentBlendMode.factors,
            uniforms = uniforms.cloneReadOnly(),
            newUniformBlocks = ctx.createCurrentNewUniformsRef(currentProgram, autoUpload = false),
            stencilOpFunc = stencilOpFunc,
            stencilRef = stencilRef,
            colorMask = colorMask,
            scissor = _scissor,
            drawOffset = lastIndexPos * 2,
            vertexCount = (indexPos - lastIndexPos),
        )
        lastIndexPos = indexPos
    }

    fun flush(uploadVertices: Boolean = true, uploadIndices: Boolean = true) {
        flushPartial(uploadVertices, uploadIndices)
        for (n in 0 until maxTextures) currentTexN[n] = null
        currentTexIndex = 0
    }

    /** When there are vertices pending, this performs a [AG.draw] call flushing all the buffered geometry pending to draw */
	fun flushPartial(uploadVertices: Boolean = true, uploadIndices: Boolean = true) {
        createBatchIfRequired()

        //println("vertexCount=${vertexCount}")
		if (batches.isNotEmpty()) {
			//println("ORTHO: ${ag.backHeight.toFloat()}, ${ag.backWidth.toFloat()}")
			if (uploadVertices) {
                buffers.current.vertexBuffer.upload(vertices, 0, vertexPos * 4)
                buffers.current.texIndexVertexBuffer.upload(verticesTexIndex, 0, vertexPos / 6)
            }
            if (uploadIndices) uploadIndices()

			//println("MyUniforms: $uniforms")

			//println("RENDER: $realFactors")
            //println("DRAW: $uniforms")

            //println("program=$program, currentTexN[0]=${currentTexN[0]}")
            ctx.uploadUpdatedUniforms()
            ag.draw(AGMultiBatch(batches.toList()))
            batches.clear()
            beforeFlush(this)
            fullBatchCount++
            ctx.afterFullBatch()

            buffers.next()

            //println("indexPos=$indexPos, vertexCount=$vertexCount")
		}

		vertexCount = 0
		vertexPos = 0
        lastIndexPos = 0
		indexPos = 0

        //currentProgram = null
        //resetCachedState()
	}

    fun simulateBatchStats(vertexCount: Int) {
        val oldVertexCount = this.vertexCount
        this.vertexCount = vertexCount
        try {
            beforeFlush(this)
        } finally {
            this.vertexCount = oldVertexCount
        }
    }

    /**
     * Executes [callback] while setting temporarily the view matrix to [matrix]
     */
	inline fun setViewMatrixTemp(matrix: MMatrix, crossinline callback: () -> Unit) = ctx.setViewMatrixTemp(matrix, callback)

    /**
     * Executes [callback] while restoring [uniform] to its current value after [callback] is exexcuted.
     */
    @Deprecated("")
    inline fun keepUniform(uniform: Uniform, flush: Boolean = true, callback: (AGUniformValues) -> Unit) {
        ctx.keepUniform(uniform, flush, callback)
    }

    inline fun flush(block: () -> Unit) {
        ctx.flush()
        try {
            block()
        } finally {
            ctx.flush()
        }
    }

    /**
     * Executes [callback] while restoring [uniforms] to its current value after [callback] is exexcuted.
     */
    inline fun keepUniforms(uniforms: Array<Uniform>, flush: Boolean = true, callback: (AGUniformValues) -> Unit) {
        ctx.keepUniforms(uniforms, flush, callback)
    }

    /**
     * Executes [callback] while setting temporarily a set of [uniforms]
     */
	inline fun setTemporalUniforms(uniforms: AGUniformValues?, callback: (AGUniformValues) -> Unit) = ctx.setTemporalUniforms(uniforms, callback)
}

internal val BB_MAX_TEXTURES = when {
    Platform.isLinux && Platform.arch.isArm && Platform.arch.is32Bits -> 1
    //"iosArm32" -> 1
    else -> 4
}
