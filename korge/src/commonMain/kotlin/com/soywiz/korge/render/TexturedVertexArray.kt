package com.soywiz.korge.render

import com.soywiz.kmem.*
import com.soywiz.korge.internal.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.triangle.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.triangle.poly2tri.*
import kotlin.native.concurrent.ThreadLocal

// @TODO: Call this mesh?
/**
 * Allows to build a set of textured and colored vertices. Where [vcount] is the number of vertices and [isize] [indices],
 * the maximum number of indices.
 *
 * [vcount] and [isize] could be decreased later, but not increased since the buffer is created at the beginning.
 */
class TexturedVertexArray(var vcount: Int, val indices: ShortArray, var isize: Int = indices.size) {
    /** The initial/maximum number of vertices */
    val initialVcount = vcount
    //internal val data = IntArray(TEXTURED_ARRAY_COMPONENTS_PER_VERTEX * vcount)
    //internal val _data = FBuffer(TEXTURED_ARRAY_COMPONENTS_PER_VERTEX * initialVcount * 4, direct = false)
    @PublishedApi internal val _data = FBuffer.allocNoDirect(TEXTURED_ARRAY_COMPONENTS_PER_VERTEX * initialVcount * 4)
    @PublishedApi internal val fast = _data.fast32
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

        fun fromPath(path: VectorPath, colorMul: RGBA = Colors.WHITE, colorAdd: ColorAdd = ColorAdd.NEUTRAL, matrix: Matrix? = null, doClipper: Boolean = true): TexturedVertexArray {
            //return fromTriangles(path.triangulateEarCut(), colorMul, colorAdd, matrix)
            //return fromTriangles(path.triangulatePoly2tri(), colorMul, colorAdd, matrix)
            return fromTriangles(path.triangulateSafe(doClipper), colorMul, colorAdd, matrix)
        }

        fun fromTriangles(triangles: TriangleList, colorMul: RGBA = Colors.WHITE, colorAdd: ColorAdd = ColorAdd.NEUTRAL, matrix: Matrix? = null): TexturedVertexArray {
            val tva = TexturedVertexArray(triangles.pointCount, triangles.indices, triangles.numIndices)
            tva.setSimplePoints(triangles.points, matrix, colorMul, colorAdd)
            return tva
        }

