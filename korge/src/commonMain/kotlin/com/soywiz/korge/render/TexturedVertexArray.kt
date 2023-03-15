package com.soywiz.korge.render

import com.soywiz.kmem.*
import com.soywiz.korge.internal.KorgeInternal
import com.soywiz.korim.bitmap.BmpCoords
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.*
import kotlin.native.concurrent.SharedImmutable

// @TODO: Call this mesh?
/**
 * Allows to build a set of textured and colored vertices. Where [vcount] is the number of vertices and [icount] [indices],
 * the maximum number of indices.
 *
 * [vcount] and [icount] could be decreased later, but not increased since the buffer is created at the beginning.
 */
class TexturedVertexArray(vcount: Int, val indices: ShortArray, icount: Int = indices.size) {
    /** The initial/maximum number of vertices */
    val initialVcount = vcount
    var vcount: Int = vcount
        set(value) {
            if (field == value) return // Avoid mutating in K/N
            //if (initialVcount == 0) error("trying to change TexturedVertexArray.EMPTY")
            if (value > initialVcount) error("value($value) > initialVcount($initialVcount)")
            field = value
        }
    var icount: Int = icount
        set(value) {
            if (field == value) return // Avoid mutating in K/N
            //if (indices.isEmpty()) error("trying to change TexturedVertexArray.EMPTY")
            if (value > indices.size) error("value($value) > indices.size(${indices.size})")
            field = value
        }
    //internal val data = IntArray(TEXTURED_ARRAY_COMPONENTS_PER_VERTEX * vcount)
    //internal val _data = Buffer(TEXTURED_ARRAY_COMPONENTS_PER_VERTEX * initialVcount * 4, direct = false)
    @PublishedApi internal val _data: Buffer = Buffer.allocNoDirect(TEXTURED_ARRAY_COMPONENTS_PER_VERTEX * initialVcount * 4)
    @PublishedApi internal val fast = _data
    //private val f32 = _data.f32
    //private val i32 = _data.i32
    //val points = (0 until vcount).map { Item(data, it) }
    //val icount = indices.size

    companion object {
        fun forQuads(quadCount: Int) = TexturedVertexArray(quadCount * 4, quadIndices(quadCount))

        val EMPTY = TexturedVertexArray(0, ShortArray(0))

        // // @TODO: const val optimization issue in Kotlin/Native: https://youtrack.jetbrains.com/issue/KT-46425
        @KorgeInternal
        inline val COMPONENTS_PER_VERTEX get() = TEXTURED_ARRAY_COMPONENTS_PER_VERTEX

        @KorgeInternal
        inline val QUAD_INDICES get() = TEXTURED_ARRAY_QUAD_INDICES

        inline val EMPTY_INT_ARRAY get() = TEXTURED_ARRAY_EMPTY_INT_ARRAY

        /** Builds indices for drawing triangles when the vertices information is stored as quads (4 vertices per quad primitive) */
        inline fun quadIndices(quadCount: Int): ShortArray = TEXTURED_ARRAY_quadIndices(quadCount)

        /** This doesn't handle holes */
        fun fromPointArrayList(points: PointList, colorMul: RGBA = Colors.WHITE, matrix: Matrix = Matrix.NIL): TexturedVertexArray {
            val indices = ShortArray((points.size - 2) * 3)
            for (n in 0 until points.size - 2) {
                indices[n * 3 + 0] = (0).toShort()
                indices[n * 3 + 1] = (n + 1).toShort()
                indices[n * 3 + 2] = (n + 2).toShort()
            }
            val tva = TexturedVertexArray(points.size, indices)
            tva.setSimplePoints(points, matrix, colorMul)
            return tva
        }

    }

    fun setSimplePoints(points: PointList, matrix: Matrix, colorMul: RGBA = Colors.WHITE) {
        points.fastForEachIndexed { index, p ->
            this.set(index, p.transformed(matrix), Point(), colorMul)
        }
    }

    @PublishedApi internal var offset = 0

    fun set(index: Int, p: Point, tex: Point, colMul: RGBA) = set(index, p.x, p.y, tex.x, tex.y, colMul)

    fun set(index: Int, x: Float, y: Float, u: Float, v: Float, colMul: RGBA) {
        select(index).setX(x).setY(y).setU(u).setV(v).setCMul(colMul)
    }

    fun setXY(x: Float, y: Float) {
        setX(x).setY(y)
    }

