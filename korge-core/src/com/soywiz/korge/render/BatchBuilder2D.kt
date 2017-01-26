package com.soywiz.korge.render

import com.jtransc.FastMemory
import com.soywiz.korag.AG
import com.soywiz.korag.DefaultShaders
import com.soywiz.korag.shader.FragmentShader
import com.soywiz.korag.shader.Program
import com.soywiz.korag.shader.VertexLayout
import com.soywiz.korag.shader.VertexShader
import com.soywiz.korim.geom.Matrix2d

class BatchBuilder2D(val ag: AG) {
	private val maxQuads = 10
	private val vertices = FastMemory.alloc(16 * 1024)
	private val indices = ShortArray(1024)
	private var vertexCount = 0
	private var vertexPos = 0
	private var indexPos = 0
	private var quadCount = 0

	private fun addVertex(x: Float, y: Float, u: Float, v: Float) {
		vertices.setAlignedFloat32(vertexPos++, x)
		vertices.setAlignedFloat32(vertexPos++, y)
		vertices.setAlignedFloat32(vertexPos++, u)
		vertices.setAlignedFloat32(vertexPos++, v)
		vertexCount++
	}

	private fun addIndex(idx: Int) {
		indices[indexPos++] = idx.toShort()
	}

	// 0..1
	// |  |
	// 3..2
	fun addQuad(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, tex: Texture) {
		addIndex(vertexCount + 0)
		addIndex(vertexCount + 1)
		addIndex(vertexCount + 2)

		addIndex(vertexCount + 3)
		addIndex(vertexCount + 1)
		addIndex(vertexCount + 2)

		addVertex(x0, y0, tex.x0, tex.y0)
		addVertex(x1, y1, tex.x1, tex.y0)
		addVertex(x2, y2, tex.x1, tex.y1)
		addVertex(x3, y3, tex.x0, tex.y1)
	}

	private val identity = Matrix2d()

	fun addQuad(tex: Texture, x: Float = 0f, y: Float = 0f, width: Float = tex.width.toFloat(), height: Float = tex.height.toFloat(), m: Matrix2d = identity) {
		val x0 = x.toDouble()
		val x1 = (x + width).toDouble()
		val y0 = y.toDouble()
		val y1 = (y + height).toDouble()

		addQuad(
			m.transformX(x0, y0).toFloat(), m.transformY(x0, y0).toFloat(),
			m.transformX(x1, y0).toFloat(), m.transformY(x1, y0).toFloat(),
			m.transformX(x1, y1).toFloat(), m.transformY(x1, y1).toFloat(),
			m.transformX(x0, y1).toFloat(), m.transformY(x0, y1).toFloat(),
			tex
		)
		quadCount++

		if (quadCount >= maxQuads) {
			flush()
		}
	}

	companion object {
		val LAYOUT = VertexLayout(DefaultShaders.a_Pos, DefaultShaders.a_Tex)
		val PROGRAM = Program(
			vertex = VertexShader {
				SET(DefaultShaders.v_Tex, DefaultShaders.a_Tex)
				SET(out, DefaultShaders.u_ProjMat * vec4(DefaultShaders.a_Pos, 0f.lit, 1f.lit))
			},
			fragment = FragmentShader {
				SET(out, texture2D(DefaultShaders.u_Tex, DefaultShaders.v_Tex["xy"])["rgba"])
			},
			name = "BatchBuilder2D"
		)
	}

	fun flush() {
		ag.createVertexBuffer(vertices, 0, vertexPos * 4).use { vertexBuffer ->
			ag.createIndexBuffer(indices, 0, indexPos).use { indexBuffer ->
				ag.draw(
					vertices = vertexBuffer,
					indices = indexBuffer,
					program = PROGRAM,
					type = AG.DrawType.TRIANGLES,
					vertexLayout = LAYOUT,
					vertexCount = indexPos
				)
			}
		}

		vertexCount = 0
		vertexPos = 0
		indexPos = 0
		quadCount = 0
	}
}