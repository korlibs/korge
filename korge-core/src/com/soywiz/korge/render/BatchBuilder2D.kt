package com.soywiz.korge.render

import com.jtransc.FastMemory
import com.soywiz.korag.AG
import com.soywiz.korag.DefaultShaders
import com.soywiz.korag.geom.Matrix4
import com.soywiz.korag.shader.*
import com.soywiz.korge.view.BlendMode
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.Point2d

class BatchBuilder2D(val ag: AG, val maxQuads: Int = 1000) {
	val maxQuadsMargin = maxQuads + 9
	val maxVertices = maxQuads * 4
	val maxIndices = maxQuads * 6
	private val vertices = FastMemory.alloc(6 * 4 * maxVertices)
	private val indices = FastMemory.alloc(2 * maxIndices)
	private var vertexCount = 0
	private var vertexPos = 0
	private var indexPos = 0
	private var currentTex: Texture.Base? = null
	private var currentSmoothing: Boolean = false
	private var currentBlendFactors: AG.Blending = BlendMode.NORMAL.factors

	var stencil = AG.StencilState()
	var colorMask = AG.ColorMaskState()

	private fun addVertex(x: Float, y: Float, u: Float, v: Float, colMul: Int, colAdd: Int) {
		vertices.setAlignedFloat32(vertexPos++, x)
		vertices.setAlignedFloat32(vertexPos++, y)
		vertices.setAlignedFloat32(vertexPos++, u)
		vertices.setAlignedFloat32(vertexPos++, v)
		vertices.setAlignedInt32(vertexPos++, colMul)
		vertices.setAlignedInt32(vertexPos++, colAdd)
		vertexCount++
	}

	private fun addIndex(idx: Int) {
		indices.setAlignedInt16(indexPos++, idx)
	}

	// 0..1
	// |  |
	// 3..2
	fun drawQuadFast(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, tex: Texture, colMul: Int, colAdd: Int, rotated: Boolean = false) {
		ensure(6, 4)

		addIndex(vertexCount + 0)
		addIndex(vertexCount + 1)
		addIndex(vertexCount + 2)

		addIndex(vertexCount + 3)
		addIndex(vertexCount + 0)
		addIndex(vertexCount + 2)

		if (rotated) {
			// @TODO:
			addVertex(x0, y0, tex.x0, tex.y0, colMul, colAdd)
			addVertex(x1, y1, tex.x1, tex.y0, colMul, colAdd)
			addVertex(x2, y2, tex.x1, tex.y1, colMul, colAdd)
			addVertex(x3, y3, tex.x0, tex.y1, colMul, colAdd)
		} else {
			addVertex(x0, y0, tex.x0, tex.y0, colMul, colAdd)
			addVertex(x1, y1, tex.x1, tex.y0, colMul, colAdd)
			addVertex(x2, y2, tex.x1, tex.y1, colMul, colAdd)
			addVertex(x3, y3, tex.x0, tex.y1, colMul, colAdd)
		}
	}

	private fun ensure(indices: Int, vertices: Int) {
		if ((this.indexPos + indices >= maxIndices) || (this.vertexPos + vertices >= maxQuads)) {
			flush()
		}
	}

	fun setStateFast(tex: Texture.Base, smoothing: Boolean, blendFactors: AG.Blending) {
		if (tex != currentTex || currentSmoothing != smoothing || currentBlendFactors != blendFactors) {
			flush()
			currentTex = tex
			currentSmoothing = smoothing
			currentBlendFactors = blendFactors
		}
	}

	private val identity = Matrix2d()

	private val ptt1 = Point2d()
	private val ptt2 = Point2d()

	private val pt1 = Point2d()
	private val pt2 = Point2d()
	private val pt3 = Point2d()
	private val pt4 = Point2d()
	private val pt5 = Point2d()

	private val pt6 = Point2d()
	private val pt7 = Point2d()
	private val pt8 = Point2d()

