package com.soywiz.korge.view

import com.soywiz.kmem.Float32Buffer
import com.soywiz.kmem.Float32BufferAlloc
import com.soywiz.kmem.Uint16Buffer
import com.soywiz.kmem.Uint16BufferAlloc
import com.soywiz.kmem.get
import com.soywiz.kmem.size
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.TexturedVertexArray
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korma.geom.BoundsBuilder
import com.soywiz.korma.geom.Rectangle

open class Mesh(
	var texture: BmpSlice? = null,
	var vertices: Float32Buffer = Float32BufferAlloc(0),
	var uvs: Float32Buffer = Float32BufferAlloc(0),
	var indices: Uint16Buffer = Uint16BufferAlloc(0),
	var drawMode: DrawModes = DrawModes.Triangles
) : View() {
	enum class DrawModes { Triangles, TriangleStrip }

	val textureNN get() = texture ?: Bitmaps.white
	var dirty: Int = 0
	var indexDirty: Int = 0

	var pivotX: Double = 0.0
	var pivotY: Double = 0.0

	fun updatedVertices() {
		dirtyVertices = true
	}

	private var tva = TexturedVertexArray.EMPTY
	private val bb = BoundsBuilder()
	private val localBounds = Rectangle()

	private fun recomputeVerticesIfRequired() {
		if (!dirtyVertices) return
		dirtyVertices = false

		// @TODO: Render in one batch without matrix multiplication in CPU
		val m = globalMatrix
		val cmul = this.renderColorMul
		val cadd = this.renderColorAdd
		val vcount = vertices.size / 2
		val isize = indices.size
		tva = if (vcount > tva.initialVcount || isize > tva.indices.size) TexturedVertexArray(vcount, ShortArray(isize)) else tva
		tva.vcount = vcount
		tva.icount = isize

		bb.reset()
        val pivotXf = pivotX.toFloat()
        val pivotYf = pivotY.toFloat()
		for (n in 0 until tva.icount) tva.indices[n] = indices[n].toShort()
		for (n in 0 until tva.vcount) {
			val x = vertices[n * 2 + 0] + pivotXf
			val y = vertices[n * 2 + 1] + pivotYf

            tva.quadV(n, m.transformXf(x, y), m.transformYf(x, y), uvs[n * 2 + 0], uvs[n * 2 + 1], cmul, cadd)
			bb.add(x, y)
		}
		bb.getBounds(localBounds)
	}

	override fun renderInternal(ctx: RenderContext) {
		recomputeVerticesIfRequired()
        ctx.useBatcher { batch ->
            batch.drawVertices(tva, ctx.getTex(textureNN).base, true, renderBlendMode.factors)
        }
	}

	override fun getLocalBoundsInternal(out: Rectangle) {
		recomputeVerticesIfRequired()
		out.copyFrom(localBounds)
	}
}

fun <T : Mesh> T.pivot(x: Double, y: Double): T {
    this.pivotX = x
    this.pivotY = y
    return this
}
