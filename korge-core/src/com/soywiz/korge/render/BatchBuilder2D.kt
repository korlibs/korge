package com.soywiz.korge.render

import com.jtransc.FastMemory
import com.soywiz.korag.AG
import com.soywiz.korag.DefaultShaders
import com.soywiz.korag.geom.Matrix4
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.toGlSl
import com.soywiz.korma.Matrix2d

object MyBlendFactors{
	val NORMAL = AG.BlendFactors.NORMAL
	val ADD = AG.BlendFactors.ADD
	//val NORMAL = AG.BlendFactors.NORMAL_PREMULT
	//val ADD = AG.BlendFactors.ADD_PREMULT
}

class BatchBuilder2D(val ag: AG, val maxQuads: Int = 4000) {
	private val vertices = FastMemory.alloc(16 * 4 * maxQuads * 4)
	private val indices = FastMemory.alloc(2 * maxQuads * 6)
	private var vertexCount = 0
	private var vertexPos = 0
	private var indexPos = 0
	private var quadCount = 0
	private var currentTex: Texture.Base? = null
	private var currentSmoothing: Boolean = false
	private var currentBlendFactors: AG.BlendFactors = MyBlendFactors.NORMAL

	var stencil = AG.StencilState()
	var colorMask = AG.ColorMaskState()

	private fun addVertex(x: Float, y: Float, u: Float, v: Float, col1: Int) {
		vertices.setAlignedFloat32(vertexPos++, x)
		vertices.setAlignedFloat32(vertexPos++, y)
		vertices.setAlignedFloat32(vertexPos++, u)
		vertices.setAlignedFloat32(vertexPos++, v)
		vertices.setAlignedInt32(vertexPos++, col1)
		vertexCount++
	}

	private fun addIndex(idx: Int) {
		indices.setAlignedInt16(indexPos++, idx)
	}

	// 0..1
	// |  |
	// 3..2
	fun addQuadFast(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, tex: Texture, col1: Int, rotated: Boolean = false) {
		addIndex(vertexCount + 0)
		addIndex(vertexCount + 1)
		addIndex(vertexCount + 2)

		addIndex(vertexCount + 3)
		addIndex(vertexCount + 0)
		addIndex(vertexCount + 2)

		if (rotated) {
			// @TODO:
			addVertex(x0, y0, tex.x0, tex.y0, col1)
			addVertex(x1, y1, tex.x1, tex.y0, col1)
			addVertex(x2, y2, tex.x1, tex.y1, col1)
			addVertex(x3, y3, tex.x0, tex.y1, col1)
		} else {
			addVertex(x0, y0, tex.x0, tex.y0, col1)
			addVertex(x1, y1, tex.x1, tex.y0, col1)
			addVertex(x2, y2, tex.x1, tex.y1, col1)
			addVertex(x3, y3, tex.x0, tex.y1, col1)
		}

		quadCount++

		if (quadCount >= maxQuads) flush()
	}

	fun setStateFast(tex: Texture.Base, smoothing: Boolean, blendFactors: AG.BlendFactors) {
		if (tex != currentTex || currentSmoothing != smoothing || currentBlendFactors != blendFactors) {
			flush()
			currentTex = tex
			currentSmoothing = smoothing
			currentBlendFactors = blendFactors
		}
	}

	private val identity = Matrix2d()

	fun addQuad(tex: Texture, x: Float = 0f, y: Float = 0f, width: Float = tex.width.toFloat(), height: Float = tex.height.toFloat(), m: Matrix2d = identity, filtering: Boolean = true, col1: Int = -1, col2: Int = 0, blendFactors: AG.BlendFactors = MyBlendFactors.NORMAL, rotated: Boolean = false) {
		val x0 = x.toDouble()
		val x1 = (x + width).toDouble()
		val y0 = y.toDouble()
		val y1 = (y + height).toDouble()

		setStateFast(tex.base, filtering, blendFactors)

		addQuadFast(
			m.transformXf(x0, y0), m.transformYf(x0, y0),
			m.transformXf(x1, y0), m.transformYf(x1, y0),
			m.transformXf(x1, y1), m.transformYf(x1, y1),
			m.transformXf(x0, y1), m.transformYf(x0, y1),
			tex, col1, rotated
		)
	}

	companion object {
		val LAYOUT = VertexLayout(DefaultShaders.a_Pos, DefaultShaders.a_Tex, DefaultShaders.a_Col)
		val VERTEX = VertexShader {
			SET(DefaultShaders.v_Tex, DefaultShaders.a_Tex)
			SET(DefaultShaders.v_Col, DefaultShaders.a_Col)
			SET(out, DefaultShaders.u_ProjMat * vec4(DefaultShaders.a_Pos, 0f.lit, 1f.lit))
		}

		val PROGRAM_NORMAL = Program(
			vertex = VERTEX,
			fragment = FragmentShader {
				SET(out, texture2D(DefaultShaders.u_Tex, DefaultShaders.v_Tex["xy"])["rgba"] * DefaultShaders.v_Col["rgba"])
				// Required for shape masks:
				IF(out["a"] le 0.lit) { DISCARD() }
			},
			name = "BatchBuilder2D.Tinted"
		)

		val PROGRAM_PRE = Program(
			vertex = VERTEX,
			fragment = FragmentShader {
				DefaultShaders.apply {
					SET(t_Temp1, texture2D(u_Tex, v_Tex["xy"]))
					SET(t_Temp1["rgb"], t_Temp1["rgb"] / t_Temp1["a"])
					SET(out, t_Temp1["rgba"] * v_Col["rgba"])
					// Required for shape masks:
					IF(out["a"] le 0.lit) { DISCARD() }
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
						program = if (currentTex?.base?.premultiplied ?: false) PROGRAM_PRE else PROGRAM_NORMAL,
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
		quadCount = 0
		currentTex = null
	}
}