	fun drawNinePatch(
		tex: Texture,
		x: Float = 0f, y: Float = 0f,
		width: Float = tex.width.toFloat(), height: Float = tex.height.toFloat(),
		posCuts: Array<Point2d>,
		texCuts: Array<Point2d>,
		m: Matrix2d = identity, filtering: Boolean = true, colMul: Int = -1, colAdd: Int = 0x7f7f7f7f, blendFactors: AG.Blending = BlendMode.NORMAL.factors
	) {
		val start = vertexCount

		setStateFast(tex.base, filtering, blendFactors)

		ensure(indices = 6 * 9, vertices = 4 * 4)

		val p_o = pt1.setToTransform(m, ptt1.setTo(x, y))
		val p_dU = pt2.setToSub(ptt1.setToTransform(m, ptt1.setTo(x + width, y)), p_o)
		val p_dV = pt3.setToSub(ptt1.setToTransform(m, ptt1.setTo(x, y + height)), p_o)

		val t_o = pt4.setTo(tex.x0, tex.y0)
		val t_dU = pt5.setToSub(ptt1.setTo(tex.x1, tex.y0), t_o)
		val t_dV = pt6.setToSub(ptt1.setTo(tex.x0, tex.y1), t_o)

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

				addVertex(p.x.toFloat(), p.y.toFloat(), t.x.toFloat(), t.y.toFloat(), colMul, colAdd)
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

	fun drawQuad(tex: Texture, x: Float = 0f, y: Float = 0f, width: Float = tex.width.toFloat(), height: Float = tex.height.toFloat(), m: Matrix2d = identity, filtering: Boolean = true, colMul: Int = -1, colAdd: Int = 0x7f7f7f7f, blendFactors: AG.Blending = BlendMode.NORMAL.factors, rotated: Boolean = false) {
		val x0 = x.toDouble()
		val x1 = (x + width).toDouble()
		val y0 = y.toDouble()
		val y1 = (y + height).toDouble()

		setStateFast(tex.base, filtering, blendFactors)

		drawQuadFast(
			m.transformXf(x0, y0), m.transformYf(x0, y0),
			m.transformXf(x1, y0), m.transformYf(x1, y0),
			m.transformXf(x1, y1), m.transformYf(x1, y1),
			m.transformXf(x0, y1), m.transformYf(x0, y1),
			tex, colMul, colAdd, rotated
		)
	}

	companion object {
		val a_ColMul = DefaultShaders.a_Col
		val a_ColAdd = Attribute("a_Col2", VarType.Byte4, normalized = true)

		val v_ColMul = DefaultShaders.v_Col
		val v_ColAdd = Varying("v_Col2", VarType.Byte4)

		val LAYOUT = VertexLayout(DefaultShaders.a_Pos, DefaultShaders.a_Tex, a_ColMul, a_ColAdd)
		val VERTEX = VertexShader {
			SET(DefaultShaders.v_Tex, DefaultShaders.a_Tex)
			SET(v_ColMul, a_ColMul)
			SET(v_ColAdd, a_ColAdd)
			SET(out, DefaultShaders.u_ProjMat * vec4(DefaultShaders.a_Pos, 0f.lit, 1f.lit))
		}

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

		val PROGRAM_PRE = Program(
			vertex = VERTEX,
			fragment = FragmentShader {
				DefaultShaders.apply {
					SET(t_Temp1, texture2D(u_Tex, v_Tex["xy"]))
					SET(t_Temp1["rgb"], t_Temp1["rgb"] / t_Temp1["a"])
					SET(t_Temp1, (t_Temp1["rgba"] * v_ColMul["rgba"]) + ((v_ColAdd["rgba"] - vec4(0.5f.lit, 0.5f.lit, 0.5f.lit, 0.5f.lit)) * 2f.lit))
					SET(out, t_Temp1)
					// Required for shape masks:
					IF(out["a"] le 0f.lit) { DISCARD() }
				}
			},
			name = "BatchBuilder2D.Tinted"
		)

		//init { println(PROGRAM_PRE.fragment.toGlSl()) }
	}

	private val projMat = Matrix4()

	fun flush() {
		if (vertexCount > 0) {
			val mat = if (ag.renderingToTexture) {
				projMat.setToOrtho(0f, ag.backHeight.toFloat(), ag.backWidth.toFloat(), 0f, -1f, 1f)
			} else {
				projMat.setToOrtho(0f, 0f, ag.backWidth.toFloat(), ag.backHeight.toFloat(), -1f, 1f)
			}

			val factors = currentBlendFactors

			ag.createVertexBuffer(vertices, 0, vertexPos * 4).use { vertexBuffer ->
				ag.createIndexBuffer(indices, 0, indexPos * 2).use { indexBuffer ->
					ag.draw(
						vertices = vertexBuffer,
						indices = indexBuffer,
						//program = if (currentTex?.base?.premultiplied ?: false) PROGRAM_PRE else PROGRAM_NORMAL,
						program = PROGRAM_PRE,
						type = AG.DrawType.TRIANGLES,
						vertexLayout = LAYOUT,
						vertexCount = indexPos,
						blending = factors,
						uniforms = mapOf<Uniform, Any>(
							DefaultShaders.u_ProjMat to mat,
							DefaultShaders.u_Tex to AG.TextureUnit(currentTex?.base, linear = currentSmoothing)
						),
						stencil = stencil,
						colorMask = colorMask
					)
				}
			}
		}

		vertexCount = 0
		vertexPos = 0
		indexPos = 0
		currentTex = null
	}
}
