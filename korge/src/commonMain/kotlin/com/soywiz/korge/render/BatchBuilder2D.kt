package com.soywiz.korge.render

import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.html.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*
import kotlin.math.*

private val logger = Logger("BatchBuilder2D")

class BatchBuilder2D(val ag: AG, val maxQuads: Int = 1000) {
	init { logger.trace { "BatchBuilder2D[0]" } }

	var flipRenderTexture = true
	//var flipRenderTexture = false
	val maxQuadsMargin = maxQuads + 9
	val maxVertices = maxQuads * 4
	val maxIndices = maxQuads * 6

	init { logger.trace { "BatchBuilder2D[1]" } }

	private val vertices = FBuffer.alloc(6 * 4 * maxVertices)
	private val indices = FBuffer.alloc(2 * maxIndices)

	init { logger.trace { "BatchBuilder2D[2]" } }

	private var vertexCount = 0
	private var vertexPos = 0
	private var indexPos = 0
	private var currentTex: AG.Texture? = null
	private var currentSmoothing: Boolean = false
	private var currentBlendFactors: AG.Blending = BlendMode.NORMAL.factors
	private var currentProgram: Program? = null

	init { logger.trace { "BatchBuilder2D[3]" } }

	private val vertexBuffer = ag.createVertexBuffer()
	private val indexBuffer = ag.createIndexBuffer()

	init { logger.trace { "BatchBuilder2D[4]" } }

	var stencil = AG.StencilState()

	init { logger.trace { "BatchBuilder2D[5]" } }

	var colorMask = AG.ColorMaskState()

	init { logger.trace { "BatchBuilder2D[6]" } }

	var scissor: AG.Scissor? = null

	private val identity = Matrix2d()

	init { logger.trace { "BatchBuilder2D[7]" } }

	private val ptt1 = MPoint2d()
	private val ptt2 = MPoint2d()

	private val pt1 = MPoint2d()
	private val pt2 = MPoint2d()
	private val pt3 = MPoint2d()
	private val pt4 = MPoint2d()
	private val pt5 = MPoint2d()

	private val pt6 = MPoint2d()
	private val pt7 = MPoint2d()
	private val pt8 = MPoint2d()

	init { logger.trace { "BatchBuilder2D[8]" } }

	private val projMat = Matrix4()
	val viewMat = Matrix4()

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

	// @TODO: copy data from TexturedVertexArray
	private fun addVertex(x: Float, y: Float, u: Float, v: Float, colorMulInt: Int, colorAdd: Int) {
		vertices.setAlignedFloat32(vertexPos++, x)
		vertices.setAlignedFloat32(vertexPos++, y)
		vertices.setAlignedFloat32(vertexPos++, u)
		vertices.setAlignedFloat32(vertexPos++, v)
		vertices.setAlignedInt32(vertexPos++, colorMulInt)
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

	// 0..1
	// |  |
	// 3..2
	fun drawQuadFast(
		x0: Float,
		y0: Float,
		x1: Float,
		y1: Float,
		x2: Float,
		y2: Float,
		x3: Float,
		y3: Float,
		tex: Texture,
		colorMulInt: Int,
		colorAdd: Int,
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
			addVertex(x0, y0, tex.x0, tex.y0, colorMulInt, colorAdd)
			addVertex(x1, y1, tex.x1, tex.y0, colorMulInt, colorAdd)
			addVertex(x2, y2, tex.x1, tex.y1, colorMulInt, colorAdd)
			addVertex(x3, y3, tex.x0, tex.y1, colorMulInt, colorAdd)
		} else {
			addVertex(x0, y0, tex.x0, tex.y0, colorMulInt, colorAdd)
			addVertex(x1, y1, tex.x1, tex.y0, colorMulInt, colorAdd)
			addVertex(x2, y2, tex.x1, tex.y1, colorMulInt, colorAdd)
			addVertex(x3, y3, tex.x0, tex.y1, colorMulInt, colorAdd)
		}
	}