    /** Moves the cursor for setting vertexs to the vertex [i] */
    inline fun select(i: Int): TexturedVertexArray {
        offset = i * TEXTURED_ARRAY_COMPONENTS_PER_VERTEX
        return this
    }
    /** Sets the [x] of the vertex previously selected calling [select] */
    inline fun setX(v: Float): TexturedVertexArray {
        fast.setFloat32(offset + 0, v)
        return this
    }
    /** Sets the [y] of the vertex previously selected calling [select] */
    inline fun setY(v: Float): TexturedVertexArray {
        fast.setFloat32(offset + 1, v)
        return this
    }
    /** Sets the [u] (x in texture) of the vertex previously selected calling [select] */
    inline fun setU(v: Float): TexturedVertexArray {
        fast.setFloat32(offset + 2, v)
        return this
    }
    /** Sets the [v] (y in texture) of the vertex previously selected calling [select] */
    inline fun setV(v: Float): TexturedVertexArray {
        fast.setFloat32(offset + 3, v)
        return this
    }
    /** Sets the [cMul] (multiplicative color) of the vertex previously selected calling [select] */
    inline fun setCMul(v: RGBA): TexturedVertexArray {
        fast.setInt32(offset + 4, v.value)
        return this
    }
    /** Sets the [x] and [y] with the [matrix] transform applied of the vertex previously selected calling [select] */
    fun xy(x: Double, y: Double, matrix: Matrix) = setX(matrix.transformX(x, y).toFloat()).setY(matrix.transformY(x, y).toFloat())
    /** Sets the [x] and [y] of the vertex previously selected calling [select] */
    fun xy(x: Double, y: Double) = setX(x.toFloat()).setY(y.toFloat())
    /** Sets the [u] and [v] of the vertex previously selected calling [select] */
    fun uv(tx: Float, ty: Float) = setU(tx).setV(ty)
    /** Sets the [cMul]  (multiplicative colors) of the vertex previously selected calling [select] */
    fun cols(colMul: RGBA) = setCMul(colMul)

    fun quadV(index: Int, x: Float, y: Float, u: Float, v: Float, colMul: RGBA) {
        quadV(fast, index * TEXTURED_ARRAY_COMPONENTS_PER_VERTEX, x, y, u, v, colMul.value)
    }

    fun quadV(fast: Buffer, pos: Int, x: Float, y: Float, u: Float, v: Float, colMul: Int): Int {
        fast.setFloat32(pos + 0, x)
        fast.setFloat32(pos + 1, y)
        fast.setFloat32(pos + 2, u)
        fast.setFloat32(pos + 3, v)
        fast.setInt32(pos + 4, colMul)
        fast.setInt32(pos + 5, 0)
        return TEXTURED_ARRAY_COMPONENTS_PER_VERTEX
    }