        /** This doesn't handle holes */
        fun fromPointArrayList(points: IPointArrayList, colorMul: RGBA = Colors.WHITE, colorAdd: ColorAdd = ColorAdd.NEUTRAL, matrix: Matrix? = null): TexturedVertexArray {
            val indices = ShortArray((points.size - 2) * 3)
            for (n in 0 until points.size - 2) {
                indices[n * 3 + 0] = (0).toShort()
                indices[n * 3 + 1] = (n + 1).toShort()
                indices[n * 3 + 2] = (n + 2).toShort()
            }
            val tva = TexturedVertexArray(points.size, indices)
            tva.setSimplePoints(points, matrix, colorMul, colorAdd)
            return tva
        }

    }

    fun setSimplePoints(points: IPointArrayList, matrix: Matrix?, colorMul: RGBA = Colors.WHITE, colorAdd: ColorAdd = ColorAdd.NEUTRAL) {
        if (matrix != null) {
            points.fastForEachWithIndex { index, x, y ->
                val xf = x.toFloat()
                val yf = y.toFloat()
                this.set(index, matrix.transformXf(xf, yf), matrix.transformYf(xf, yf), 0f, 0f, colorMul, colorAdd)
            }
        } else {
            points.fastForEachWithIndex { index, x, y -> this.set(index, x.toFloat(), y.toFloat(), 0f, 0f, colorMul, colorAdd) }
        }
    }

    @PublishedApi internal var offset = 0

    fun set(index: Int, x: Float, y: Float, u: Float, v: Float, colMul: RGBA, colAdd: ColorAdd) {
        select(index).setX(x).setY(y).setU(u).setV(v).setCMul(colMul).setCAdd(colAdd)
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
        fast.setF(offset + 0, v)
        return this
    }
    /** Sets the [y] of the vertex previously selected calling [select] */
    inline fun setY(v: Float): TexturedVertexArray {
        fast.setF(offset + 1, v)
        return this
    }
    /** Sets the [u] (x in texture) of the vertex previously selected calling [select] */
    inline fun setU(v: Float): TexturedVertexArray {
        fast.setF(offset + 2, v)
        return this
    }
    /** Sets the [v] (y in texture) of the vertex previously selected calling [select] */
    inline fun setV(v: Float): TexturedVertexArray {
        fast.setF(offset + 3, v)
        return this
    }
    /** Sets the [cMul] (multiplicative color) of the vertex previously selected calling [select] */
    inline fun setCMul(v: RGBA): TexturedVertexArray {
        fast.setI(offset + 4, v.value)
        return this
    }
    /** Sets the [cAdd] (additive color) of the vertex previously selected calling [select] */
    inline fun setCAdd(v: ColorAdd): TexturedVertexArray {
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
        quadV(fast, index * TEXTURED_ARRAY_COMPONENTS_PER_VERTEX, x, y, u, v, colMul.value, colAdd.value)
    }

    fun quadV(fast: Fast32Buffer, pos: Int, x: Float, y: Float, u: Float, v: Float, colMul: Int, colAdd: Int): Int {
        fast.setF(pos + 0, x)
        fast.setF(pos + 1, y)
        fast.setF(pos + 2, u)
        fast.setF(pos + 3, v)
        fast.setI(pos + 4, colMul)
        fast.setI(pos + 5, colAdd)
        return TEXTURED_ARRAY_COMPONENTS_PER_VERTEX
    }

    fun quadV(index: Int, x: Double, y: Double, u: Float, v: Float, colMul: RGBA, colAdd: ColorAdd) = quadV(index, x.toFloat(), y.toFloat(), u, v, colMul, colAdd)

    /**
     * Sets a textured quad at vertice [index] with the region defined by [x],[y] [width]x[height] and the [matrix],
     * using the texture coords defined by [BmpSlice] and color transforms [colMul] and [colAdd]
     */
    @OptIn(KorgeInternal::class)
    fun quad(index: Int, x: Double, y: Double, width: Double, height: Double, matrix: Matrix, bmp: BmpCoords, colMul: RGBA, colAdd: ColorAdd) {
        quad(index, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), matrix, bmp, colMul, colAdd)
    }

    @OptIn(KorgeInternal::class)
    fun quad(index: Int, x: Float, y: Float, width: Float, height: Float, matrix: Matrix, bmp: BmpCoords, colMul: RGBA, colAdd: ColorAdd) {
        quad(index, x, y, width, height, matrix, bmp.tl_x, bmp.tl_y, bmp.tr_x, bmp.tr_y, bmp.bl_x, bmp.bl_y, bmp.br_x, bmp.br_y, colMul, colAdd)
    }
    @OptIn(KorgeInternal::class)
    fun quad(
        index: Int, x: Float, y: Float, width: Float, height: Float, matrix: Matrix,
        tl_x: Float, tl_y: Float,
        tr_x: Float, tr_y: Float,
        bl_x: Float, bl_y: Float,
        br_x: Float, br_y: Float,
        colMul: RGBA, colAdd: ColorAdd,
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
        var pos = index * TEXTURED_ARRAY_COMPONENTS_PER_VERTEX
        val cm = colMul.value
        val ca = colAdd.value

        pos += quadV(fast, pos, x0, y0, tl_x, tl_y, cm, ca)
        pos += quadV(fast, pos, x1, y1, tr_x, tr_y, cm, ca)
        pos += quadV(fast, pos, x2, y2, br_x, br_y, cm, ca)
        pos += quadV(fast, pos, x3, y3, bl_x, bl_y, cm, ca)
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

    fun copyFrom(other: TexturedVertexArray) {
        arraycopy(other._data.arrayInt, 0, this._data.arrayInt, 0, this._data.size / 4)
        arraycopy(other.indices, 0, this.indices, 0, this.indices.size)
    }

    fun applyMatrix(matrix: Matrix) {
        for (n in 0 until vcount){
            select(n)
            val x = this.x
            val y = this.y
            setXY(matrix.transformXf(x, y), matrix.transformYf(x, y))
        }
    }

    fun copy(): TexturedVertexArray {
        val out = TexturedVertexArray(vcount, indices, isize)
        arraycopy(this._data.arrayByte, 0, out._data.arrayByte, 0, _data.size)
        return out
    }

    //class Item(private val data: IntArray, index: Int) {
    //	val offset = index * TEXTURED_ARRAY_COMPONENTS_PER_VERTEX
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


@PublishedApi internal const val TEXTURED_ARRAY_COMPONENTS_PER_VERTEX = 6
@KorgeInternal
@ThreadLocal
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
@PublishedApi internal const val VERTEX_INDEX_COL_ADD = 5