	fun drawVertices(array: TexturedVertexArray, vcount: Int = array.vcount, icount: Int = array.isize) {
		ensure(icount, vcount)

		for (idx in 0 until min(icount, array.isize)) addIndex(vertexCount + array.indices[idx])
		//for (p in array.points) addVertex(p.x, p.y, p.tx, p.ty, p.colMul, p.colAdd)

		FBuffer.copy(array._data, 0, vertices, vertexPos * 4, vcount * 6 * 4)
		//vertices.setAlignedArrayInt32(vertexPos, array.data, 0, vcount * 6)
		vertexCount += vcount
		vertexPos += vcount * 6
	}

	fun drawVertices(array: TexturedVertexArray, tex: Texture.Base, smoothing: Boolean, blendFactors: AG.Blending, vcount: Int = array.vcount, icount: Int = array.isize, program: Program? = null) {
		setStateFast(tex, smoothing, blendFactors, program)
		drawVertices(array, vcount, icount)
	}

	private fun ensure(indices: Int, vertices: Int) {
		if ((this.indexPos + indices >= maxIndices) || (this.vertexPos + vertices >= maxQuads)) {
			flush()
		}
	}

	fun setStateFast(tex: Texture.Base, smoothing: Boolean, blendFactors: AG.Blending, program: Program?) =
		setStateFast(tex.base, smoothing, blendFactors, program)

	fun setStateFast(tex: AG.Texture, smoothing: Boolean, blendFactors: AG.Blending, program: Program?) {
		if (tex != currentTex || currentSmoothing != smoothing || currentBlendFactors != blendFactors || currentProgram != program) {
			flush()
			currentTex = tex
			currentSmoothing = smoothing
			currentBlendFactors = if (tex.isFbo) blendFactors.toRenderFboIntoBack() else blendFactors
			currentProgram = program
		}
	}

	fun drawNinePatch(
		tex: Texture,
		x: Float = 0f,
		y: Float = 0f,
		width: Float = tex.width.toFloat(),
		height: Float = tex.height.toFloat(),
		posCuts: Array<MPoint2d>,
		texCuts: Array<MPoint2d>,
		m: Matrix2d = identity,
		filtering: Boolean = true,
		colorMulInt: Int = Colors.WHITE.rgba,
		colorAdd: Int = 0x7f7f7f7f,
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

				addVertex(p.x.toFloat(), p.y.toFloat(), t.x.toFloat(), t.y.toFloat(), colorMulInt, colorAdd)
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

	fun drawQuad(
		tex: Texture,
		x: Float = 0f,
		y: Float = 0f,
		width: Float = tex.width.toFloat(),
		height: Float = tex.height.toFloat(),
		m: Matrix2d = identity,
		filtering: Boolean = true,
		colorMulInt: Int = Colors.WHITE.rgba,
		colorAdd: Int = 0x7f7f7f7f,
		blendFactors: AG.Blending = BlendMode.NORMAL.factors,
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
			tex, colorMulInt, colorAdd, rotated
		)
	}

	companion object {
		init { logger.trace { "BatchBuilder2D.Companion[0]" } }

		val a_ColMul = DefaultShaders.a_Col
		val a_ColAdd = Attribute("a_Col2", VarType.Byte4, normalized = true)

		init { logger.trace { "BatchBuilder2D.Companion[1]" } }

		val v_ColMul = DefaultShaders.v_Col
		val v_ColAdd = Varying("v_Col2", VarType.Byte4)

		init { logger.trace { "BatchBuilder2D.Companion[2]" } }

		val LAYOUT = VertexLayout(DefaultShaders.a_Pos, DefaultShaders.a_Tex, a_ColMul, a_ColAdd)
		val VERTEX = VertexShader {
			DefaultShaders.apply {
				SET(v_Tex, a_Tex)
				SET(v_ColMul, a_ColMul)
				SET(v_ColAdd, a_ColAdd)
				SET(out, (u_ProjMat * u_ViewMat) * vec4(DefaultShaders.a_Pos, 0f.lit, 1f.lit))
			}
		}

		init { logger.trace { "BatchBuilder2D.Companion[3]" } }

		val PROGRAM_PRE = Program(
			vertex = VERTEX,
			fragment = buildTextureLookupFragment(premultiplied = true),
			name = "BatchBuilder2D.Premultiplied.Tinted"
		)

		val PROGRAM_NOPRE = Program(
			vertex = VERTEX,
			fragment = buildTextureLookupFragment(premultiplied = false),
			name = "BatchBuilder2D.NoPremultiplied.Tinted"
		)

		init { logger.trace { "BatchBuilder2D.Companion[4]" } }

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

		fun buildTextureLookupFragment(premultiplied: Boolean) = FragmentShader {
			DefaultShaders.apply {
				SET(out, texture2D(u_Tex, v_Tex["xy"]))
				if (premultiplied) {
					SET(out["rgb"], out["rgb"] / out["a"])
				}

				// @TODO: Kotlin.JS bug?
				//SET(out, (out["rgba"] * v_ColMul["rgba"]) + ((v_ColAdd["rgba"] - vec4(.5f, .5f, .5f, .5f)) * 2f))
				SET(out, (out["rgba"] * v_ColMul["rgba"]) + ((v_ColAdd["rgba"] - vec4(.5f, .5f, .5f, .5f)) * 2f.lit))

				//SET(out, t_Temp1)
				// Required for shape masks:
				if (premultiplied) {
					IF(out["a"] le 0f.lit) { DISCARD() }
				}
			}
		}

		//init { println(PROGRAM_PRE.fragment.toGlSl()) }
	}

