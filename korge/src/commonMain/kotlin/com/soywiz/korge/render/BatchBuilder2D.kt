@file:UseExperimental(KorgeInternal::class)

package com.soywiz.korge.render

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

private val logger = Logger("BatchBuilder2D")

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
class BatchBuilder2D constructor(
    @KorgeInternal
    val ctx: RenderContext,
    /** Maximum number of quads that could be drawn in a single batch.
     * Bigger numbers will increase memory usage, but might reduce the number of batches per frame when using the same texture and properties.
     */
    val reqMaxQuads: Int = DEFAULT_BATCH_QUADS
) {
    val maxQuads: Int = min2(reqMaxQuads, MAX_BATCH_QUADS)

    val texManager = ctx.agBitmapTextureManager
    constructor(ag: AG, maxQuads: Int = DEFAULT_BATCH_QUADS) : this(RenderContext(ag), maxQuads)
    val ag: AG = ctx.ag
	init {
        logger.trace { "BatchBuilder2D[0]" }
        ctx.flushers.add { flush() }
    }

	var flipRenderTexture = true
	//var flipRenderTexture = false
	val maxQuadsMargin = maxQuads + 9

    /** Maximum number of vertices that can be buffered here in a single batch. It depens on the [maxQuads] parameter */
	val maxVertices = maxQuads * 4
    /** Maximum number of indices that can be buffered here in a single batch. It depens on the [maxQuads] parameter */
	val maxIndices = maxQuads * 6

	init { logger.trace { "BatchBuilder2D[1]" } }

	internal val vertices = FBuffer.alloc(6 * 4 * maxVertices)
    internal val indices = FBuffer.alloc(2 * maxIndices)
    //internal val vertices = FBuffer.allocNoDirect(6 * 4 * maxVertices)
    //internal val indices = FBuffer.allocNoDirect(2 * maxIndices)
    private val indicesI16 = indices.i16
    private val verticesI32 = vertices.i32
    private val verticesF32 = vertices.f32
    private val verticesData = vertices.data
    internal val verticesFast32 = vertices.fast32

	init { logger.trace { "BatchBuilder2D[2]" } }

	var vertexCount = 0
        internal set
    internal var vertexPos = 0
	internal var indexPos = 0
	private var currentTex: AG.Texture? = null
	private var currentSmoothing: Boolean = false
	private var currentBlendFactors: AG.Blending = BlendMode.NORMAL.factors
	private var currentProgram: Program? = null

	init { logger.trace { "BatchBuilder2D[3]" } }

	private val vertexBuffer = ag.createVertexBuffer()
	private val indexBuffer = ag.createIndexBuffer()

	init { logger.trace { "BatchBuilder2D[4]" } }

    /** The current stencil state. If you change it, you must call the [flush] method to ensure everything has been drawn. */
	var stencil = AG.StencilState()

	init { logger.trace { "BatchBuilder2D[5]" } }

    /** The current color mask state. If you change it, you must call the [flush] method to ensure everything has been drawn. */
	var colorMask = AG.ColorMaskState()

	init { logger.trace { "BatchBuilder2D[6]" } }

    /** The current scissor state. If you change it, you must call the [flush] method to ensure everything has been drawn. */
	var scissor: AG.Scissor? = null

	private val identity = Matrix()

	init { logger.trace { "BatchBuilder2D[7]" } }

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

	init { logger.trace { "BatchBuilder2D[8]" } }

	private val projMat = Matrix3D()

    @KorgeInternal
	val viewMat = Matrix3D()

	init { logger.trace { "BatchBuilder2D[9]" } }

	private val textureUnit = AG.TextureUnit(null, linear = false)

	init { logger.trace { "BatchBuilder2D[10]" } }

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

	init { logger.trace { "BatchBuilder2D[11]" } }

    fun readVertices(): List<VertexInfo> = (0 until vertexCount).map { readVertex(it) }

    fun readVertex(n: Int, out: VertexInfo = VertexInfo()): VertexInfo {
        out.read(this.vertices, n)
        val source = textureUnit.texture?.source
        out.texWidth = source?.width ?: -1
        out.texHeight = source?.height ?: -1
        return out
    }

	// @TODO: copy data from TexturedVertexArray
	fun addVertex(x: Float, y: Float, u: Float, v: Float, colorMul: RGBA, colorAdd: ColorAdd) {
        _addVertex(x, y, u, v, colorMul.value, colorAdd.value)
	}

    fun _addVertex(x: Float, y: Float, u: Float, v: Float, colorMul: Int, colorAdd: Int) {
        vertexPos += _addVertex(verticesFast32, vertexPos, x, y, u, v, colorMul, colorAdd)
        vertexCount++
    }

    fun _addVertex(vd: Fast32Buffer, vp: Int, x: Float, y: Float, u: Float, v: Float, colorMul: Int, colorAdd: Int): Int {
        vd.setF(vp + 0, x)
        vd.setF(vp + 1, y)
        vd.setF(vp + 2, u)
        vd.setF(vp + 3, v)
        vd.setI(vp + 4, colorMul)
        vd.setI(vp + 5, colorAdd)
        return TexturedVertexArray.COMPONENTS_PER_VERTEX
    }

    fun _addVertex(f32: Float32Buffer, i32: Int32Buffer, vp: Int, x: Float, y: Float, u: Float, v: Float, colorMul: Int, colorAdd: Int): Int {
        f32[vp + 0] = x
        f32[vp + 1] = y
        f32[vp + 2] = u
        f32[vp + 3] = v
        i32[vp + 4] = colorMul
        i32[vp + 5] = colorAdd
        return TexturedVertexArray.COMPONENTS_PER_VERTEX
    }

	fun addIndex(idx: Int) {
		indicesI16[indexPos++] = idx.toShort()
	}

    fun addIndexRelative(idx: Int) {
        indicesI16[indexPos++] = (vertexCount + idx).toShort()
    }

	fun addIndices(i0: Int, i1: Int, i2: Int, i3: Int, i4: Int, i5: Int) {
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
		colorMul: RGBA, colorAdd: ColorAdd,
        /** Not working right now */
		rotated: Boolean = false
	) {
        drawQuadFast(x0, y0, x1, y1, x2, y2, x3, y3, tex.x0, tex.y0, tex.x1, tex.y1, colorMul, colorAdd, rotated)
	}

    fun drawQuadFast(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        tx0: Float, ty0: Float,
        tx1: Float, ty1: Float,
        colorMul: RGBA,
        colorAdd: ColorAdd,
        rotated: Boolean
    ) {
        ensure(6, 4)

        addQuadIndices()

        if (rotated) {
            // @TODO:
            addQuadVerticesFastRotated(x0, y0, x1, y1, x2, y2, x3, y3, tx0, ty0, tx1, ty1, colorMul, colorAdd)
        } else {
            addQuadVerticesFastNormal(x0, y0, x1, y1, x2, y2, x3, y3, tx0, ty0, tx1, ty1, colorMul.value, colorAdd.value)
        }
    }

    fun drawQuadFast(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        tx0: Float, ty0: Float,
        tx1: Float, ty1: Float,
        colorMul: RGBA,
        colorAdd: ColorAdd
    ) {
        ensure(6, 4)
        addQuadIndices()
        addQuadVerticesFastNormal(x0, y0, x1, y1, x2, y2, x3, y3, tx0, ty0, tx1, ty1, colorMul.value, colorAdd.value)
    }

    fun addQuadIndices() {
        val vc = vertexCount
        addIndices(vc + 0, vc + 1, vc + 2, vc + 3, vc + 0, vc + 2)
    }

    fun addQuadVerticesFastNormal(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        tx0: Float, ty0: Float,
        tx1: Float, ty1: Float,
        colorMul: Int,
        colorAdd: Int,
    ) {
        vertexPos = _addQuadVerticesFastNormal(vertexPos, verticesFast32, x0, y0, x1, y1, x2, y2, x3, y3, tx0, ty0, tx1, ty1, colorMul, colorAdd)
        vertexCount += 4
    }

    fun _addQuadVerticesFastNormal(
        vp: Int,
        vd: Fast32Buffer,
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        tx0: Float, ty0: Float,
        tx1: Float, ty1: Float,
        colorMul: Int,
        colorAdd: Int,
    ): Int {
        var vp = vp
        vp += _addVertex(vd, vp, x0, y0, tx0, ty0, colorMul, colorAdd)
        vp += _addVertex(vd, vp, x1, y1, tx1, ty0, colorMul, colorAdd)
        vp += _addVertex(vd, vp, x2, y2, tx1, ty1, colorMul, colorAdd)
        vp += _addVertex(vd, vp, x3, y3, tx0, ty1, colorMul, colorAdd)
        return vp
    }

    fun _addQuadVerticesFastNormalNonRotated(
        vp: Int,
        vd: Fast32Buffer,
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        tx0: Float, ty0: Float,
        tx1: Float, ty1: Float,
        colorMul: Int,
        colorAdd: Int,
    ): Int {
        var vp = vp
        vp += _addVertex(vd, vp, x0, y0, tx0, ty0, colorMul, colorAdd)
        vp += _addVertex(vd, vp, x1, y0, tx1, ty0, colorMul, colorAdd)
        vp += _addVertex(vd, vp, x1, y1, tx1, ty1, colorMul, colorAdd)
        vp += _addVertex(vd, vp, x0, y1, tx0, ty1, colorMul, colorAdd)
        return vp
    }

    fun addQuadVerticesFastRotated(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        tx0: Float, ty0: Float,
        tx1: Float, ty1: Float,
        colorMul: RGBA,
        colorAdd: ColorAdd,
    ) {
        // @TODO:
        addVertex(x0, y0, tx0, ty0, colorMul, colorAdd)
        addVertex(x1, y1, tx1, ty0, colorMul, colorAdd)
        addVertex(x2, y2, tx1, ty1, colorMul, colorAdd)
        addVertex(x3, y3, tx0, ty1, colorMul, colorAdd)
    }

    /**
     * Draws/buffers a set of textured and colorized array of vertices [array] with the current state previously set by calling [setStateFast].
     */
	fun drawVertices(array: TexturedVertexArray, vcount: Int = array.vcount, icount: Int = array.isize) {
		ensure(icount, vcount)

		for (idx in 0 until min2(icount, array.isize)) addIndex(vertexCount + array.indices[idx])
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

	fun ensure(indices: Int, vertices: Int) {
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
        if (tex === currentTex && currentSmoothing === smoothing && currentBlendFactors === blendFactors && currentProgram === program) return
        flush()
        currentTex = tex
        currentSmoothing = smoothing
        currentBlendFactors = if (tex != null && tex.isFbo) blendFactors.toRenderFboIntoBack() else blendFactors
        currentProgram = program
    }

    fun setStateFast(tex: Bitmap, smoothing: Boolean, blendFactors: AG.Blending, program: Program?) {
        setStateFast(texManager.getTextureBase(tex), smoothing, blendFactors, program)
    }

    fun setStateFast(tex: BmpSlice, smoothing: Boolean, blendFactors: AG.Blending, program: Program?) {
        setStateFast(texManager.getTexture(tex).base, smoothing, blendFactors, program)
    }

    /**
     * Draws/buffers a 9-patch image with the texture [tex] at [x], [y] with the total size of [width] and [height].
     * [posCuts] and [texCuts] are [Point] an array of 4 points describing ratios (values between 0 and 1) inside the width/height of the area to be drawn,
     * and the positions inside the texture.
     *
     * The 9-patch looks like this (dividing the image in 9 parts).
     *
     * 0--+-----+--+
     * |  |     |  |
     * |--1-----|--|
     * |  |SSSSS|  |
     * |  |SSSSS|  |
     * |  |SSSSS|  |
     * |--|-----2--|
     * |  |     |  |
     * +--+-----+--3
     *
     * 0: Top-left of the 9-patch
     * 1: Top-left part where scales starts
     * 2: Bottom-right part where scales ends
     * 3: Bottom-right of the 9-patch
     *
     * S: Is the part that is scaled. The other regions are not scaled.
     *
     * It uses the transform [m] matrix, with an optional [filtering] and [colorMul]/[colorAdd], [blendFactors] and [program]
     */
	fun drawNinePatch(
		tex: Texture,
		x: Float = 0f,
		y: Float = 0f,
		width: Float = tex.width.toFloat(),
		height: Float = tex.height.toFloat(),
		posCuts: Array<Point>,
		texCuts: Array<Point>,
		m: Matrix = identity,
		filtering: Boolean = true,
		colorMul: RGBA = Colors.WHITE,
		colorAdd: ColorAdd = ColorAdd.NEUTRAL,
		blendFactors: AG.Blending = BlendMode.NORMAL.factors,
		program: Program? = null
	) {
		setStateFast(tex.base, filtering, blendFactors, program)

		ensure(indices = 6 * 9, vertices = 4 * 4)

		val p_o = pt1.setToTransform(m, ptt1.setTo(x, y))
		val p_dU = pt2.setToSub(ptt1.setToTransform(m, ptt1.setTo(x + width, y)), p_o)
		val p_dV = pt3.setToSub(ptt1.setToTransform(m, ptt1.setTo(x, y + height)), p_o)

		val t_o = pt4.setTo(tex.x0, tex.y0)
		val t_dU = pt5.setToSub(ptt1.setTo(tex.x1, tex.y0), t_o)
		val t_dV = pt6.setToSub(ptt1.setTo(tex.x0, tex.y1), t_o)

		val start = vertexCount

		for (cy in 0 until 4) {
			val posCutY = posCuts[cy].y
			val texCutY = texCuts[cy].y
			for (cx in 0 until 4) {
				val posCutX = posCuts[cx].x
				val texCutX = texCuts[cx].x

				val p = pt7.setToAdd(
					p_o,
					ptt1.setToAdd(
						ptt1.setToMul(p_dU, posCutX),
						ptt2.setToMul(p_dV, posCutY)
					)
				)

				val t = pt8.setToAdd(
					t_o,
					ptt1.setToAdd(
						ptt1.setToMul(t_dU, texCutX),
						ptt2.setToMul(t_dV, texCutY)
					)
				)

				addVertex(p.x.toFloat(), p.y.toFloat(), t.x.toFloat(), t.y.toFloat(), colorMul, colorAdd)
			}
		}

		for (cy in 0 until 3) {
			for (cx in 0 until 3) {
				// v0...v1
				// .    .
				// v2...v3

				val v0 = start + cy * 4 + cx
				val v1 = v0 + 1
				val v2 = v0 + 4
				val v3 = v0 + 5

				addIndex(v0)
				addIndex(v1)
				addIndex(v2)
				addIndex(v2)
				addIndex(v1)
				addIndex(v3)
			}
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
		colorAdd: ColorAdd = ColorAdd.NEUTRAL,
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
            m.transformXf(x0, y0), m.transformYf(x0, y0),
            m.transformXf(x1, y0), m.transformYf(x1, y0),
            m.transformXf(x1, y1), m.transformYf(x1, y1),
            m.transformXf(x0, y1), m.transformYf(x0, y1),
			tex, colorMul, colorAdd, rotated
		)
	}

	companion object {
        val DEFAULT_BATCH_QUADS = 4096
        val MAX_BATCH_QUADS = 16383

        init { logger.trace { "BatchBuilder2D.Companion[0]" } }

        @KorgeInternal
		val a_ColMul = DefaultShaders.a_Col
        @KorgeInternal
		val a_ColAdd = Attribute("a_Col2", VarType.Byte4, normalized = true)

		init { logger.trace { "BatchBuilder2D.Companion[1]" } }

        @KorgeInternal
		val v_ColMul = DefaultShaders.v_Col
        @KorgeInternal
		val v_ColAdd = Varying("v_Col2", VarType.Byte4)

		init { logger.trace { "BatchBuilder2D.Companion[2]" } }

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

		init { logger.trace { "BatchBuilder2D.Companion[3]" } }

        @KorgeInternal
        val FRAGMENT_PRE = buildTextureLookupFragment(premultiplied = true)

        @KorgeInternal
        val FRAGMENT_NOPRE = buildTextureLookupFragment(premultiplied = false)

        @KorgeInternal
		val PROGRAM_PRE = Program(
			vertex = VERTEX,
			fragment = FRAGMENT_PRE,
			name = "BatchBuilder2D.Premultiplied.Tinted"
		)

        @KorgeInternal
		val PROGRAM_NOPRE = Program(
			vertex = VERTEX,
			fragment = FRAGMENT_NOPRE,
			name = "BatchBuilder2D.NoPremultiplied.Tinted"
		)

		init { logger.trace { "BatchBuilder2D.Companion[4]" } }

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
    val beforeFlush = Signal<BatchBuilder2D>()

    fun uploadVertices() {
        vertexBuffer.upload(vertices, 0, vertexPos * 4)
    }

    fun uploadIndices() {
        indexBuffer.upload(indices, 0, indexPos * 2)
    }

    /** When there are vertices pending, this performs a [AG.draw] call flushing all the buffered geometry pending to draw */
	fun flush(uploadVertices: Boolean = true, uploadIndices: Boolean = true) {
		if (vertexCount > 0) {
			if (flipRenderTexture && ag.renderingToTexture) {
				projMat.setToOrtho(tempRect.setBounds(0, ag.currentHeight, ag.currentWidth, 0), -1f, 1f)
			} else {
				projMat.setToOrtho(tempRect.setBounds(0, 0, ag.currentWidth, ag.currentHeight), -1f, 1f)
			}

			//println("ORTHO: ${ag.backHeight.toFloat()}, ${ag.backWidth.toFloat()}")

			val factors = currentBlendFactors

			if (uploadVertices) uploadVertices()
            if (uploadIndices) uploadIndices()

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
	inline fun setViewMatrixTemp(matrix: Matrix, crossinline callback: () -> Unit) {
        ctx.matrix3DPool.alloc { temp ->
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

// @TODO: Call this mesh?
/**
 * Allows to build a set of textured and colored vertices. Where [vcount] is the number of vertices and [isize] [indices],
 * the maximum number of indices.
 * 
 * [vcount] and [isize] could be decreased later, but not increased since the buffer is created at the beginning.
 */
class TexturedVertexArray(var vcount: Int, val indices: IntArray, var isize: Int = indices.size) {
    /** The initial/maximum number of vertices */
	val initialVcount = vcount
	//internal val data = IntArray(COMPONENTS_PER_VERTEX * vcount)
	//internal val _data = FBuffer(COMPONENTS_PER_VERTEX * initialVcount * 4, direct = false)
    internal val _data = FBuffer.allocNoDirect(COMPONENTS_PER_VERTEX * initialVcount * 4)
    private val fast = _data.fast32
	//private val f32 = _data.f32
    //private val i32 = _data.i32
	//val points = (0 until vcount).map { Item(data, it) }
	//val icount = indices.size

	companion object {
        @KorgeInternal
		const val COMPONENTS_PER_VERTEX = 6

        @KorgeInternal
		val QUAD_INDICES = intArrayOf(0, 1, 2,  3, 0, 2)

        val EMPTY_INT_ARRAY = IntArray(0)

        /** Builds indices for drawing triangles when the vertices information is stored as quads (4 vertices per quad primitive) */
		fun quadIndices(quadCount: Int): IntArray {
            if (quadCount == 0) return EMPTY_INT_ARRAY
			val out = IntArray(quadCount * 6)
			var m = 0
			var base = 0
			for (n in 0 until quadCount) {
				out[m++] = base + 0
				out[m++] = base + 1
				out[m++] = base + 2
				out[m++] = base + 3
				out[m++] = base + 0
				out[m++] = base + 2
				base += 4
			}
			//QUAD_INDICES.repeat(quadCount)
			return out
		}
	}

	private var offset = 0
    
    /** Moves the cursor for setting vertexs to the vertex [i] */
    fun select(i: Int): TexturedVertexArray {
        offset = i * COMPONENTS_PER_VERTEX
        return this
    }
    /** Sets the [x] of the vertex previously selected calling [select] */
	fun setX(v: Float): TexturedVertexArray {
        fast.setF(offset + 0, v)
        return this
    }
    /** Sets the [y] of the vertex previously selected calling [select] */
	fun setY(v: Float): TexturedVertexArray {
        fast.setF(offset + 1, v)
        return this
    }
    /** Sets the [u] (x in texture) of the vertex previously selected calling [select] */
	fun setU(v: Float): TexturedVertexArray {
        fast.setF(offset + 2, v)
        return this
    }
    /** Sets the [v] (y in texture) of the vertex previously selected calling [select] */
	fun setV(v: Float): TexturedVertexArray {
        fast.setF(offset + 3, v)
        return this
    }
    /** Sets the [cMul] (multiplicative color) of the vertex previously selected calling [select] */
	fun setCMul(v: RGBA): TexturedVertexArray {
        fast.setI(offset + 4, v.value)
        return this
    }
    /** Sets the [cAdd] (additive color) of the vertex previously selected calling [select] */
	fun setCAdd(v: ColorAdd): TexturedVertexArray {
        fast.setI(offset + 5, v.value)
        return this
    }
    /** Sets the [x] and [y] with the [matrix] transform applied of the vertex previously selected calling [select] */
	fun xy(x: Double, y: Double, matrix: Matrix) = setX(matrix.transformXf(x, y)).setY(matrix.transformYf(x, y))
    /** Sets the [x] and [y] of the vertex previously selected calling [select] */
	fun xy(x: Double, y: Double) = setX(x.toFloat()).setY(y.toFloat())
    /** Sets the [u] and [v] of the vertex previously selected calling [select] */
	fun uv(tx: Float, ty: Float) = setU(tx).setV(ty)
    /** Sets the [cMul] and [cAdd] (multiplicative and additive colors) of the vertex previously selected calling [select] */
	fun cols(colMul: RGBA, colAdd: ColorAdd) = setCMul(colMul).setCAdd(colAdd)

    fun quadV(index: Int, x: Float, y: Float, u: Float, v: Float, colMul: RGBA, colAdd: ColorAdd) {
        quadV(fast, index * COMPONENTS_PER_VERTEX, x, y, u, v, colMul.value, colAdd.value)
    }

    fun quadV(fast: Fast32Buffer, pos: Int, x: Float, y: Float, u: Float, v: Float, colMul: Int, colAdd: Int): Int {
        fast.setF(pos + 0, x)
        fast.setF(pos + 1, y)
        fast.setF(pos + 2, u)
        fast.setF(pos + 3, v)
        fast.setI(pos + 4, colMul)
        fast.setI(pos + 5, colAdd)
        return COMPONENTS_PER_VERTEX
    }

    fun quadV(index: Int, x: Double, y: Double, u: Float, v: Float, colMul: RGBA, colAdd: ColorAdd) = quadV(index, x.toFloat(), y.toFloat(), u, v, colMul, colAdd)

    /**
     * Sets a textured quad at vertice [index] with the region defined by [x],[y] [width]x[height] and the [matrix],
     * using the texture coords defined by [BmpSlice] and color transforms [colMul] and [colAdd]
     */
    @OptIn(KorgeInternal::class)
	fun quad(index: Int, x: Double, y: Double, width: Double, height: Double, matrix: Matrix, bmp: BmpSlice, colMul: RGBA, colAdd: ColorAdd) {
        quad(index, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), matrix, bmp, colMul, colAdd)
	}

    @OptIn(KorgeInternal::class)
    fun quad(index: Int, x: Float, y: Float, width: Float, height: Float, matrix: Matrix, bmp: BmpSlice, colMul: RGBA, colAdd: ColorAdd) {
        val xw = x + width
        val yh = y + height

        /*
        val x0 = matrix.transformXf(x, y)
        val x1 = matrix.transformXf(xw, y)
        val x2 = matrix.transformXf(xw, yh)
        val x3 = matrix.transformXf(x, yh)

        val y0 = matrix.transformYf(x, y)
        val y1 = matrix.transformYf(xw, y)
        val y2 = matrix.transformYf(xw, yh)
        val y3 = matrix.transformYf(x, yh)
        */

        val af = matrix.af
        val cf = matrix.cf
        val txf = matrix.txf
        val x0 = af * x + cf * y + txf
        val x1 = af * xw + cf * y + txf
        val x2 = af * xw + cf * yh + txf
        val x3 = af * x + cf * yh + txf

        val df = matrix.df
        val bf = matrix.bf
        val tyf = matrix.tyf
        val y0 = df * y + bf * x + tyf
        val y1 = df * y + bf * xw + tyf
        val y2 = df * yh + bf * xw + tyf
        val y3 = df * yh + bf * x + tyf

        val fast = this.fast
        var pos = index * COMPONENTS_PER_VERTEX
        val cm = colMul.value
        val ca = colAdd.value
        pos += quadV(fast, pos, x0, y0, bmp.tl_x, bmp.tl_y, cm, ca)
        pos += quadV(fast, pos, x1, y1, bmp.tr_x, bmp.tr_y, cm, ca)
        pos += quadV(fast, pos, x2, y2, bmp.br_x, bmp.br_y, cm, ca)
        pos += quadV(fast, pos, x3, y3, bmp.bl_x, bmp.bl_y, cm, ca)
    }

	private val bounds: BoundsBuilder = BoundsBuilder()

    /**
     * Returns the bounds of the vertices defined in the indices from [min] to [max] (excluding) as [Rectangle]
     * Allows to define the output as [out] to be allocation-free, setting the [out] [Rectangle] and returning it.
     */
    fun getBounds(min: Int = 0, max: Int = vcount, out: Rectangle = Rectangle()): Rectangle {
		bounds.reset()
		for (n in min until max) {
			select(n)
			bounds.add(x.toDouble(), y.toDouble())
		}
		return bounds.getBounds(out)
	}

    /** [x] at the previously vertex selected by calling [select] */
	val x: Float get() = fast.getF(offset + 0)
    /** [y] at the previously vertex selected by calling [select] */
	val y: Float get() = fast.getF(offset + 1)
    /** [u] (x in texture) at the previously vertex selected by calling [select] */
	val u: Float get() = fast.getF(offset + 2)
    /** [v] (y in texture) at the previously vertex selected by calling [select] */
	val v: Float get() = fast.getF(offset + 3)
    /** [cMul] (multiplicative color) at the previously vertex selected by calling [select] */
	val cMul: Int get() = fast.getI(offset + 4)
    /** [cAdd] (additive color) at the previously vertex selected by calling [select] */
	val cAdd: Int get() = fast.getI(offset + 5)

    /** Describes the vertice previously selected by calling [select] */
	val vertexString: String get() = "V(xy=($x, $y),uv=$u, $v,cMul=$cMul,cAdd=$cAdd)"

    /** Describes a vertex at [index] */
	fun str(index: Int): String {
		val old = this.offset
		try {
			return select(index).vertexString
		} finally {
			this.offset = old
		}
	}

	//class Item(private val data: IntArray, index: Int) {
	//	val offset = index * COMPONENTS_PER_VERTEX
	//	var x: Float; get() = Float.fromBits(data[offset + 0]); set(v) { data[offset + 0] = v.toBits() }
	//	var y: Float; get() = Float.fromBits(data[offset + 1]); set(v) { data[offset + 1] = v.toBits() }
	//	var tx: Float; get() = Float.fromBits(data[offset + 2]); set(v) { data[offset + 2] = v.toBits() }
	//	var ty: Float; get() = Float.fromBits(data[offset + 3]); set(v) { data[offset + 3] = v.toBits() }
	//	var colMul: Int; get() = data[offset + 4]; set(v) { data[offset + 4] = v }
	//	var colAdd: Int; get() = data[offset + 5]; set(v) { data[offset + 5] = v }
	//	fun setXY(x: Double, y: Double, matrix: Matrix) {
	//		this.x = matrix.transformX(x, y).toFloat()
	//		this.y = matrix.transformY(x, y).toFloat()
	//	}
	//	fun setXY(x: Double, y: Double) { this.x = x.toFloat() }.also { this.y = y.toFloat() }
	//	fun setTXY(tx: Float, ty: Float) { this.tx = tx }.also { this.ty = ty }
	//	fun setCols(colMul: Int, colAdd: Int) { this.colMul = colMul }.also { this.colAdd = colAdd }
	//}
}

/*
// @TODO: Move to the right place
private fun IntArray.repeat(count: Int): IntArray {
	val out = IntArray(this.size * count)

	for (n in 0 until out.size) out[n] = this[n % this.size]

	//if (count > 0) {
	//	arraycopy(this, 0, out, 0, this.size)
	//	if (count > 1) {
	//		// This should work because of overlapping!
	//		arraycopy(out, 0, out, this.size, (count - 1) * this.size)
	//	}
	//}

	//for (n in 0 until count) arraycopy(this, 0, out, n * this.size, this.size)
	return out
}
*/
