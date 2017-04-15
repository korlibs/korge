package com.soywiz.korge.render

import com.jtransc.FastMemory
import com.soywiz.korag.AG
import com.soywiz.korag.DefaultShaders
import com.soywiz.korag.geom.Matrix4
import com.soywiz.korag.shader.*
import com.soywiz.korma.Matrix2d

class BatchBuilder2D(val ag: AG) {
	private val maxQuads = 10
	private val vertices = FastMemory.alloc(16 * 1024)
	private val indices = ShortArray(1024)
	private var vertexCount = 0
	private var vertexPos = 0
	private var indexPos = 0
	private var quadCount = 0
	private var currentTex: Texture.Base? = null
	private var currentSmoothing: Boolean = false

	private fun addVertex(x: Float, y: Float, u: Float, v: Float, col1: Int) {
		vertices.setAlignedFloat32(vertexPos++, x)
		vertices.setAlignedFloat32(vertexPos++, y)
		vertices.setAlignedFloat32(vertexPos++, u)
		vertices.setAlignedFloat32(vertexPos++, v)
		vertices.setAlignedInt32(vertexPos++, col1)
		vertexCount++
	}

	private fun addIndex(idx: Int) {
		indices[indexPos++] = idx.toShort()
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

	fun setStateFast(tex: Texture.Base, smoothing: Boolean) {
		if (tex != currentTex || currentSmoothing != smoothing) {
			flush()
			currentTex = tex
			currentSmoothing = smoothing
		}
	}

	private val identity = Matrix2d()

	fun addQuad(tex: Texture, x: Float = 0f, y: Float = 0f, width: Float = tex.width.toFloat(), height: Float = tex.height.toFloat(), m: Matrix2d = identity, filtering: Boolean = true, col1: Int = -1, col2: Int = 0, rotated: Boolean = false) {
		val x0 = x.toDouble()
		val x1 = (x + width).toDouble()
		val y0 = y.toDouble()
		val y1 = (y + height).toDouble()

		setStateFast(tex.base, filtering)

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
		val PROGRAM = Program(
			vertex = VertexShader {
				SET(DefaultShaders.v_Tex, DefaultShaders.a_Tex)
				SET(DefaultShaders.v_Col, DefaultShaders.a_Col)
				SET(out, DefaultShaders.u_ProjMat * vec4(DefaultShaders.a_Pos, 0f.lit, 1f.lit))
			},
			fragment = FragmentShader {
				SET(out, texture2D(DefaultShaders.u_Tex, DefaultShaders.v_Tex["xy"])["rgba"] * DefaultShaders.v_Col["rgba"])
			},
			name = "BatchBuilder2D.Tinted"
		)
	}

	private val projMat = Matrix4()

	fun flush() {
		if (vertexCount > 0) {
			val mat = projMat.setToOrtho(0f, 0f, ag.backWidth.toFloat(), ag.backHeight.toFloat(), -1f, 1f)

			ag.createVertexBuffer(vertices, 0, vertexPos * 4).use { vertexBuffer ->
				ag.createIndexBuffer(indices, 0, indexPos).use { indexBuffer ->
					ag.draw(
						vertices = vertexBuffer,
						indices = indexBuffer,
						program = PROGRAM,
						type = AG.DrawType.TRIANGLES,
						vertexLayout = LAYOUT,
						vertexCount = indexPos,
						uniforms = mapOf<Uniform, Any>(
							DefaultShaders.u_ProjMat to mat,
							DefaultShaders.u_Tex to AG.TextureUnit(currentTex?.base, linear = currentSmoothing)
						)
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