	fun flush() {
		if (vertexCount > 0) {
			if (flipRenderTexture && ag.renderingToTexture) {
				projMat.setToOrtho(0f, ag.backHeight.toFloat(), ag.backWidth.toFloat(), 0f, -1f, 1f)
			} else {
				projMat.setToOrtho(0f, 0f, ag.backWidth.toFloat(), ag.backHeight.toFloat(), -1f, 1f)
			}

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

	inline fun setViewMatrixTemp(matrix: Matrix2d, temp: Matrix4 = Matrix4(), callback: () -> Unit) {
		flush()
		temp.copyFrom(this.viewMat)
		this.viewMat.copyFrom(matrix)
		try {
			callback()
		} finally {
			flush()
			this.viewMat.copyFrom(temp)
		}
	}

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
class TexturedVertexArray(var vcount: Int, val indices: IntArray, var isize: Int = indices.size) {
	val initialVcount = vcount
	//internal val data = IntArray(COMPONENTS_PER_VERTEX * vcount)
	internal val _data = FBuffer(COMPONENTS_PER_VERTEX * initialVcount * 4, direct = false)
	internal val f32 = _data.f32
	internal val i32 = _data.i32
	//val points = (0 until vcount).map { Item(data, it) }
	//val icount = indices.size

	companion object {
		const val COMPONENTS_PER_VERTEX = 6
		val QUAD_INDICES = intArrayOf(0, 1, 2,  3, 0, 2)
		fun quadIndices(quadCount: Int): IntArray {
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
	fun select(i: Int) = this.apply { offset = i * COMPONENTS_PER_VERTEX }
	fun setX(v: Float) = this.apply { f32[offset + 0] = v }
	fun setY(v: Float) = this.apply { f32[offset + 1] = v }
	fun setU(v: Float) = this.apply { f32[offset + 2] = v }
	fun setV(v: Float) = this.apply { f32[offset + 3] = v }
	fun setCMulInt(v: Int) = this.apply { i32[offset + 4] = v }
	fun setCAdd(v: Int) = this.apply { i32[offset + 5] = v }
	fun xy(x: Double, y: Double, matrix: Matrix2d) = setX(matrix.transformX(x, y).toFloat()).setY(matrix.transformY(x, y).toFloat())
	fun xy(x: Double, y: Double) = setX(x.toFloat()).setY(y.toFloat())
	fun uv(tx: Float, ty: Float) = setU(tx).setV(ty)
	fun cols(colMulInt: Int, colAdd: Int) = setCMulInt(colMulInt).setCAdd(colAdd)

	fun quad(index: Int, x: Double, y: Double, width: Double, height: Double, matrix: Matrix2d, bmp: BmpSlice, colMulInt: Int, colAdd: Int) {
		select(index + 0).xy(x, y, matrix).uv(bmp.tl_x, bmp.tl_y).cols(colMulInt, colAdd)
		select(index + 1).xy(x + width, y, matrix).uv(bmp.tr_x, bmp.tr_y).cols(colMulInt, colAdd)
		select(index + 2).xy(x + width, y + height, matrix).uv(bmp.br_x, bmp.br_y).cols(colMulInt, colAdd)
		select(index + 3).xy(x, y + height, matrix).uv(bmp.bl_x, bmp.bl_y).cols(colMulInt, colAdd)
	}

	private val bounds: BoundsBuilder = BoundsBuilder()
	fun getBounds(min: Int = 0, max: Int = vcount, out: Rectangle = Rectangle()): Rectangle {
		bounds.reset()
		for (n in min until max) {
			select(n)
			bounds.add(x.toDouble(), y.toDouble())
		}
		return bounds.getBounds(out)
	}

	val x: Float get() = f32[offset + 0]
	val y: Float get() = f32[offset + 1]
	val u: Float get() = f32[offset + 2]
	val v: Float get() = f32[offset + 3]
	val cMul: Int get() = i32[offset + 4]
	val cAdd: Int get() = i32[offset + 5]

	val vertexString: String get() = "V(xy=($x, $y),uv=$u, $v,cMul=$cMul,cAdd=$cAdd)"

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
	//	var x: Float; get() = Float.fromBits(data[offset + 0]); set(v) = run { data[offset + 0] = v.toBits() }
	//	var y: Float; get() = Float.fromBits(data[offset + 1]); set(v) = run { data[offset + 1] = v.toBits() }
	//	var tx: Float; get() = Float.fromBits(data[offset + 2]); set(v) = run { data[offset + 2] = v.toBits() }
	//	var ty: Float; get() = Float.fromBits(data[offset + 3]); set(v) = run { data[offset + 3] = v.toBits() }
	//	var colMul: Int; get() = data[offset + 4]; set(v) = run { data[offset + 4] = v }
	//	var colAdd: Int; get() = data[offset + 5]; set(v) = run { data[offset + 5] = v }
	//	fun setXY(x: Double, y: Double, matrix: Matrix2d) = this.apply {
	//		this.x = matrix.transformX(x, y).toFloat()
	//		this.y = matrix.transformY(x, y).toFloat()
	//	}
	//	fun setXY(x: Double, y: Double) = this.apply { this.x = x.toFloat() }.also { this.y = y.toFloat() }
	//	fun setTXY(tx: Float, ty: Float) = this.apply { this.tx = tx }.also { this.ty = ty }
	//	fun setCols(colMul: Int, colAdd: Int) = this.apply { this.colMul = colMul }.also { this.colAdd = colAdd }
	//}
}

class TexturedVertexArrayBuilder(count: Int) {
	val indices = IntArray(count * 6)
	val array = TexturedVertexArray(count * 4, indices)
	var offset = 0
	fun quad(x: Double, y: Double, width: Double, height: Double, matrix: Matrix2d, bmp: BmpSlice, colMulInt: Int, colAdd: Int) {
		val offset4 = offset * 4
		val i6 = offset * 6
		array.select(offset4 + 0).xy(x, y, matrix).uv(bmp.tl_x, bmp.tl_y).cols(colMulInt, colAdd)
		array.select(offset4 + 1).xy(x + width, y, matrix).uv(bmp.tr_x, bmp.tr_y).cols(colMulInt, colAdd)
		array.select(offset4 + 2).xy(x + width, y + height, matrix).uv(bmp.br_x, bmp.br_y).cols(colMulInt, colAdd)
		array.select(offset4 + 3).xy(x, y + height, matrix).uv(bmp.bl_x, bmp.bl_y).cols(colMulInt, colAdd)
		indices[i6 + 0] = offset4 + 0
		indices[i6 + 1] = offset4 + 1
		indices[i6 + 2] = offset4 + 2
		indices[i6 + 3] = offset4 + 3
		indices[i6 + 4] = offset4 + 0
		indices[i6 + 5] = offset4 + 2
		offset++
	}
	inline fun quad(x: Number, y: Number, width: Number, height: Number, matrix: Matrix2d, bmp: BmpSlice, colMulInt: Int, colAdd: Int) =
			quad(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble(), matrix, bmp, colMulInt, colAdd)
	fun build() = array.apply {
		vcount = offset * 4
		isize = offset * 6
	}
}

fun buildQuads(count: Int, build: TexturedVertexArrayBuilder.() -> Unit): TexturedVertexArray =
	TexturedVertexArrayBuilder(count).apply(build).build()

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