    /**
     * Sets a textured quad at vertice [index] with the region defined by [x],[y] [width]x[height] and the [matrix],
     * using the texture coords defined by [BmpSlice] and color transforms [colMul]
     */
    @OptIn(KorgeInternal::class)
    fun quad(index: Int, x: Double, y: Double, width: Double, height: Double, matrix: Matrix, bmp: BmpCoords, colMul: RGBA) {
        quad(index, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), matrix, bmp, colMul)
    }

    @OptIn(KorgeInternal::class)
    fun quad(index: Int, x: Float, y: Float, width: Float, height: Float, matrix: Matrix, bmp: BmpCoords, colMul: RGBA) {
        quad(index, x, y, width, height, matrix, bmp.tlX, bmp.tlY, bmp.trX, bmp.trY, bmp.blX, bmp.blY, bmp.brX, bmp.brY, colMul)
    }
    @OptIn(KorgeInternal::class)
    fun quad(
        index: Int, x: Float, y: Float, width: Float, height: Float, matrix: Matrix,
        tl_x: Float, tl_y: Float,
        tr_x: Float, tr_y: Float,
        bl_x: Float, bl_y: Float,
        br_x: Float, br_y: Float,
        colMul: RGBA,
    ) {
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

        val af = matrix.a
        val cf = matrix.c
        val txf = matrix.tx
        val x0 = af * x + cf * y + txf
        val x1 = af * xw + cf * y + txf
        val x2 = af * xw + cf * yh + txf
        val x3 = af * x + cf * yh + txf

        val df = matrix.d
        val bf = matrix.b
        val tyf = matrix.ty
        val y0 = df * y + bf * x + tyf
        val y1 = df * y + bf * xw + tyf
        val y2 = df * yh + bf * xw + tyf
        val y3 = df * yh + bf * x + tyf

        val fast = this.fast
        var pos = index * TEXTURED_ARRAY_COMPONENTS_PER_VERTEX
        val cm = colMul.value

        pos += quadV(fast, pos, x0, y0, tl_x, tl_y, cm)
        pos += quadV(fast, pos, x1, y1, tr_x, tr_y, cm)
        pos += quadV(fast, pos, x2, y2, br_x, br_y, cm)
        pos += quadV(fast, pos, x3, y3, bl_x, bl_y, cm)
    }


    private val bounds: BoundsBuilder = BoundsBuilder()

    /**
     * Returns the bounds of the vertices defined in the indices from [min] to [max] (excluding) as [MRectangle]
     * Allows to define the output as [out] to be allocation-free, setting the [out] [MRectangle] and returning it.
     */
    fun getBounds(min: Int = 0, max: Int = vcount, out: MRectangle = MRectangle()): MRectangle {
        bounds.reset()
        for (n in min until max) {
            select(n)
            bounds.add(x.toDouble(), y.toDouble())
        }
        return bounds.getBounds(out)
    }

    /** [x] at the previously vertex selected by calling [select] */
    val x: Float get() = fast.getFloat32(offset + 0)
    /** [y] at the previously vertex selected by calling [select] */
    val y: Float get() = fast.getFloat32(offset + 1)
    /** [u] (x in texture) at the previously vertex selected by calling [select] */
    val u: Float get() = fast.getFloat32(offset + 2)
    /** [v] (y in texture) at the previously vertex selected by calling [select] */
    val v: Float get() = fast.getFloat32(offset + 3)
    /** [cMul] (multiplicative color) at the previously vertex selected by calling [select] */
    val cMul: Int get() = fast.getInt32(offset + 4)

    /** Describes the vertice previously selected by calling [select] */
    val vertexString: String get() = "V(xy=($x, $y),uv=$u, $v,cMul=$cMul)"

    /** Describes a vertex at [index] */
    fun str(index: Int): String {
        val old = this.offset
        try {
            return select(index).vertexString
        } finally {
            this.offset = old
        }
    }

    fun copyFrom(other: TexturedVertexArray) {
        arraycopy(other._data.i32, 0, this._data.i32, 0, this._data.size / 4)
        arraycopy(other.indices, 0, this.indices, 0, this.indices.size)
    }

    fun applyMatrix(matrix: Matrix) {
        for (n in 0 until vcount){
            select(n)
            val x = this.x
            val y = this.y
            setXY(matrix.transformX(x, y), matrix.transformY(x, y))
        }
    }

    fun copy(): TexturedVertexArray {
        val out = TexturedVertexArray(vcount, indices, icount)
        arraycopy(this._data, 0, out._data, 0, _data.size)
        return out
    }
}

// @TODO: Should we move this outside?
class ShrinkableTexturedVertexArray(val vertices: TexturedVertexArray, var vcount: Int = 0, var icount: Int = 0) {
    fun quadV(x: Double, y: Double, u: Float, v: Float, colMul: RGBA) {
        vertices.quadV(vcount++, x.toFloat(), y.toFloat(), u, v, colMul)
    }

    fun reset() {
        vcount = 0
        icount = 0
    }

    override fun toString(): String = "GrowableTexturedVertexArray(vcount=$vcount, icount=$icount)"
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


@PublishedApi internal const val TEXTURED_ARRAY_COMPONENTS_PER_VERTEX = 6
@KorgeInternal
@SharedImmutable
@PublishedApi internal val TEXTURED_ARRAY_QUAD_INDICES = shortArrayOf(0, 1, 2,  3, 0, 2)
@PublishedApi internal val TEXTURED_ARRAY_EMPTY_INT_ARRAY = IntArray(0)
@PublishedApi internal val TEXTURED_ARRAY_EMPTY_SHORT_ARRAY = ShortArray(0)

/** Builds indices for drawing triangles when the vertices information is stored as quads (4 vertices per quad primitive) */
@PublishedApi internal fun TEXTURED_ARRAY_quadIndices(quadCount: Int): ShortArray {
    if (quadCount == 0) return TEXTURED_ARRAY_EMPTY_SHORT_ARRAY
    val out = ShortArray(quadCount * 6)
    var m = 0
    var base = 0
    for (n in 0 until quadCount) {
        out[m++] = (base + 0).toShort()
        out[m++] = (base + 1).toShort()
        out[m++] = (base + 2).toShort()
        out[m++] = (base + 3).toShort()
        out[m++] = (base + 0).toShort()
        out[m++] = (base + 2).toShort()
        base += 4
    }
    //QUAD_INDICES.repeat(quadCount)
    return out
}

@PublishedApi internal const val VERTEX_INDEX_SIZE = 6
@PublishedApi internal const val VERTEX_INDEX_X = 0
@PublishedApi internal const val VERTEX_INDEX_Y = 1
@PublishedApi internal const val VERTEX_INDEX_U = 2
@PublishedApi internal const val VERTEX_INDEX_V = 3
@PublishedApi internal const val VERTEX_INDEX_COL_MUL = 4
