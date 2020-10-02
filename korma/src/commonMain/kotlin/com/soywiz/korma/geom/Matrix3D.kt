package com.soywiz.korma.geom

import com.soywiz.korma.interpolation.*
import kotlin.math.*
import kotlin.native.concurrent.*

enum class MajorOrder { ROW, COLUMN }

// Stored as four consecutive column vectors (effectively stored in column-major order) see https://en.wikipedia.org/wiki/Row-_and_column-major_order
class Matrix3D {
    val data: FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, // column-0
        0f, 1f, 0f, 0f, // column-1
        0f, 0f, 1f, 0f, // column-2
        0f, 0f, 0f, 1f  // column-3
    )

    companion object {
        const val M00 = 0
        const val M10 = 1
        const val M20 = 2
        const val M30 = 3

        const val M01 = 4
        const val M11 = 5
        const val M21 = 6
        const val M31 = 7

        const val M02 = 8
        const val M12 = 9
        const val M22 = 10
        const val M32 = 11

        const val M03 = 12
        const val M13 = 13
        const val M23 = 14
        const val M33 = 15

        operator fun invoke(m: Matrix3D) = Matrix3D().copyFrom(m)

        fun rowMajorIndex(row: Int, column: Int) = row * 4 + column
        fun columnMajorIndex(row: Int, column: Int) = column * 4 + row
        fun index(row: Int, column: Int, order: MajorOrder) = if (order == MajorOrder.ROW) rowMajorIndex(row, column) else columnMajorIndex(row, column)

        fun multiply(left: FloatArray, right: FloatArray, out: FloatArray = FloatArray(16)): FloatArray {
            for (row in 0 until 4) {
                for (column in 0 until 4) {
                    var value = 0f
                    for (n in 0 until 4) {
                        value += left[columnMajorIndex(row, n)] * right[columnMajorIndex(n, column)]
                    }
                    out[columnMajorIndex(row, column)] = value
                }
            }
            return out
        }
    }

    operator fun get(row: Int, column: Int): Float = data[columnMajorIndex(row, column)]
    operator fun set(row: Int, column: Int, value: Float) = run { data[columnMajorIndex(row, column)] = value }
    operator fun set(row: Int, column: Int, value: Double) = this.set(row, column, value.toFloat())
    operator fun set(row: Int, column: Int, value: Int) = this.set(row, column, value.toFloat())

    inline var v00: Float get() = data[M00]; set(v) = run { data[M00] = v }
    inline var v01: Float get() = data[M01]; set(v) = run { data[M01] = v }
    inline var v02: Float get() = data[M02]; set(v) = run { data[M02] = v }
    inline var v03: Float get() = data[M03]; set(v) = run { data[M03] = v }

    inline var v10: Float get() = data[M10]; set(v) = run { data[M10] = v }
    inline var v11: Float get() = data[M11]; set(v) = run { data[M11] = v }
    inline var v12: Float get() = data[M12]; set(v) = run { data[M12] = v }
    inline var v13: Float get() = data[M13]; set(v) = run { data[M13] = v }

    inline var v20: Float get() = data[M20]; set(v) = run { data[M20] = v }
    inline var v21: Float get() = data[M21]; set(v) = run { data[M21] = v }
    inline var v22: Float get() = data[M22]; set(v) = run { data[M22] = v }
    inline var v23: Float get() = data[M23]; set(v) = run { data[M23] = v }

    inline var v30: Float get() = data[M30]; set(v) = run { data[M30] = v }
    inline var v31: Float get() = data[M31]; set(v) = run { data[M31] = v }
    inline var v32: Float get() = data[M32]; set(v) = run { data[M32] = v }
    inline var v33: Float get() = data[M33]; set(v) = run { data[M33] = v }

    val transposed: Matrix3D get() = this.clone().transpose()

    fun transpose(): Matrix3D = setColumns(
        v00, v01, v02, v03,
        v10, v11, v12, v13,
        v20, v21, v22, v23,
        v30, v31, v32, v33
    )

    fun setRows(
        a00: Float, a01: Float, a02: Float, a03: Float,
        a10: Float, a11: Float, a12: Float, a13: Float,
        a20: Float, a21: Float, a22: Float, a23: Float,
        a30: Float, a31: Float, a32: Float, a33: Float
    ): Matrix3D = this.apply {
        v00 = a00; v01 = a01; v02 = a02; v03 = a03
        v10 = a10; v11 = a11; v12 = a12; v13 = a13
        v20 = a20; v21 = a21; v22 = a22; v23 = a23
        v30 = a30; v31 = a31; v32 = a32; v33 = a33
    }

    fun setColumns(
        a00: Float, a10: Float, a20: Float, a30: Float,
        a01: Float, a11: Float, a21: Float, a31: Float,
        a02: Float, a12: Float, a22: Float, a32: Float,
        a03: Float, a13: Float, a23: Float, a33: Float
    ): Matrix3D = this.apply {
        v00 = a00; v01 = a01; v02 = a02; v03 = a03
        v10 = a10; v11 = a11; v12 = a12; v13 = a13
        v20 = a20; v21 = a21; v22 = a22; v23 = a23
        v30 = a30; v31 = a31; v32 = a32; v33 = a33
    }

    fun setColumns4x4(f: FloatArray, offset: Int) = setColumns(
        f[offset + 0], f[offset + 1], f[offset + 2], f[offset + 3],
        f[offset + 4], f[offset + 5], f[offset + 6], f[offset + 7],
        f[offset + 8], f[offset + 9], f[offset + 10], f[offset + 11],
        f[offset + 12], f[offset + 13], f[offset + 14], f[offset + 15]
    )

    fun setRows4x4(f: FloatArray, offset: Int) = setRows(
        f[offset + 0], f[offset + 1], f[offset + 2], f[offset + 3],
        f[offset + 4], f[offset + 5], f[offset + 6], f[offset + 7],
        f[offset + 8], f[offset + 9], f[offset + 10], f[offset + 11],
        f[offset + 12], f[offset + 13], f[offset + 14], f[offset + 15]
    )

    fun setColumns3x3(f: FloatArray, offset: Int) = setColumns(
        f[offset + 0], f[offset + 1], f[offset + 2], 0f,
        f[offset + 3], f[offset + 4], f[offset + 5], 0f,
        f[offset + 6], f[offset + 7], f[offset + 8], 0f,
        0f, 0f, 0f, 1f
    )

    fun setRows3x3(f: FloatArray, offset: Int) = setRows(
        f[offset + 0], f[offset + 1], f[offset + 2], 0f,
        f[offset + 3], f[offset + 4], f[offset + 5], 0f,
        f[offset + 6], f[offset + 7], f[offset + 8], 0f,
        0f, 0f, 0f, 1f
    )

    fun setColumns2x2(f: FloatArray, offset: Int) = setColumns(
        f[offset + 0], f[offset + 1], 0f, 0f,
        f[offset + 1], f[offset + 2], 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    fun setRows2x2(f: FloatArray, offset: Int) = setRows(
        f[offset + 0], f[offset + 1], 0f, 0f,
        f[offset + 1], f[offset + 2], 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    fun setRow(row: Int, a: Float, b: Float, c: Float, d: Float): Matrix3D {
        data[columnMajorIndex(row, 0)] = a
        data[columnMajorIndex(row, 1)] = b
        data[columnMajorIndex(row, 2)] = c
        data[columnMajorIndex(row, 3)] = d
        return this
    }
    fun setRow(row: Int, a: Double, b: Double, c: Double, d: Double): Matrix3D = setRow(row, a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat())
    fun setRow(row: Int, a: Int, b: Int, c: Int, d: Int): Matrix3D = setRow(row, a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat())
    fun setRow(row: Int, data: FloatArray): Matrix3D = setRow(row, data[0], data[1], data[2], data[3])
    fun setRow(row: Int, data: Vector3D): Matrix3D = setRow(row, data.x, data.y, data.w, data.z)

    fun setColumn(column: Int, a: Float, b: Float, c: Float, d: Float): Matrix3D {
        data[columnMajorIndex(0, column)] = a
        data[columnMajorIndex(1, column)] = b
        data[columnMajorIndex(2, column)] = c
        data[columnMajorIndex(3, column)] = d
        return this
    }
    fun setColumn(column: Int, a: Double, b: Double, c: Double, d: Double): Matrix3D = setColumn(column, a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat())
    fun setColumn(column: Int, a: Int, b: Int, c: Int, d: Int): Matrix3D = setColumn(column, a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat())
    fun setColumn(column: Int, data: FloatArray): Matrix3D = setColumn(column, data[0], data[1], data[2], data[3])
    fun setColumn(column: Int, data: Vector3D): Matrix3D = setColumn(column, data.x, data.y, data.w, data.z)

    fun getRow(n: Int, target: FloatArray = FloatArray(4)): FloatArray {
        val m = n * 4
        target[0] = data[m + 0]
        target[1] = data[m + 1]
        target[2] = data[m + 2]
        target[3] = data[m + 3]
        return target
    }

    fun getColumn(n: Int, target: FloatArray = FloatArray(4)): FloatArray {
        target[0] = data[n + 0]
        target[1] = data[n + 4]
        target[2] = data[n + 8]
        target[3] = data[n + 12]
        return target
    }

    val determinant: Float get() = 0f +
        (v30 * v21 * v12 * v03) -
        (v20 * v31 * v12 * v03) -
        (v30 * v11 * v22 * v03) +
        (v10 * v31 * v22 * v03) +
        (v20 * v11 * v32 * v03) -
        (v10 * v21 * v32 * v03) -
        (v30 * v21 * v02 * v13) +
        (v20 * v31 * v02 * v13) +
        (v30 * v01 * v22 * v13) -
        (v00 * v31 * v22 * v13) -
        (v20 * v01 * v32 * v13) +
        (v00 * v21 * v32 * v13) +
        (v30 * v11 * v02 * v23) -
        (v10 * v31 * v02 * v23) -
        (v30 * v01 * v12 * v23) +
        (v00 * v31 * v12 * v23) +
        (v10 * v01 * v32 * v23) -
        (v00 * v11 * v32 * v23) -
        (v20 * v11 * v02 * v33) +
        (v10 * v21 * v02 * v33) +
        (v20 * v01 * v12 * v33) -
        (v00 * v21 * v12 * v33) -
        (v10 * v01 * v22 * v33) +
        (v00 * v11 * v22 * v33)

    val determinant3x3: Float get() = 0f +
        (v00 * v11 * v22) +
        (v01 * v12 * v20) +
        (v02 * v10 * v21) -
        (v00 * v12 * v21) -
        (v01 * v10 * v22) -
        (v02 * v11 * v20)

    fun identity() = this.setColumns(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    fun setToTranslation(x: Float, y: Float, z: Float, w: Float = 1f): Matrix3D = this.setRows(
        1f, 0f, 0f, x,
        0f, 1f, 0f, y,
        0f, 0f, 1f, z,
        0f, 0f, 0f, w
    )
    fun setToTranslation(x: Double, y: Double, z: Double, w: Double = 1.0) = setToTranslation(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    fun setToTranslation(x: Int, y: Int, z: Int, w: Int = 1) = setToTranslation(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    fun setToScale(x: Float, y: Float, z: Float, w: Float = 1f): Matrix3D = this.setRows(
        x, 0f, 0f, 0f,
        0f, y, 0f, 0f,
        0f, 0f, z, 0f,
        0f, 0f, 0f, w
    )
    fun setToScale(x: Double, y: Double, z: Double, w: Double = 1.0) = setToScale(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    fun setToScale(x: Int, y: Int, z: Int, w: Int = 1) = setToScale(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    fun setToShear(x: Float, y: Float, z: Float): Matrix3D = this.setRows(
        1f, y, z, 0f,
        x, 1f, z, 0f,
        x, y, 1f, 0f,
        0f, 0f, 0f, 1f
    )
    fun setToShear(x: Double, y: Double, z: Double) = setToShear(x.toFloat(), y.toFloat(), z.toFloat())
    fun setToShear(x: Int, y: Int, z: Int) = setToShear(x.toFloat(), y.toFloat(), z.toFloat())

    fun setToRotationX(angle: Angle): Matrix3D {
        val c = cos(angle).toFloat()
        val s = sin(angle).toFloat()
        return this.setRows(
            1f, 0f, 0f, 0f,
            0f, c, - s, 0f,
            0f, s, c, 0f,
            0f, 0f, 0f, 1f
        )
    }

    fun setToRotationY(angle: Angle): Matrix3D {
        val c = cos(angle).toFloat()
        val s = sin(angle).toFloat()
        return this.setRows(
            c, 0f, s, 0f,
            0f, 1f, 0f, 0f,
            - s, 0f, c, 0f,
            0f, 0f, 0f, 1f
        )
    }

    fun setToRotationZ(angle: Angle): Matrix3D {
        val c = cos(angle).toFloat()
        val s = sin(angle).toFloat()
        return this.setRows(
            c, - s, 0f, 0f,
            s, c, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )
    }

    fun setToRotation(angle: Angle, x: Float, y: Float, z: Float): Matrix3D {
        val mag = sqrt(x * x + y * y + z * z)
        val norm = 1.0 / mag

        val nx = x * norm
        val ny = y * norm
        val nz = z * norm
        val c = cos(angle)
        val s = sin(angle)
        val t = 1 - c
        val tx = t * nx
        val ty = t * ny

        return this.setRows(
            tx * nx + c, tx * ny - s * nz, tx * nz + s * ny, 0.0,
            tx * ny + s * nz, ty * ny + c, ty * nz - s * nx, 0.0,
            tx * nz - s * ny, ty * nz + s * nx, t * nz * nz + c, 0.0,
            0.0, 0.0, 0.0, 1.0
        )
    }
    fun setToRotation(angle: Angle, direction: Vector3D): Matrix3D = setToRotation(angle, direction.x, direction.y, direction.z)
    fun setToRotation(angle: Angle, x: Double, y: Double, z: Double): Matrix3D = setToRotation(angle, x.toFloat(), y.toFloat(), z.toFloat())
    fun setToRotation(angle: Angle, x: Int, y: Int, z: Int): Matrix3D = setToRotation(angle, x.toFloat(), y.toFloat(), z.toFloat())

    fun multiply(l: Matrix3D, r: Matrix3D) = this.setRows(
        (l.v00 * r.v00) + (l.v01 * r.v10) + (l.v02 * r.v20) + (l.v03 * r.v30),
        (l.v00 * r.v01) + (l.v01 * r.v11) + (l.v02 * r.v21) + (l.v03 * r.v31),
        (l.v00 * r.v02) + (l.v01 * r.v12) + (l.v02 * r.v22) + (l.v03 * r.v32),
        (l.v00 * r.v03) + (l.v01 * r.v13) + (l.v02 * r.v23) + (l.v03 * r.v33),

        (l.v10 * r.v00) + (l.v11 * r.v10) + (l.v12 * r.v20) + (l.v13 * r.v30),
        (l.v10 * r.v01) + (l.v11 * r.v11) + (l.v12 * r.v21) + (l.v13 * r.v31),
        (l.v10 * r.v02) + (l.v11 * r.v12) + (l.v12 * r.v22) + (l.v13 * r.v32),
        (l.v10 * r.v03) + (l.v11 * r.v13) + (l.v12 * r.v23) + (l.v13 * r.v33),

        (l.v20 * r.v00) + (l.v21 * r.v10) + (l.v22 * r.v20) + (l.v23 * r.v30),
        (l.v20 * r.v01) + (l.v21 * r.v11) + (l.v22 * r.v21) + (l.v23 * r.v31),
        (l.v20 * r.v02) + (l.v21 * r.v12) + (l.v22 * r.v22) + (l.v23 * r.v32),
        (l.v20 * r.v03) + (l.v21 * r.v13) + (l.v22 * r.v23) + (l.v23 * r.v33),

        (l.v30 * r.v00) + (l.v31 * r.v10) + (l.v32 * r.v20) + (l.v33 * r.v30),
        (l.v30 * r.v01) + (l.v31 * r.v11) + (l.v32 * r.v21) + (l.v33 * r.v31),
        (l.v30 * r.v02) + (l.v31 * r.v12) + (l.v32 * r.v22) + (l.v33 * r.v32),
        (l.v30 * r.v03) + (l.v31 * r.v13) + (l.v32 * r.v23) + (l.v33 * r.v33)
    )

    fun multiply(
        lv00: Float, lv01: Float, lv02: Float, lv03: Float,
        lv10: Float, lv11: Float, lv12: Float, lv13: Float,
        lv20: Float, lv21: Float, lv22: Float, lv23: Float,
        lv30: Float, lv31: Float, lv32: Float, lv33: Float,

        rv00: Float, rv01: Float, rv02: Float, rv03: Float,
        rv10: Float, rv11: Float, rv12: Float, rv13: Float,
        rv20: Float, rv21: Float, rv22: Float, rv23: Float,
        rv30: Float, rv31: Float, rv32: Float, rv33: Float,
    ) = this.setRows(
        (lv00 * rv00) + (lv01 * rv10) + (lv02 * rv20) + (lv03 * rv30),
        (lv00 * rv01) + (lv01 * rv11) + (lv02 * rv21) + (lv03 * rv31),
        (lv00 * rv02) + (lv01 * rv12) + (lv02 * rv22) + (lv03 * rv32),
        (lv00 * rv03) + (lv01 * rv13) + (lv02 * rv23) + (lv03 * rv33),

        (lv10 * rv00) + (lv11 * rv10) + (lv12 * rv20) + (lv13 * rv30),
        (lv10 * rv01) + (lv11 * rv11) + (lv12 * rv21) + (lv13 * rv31),
        (lv10 * rv02) + (lv11 * rv12) + (lv12 * rv22) + (lv13 * rv32),
        (lv10 * rv03) + (lv11 * rv13) + (lv12 * rv23) + (lv13 * rv33),

        (lv20 * rv00) + (lv21 * rv10) + (lv22 * rv20) + (lv23 * rv30),
        (lv20 * rv01) + (lv21 * rv11) + (lv22 * rv21) + (lv23 * rv31),
        (lv20 * rv02) + (lv21 * rv12) + (lv22 * rv22) + (lv23 * rv32),
        (lv20 * rv03) + (lv21 * rv13) + (lv22 * rv23) + (lv23 * rv33),

        (lv30 * rv00) + (lv31 * rv10) + (lv32 * rv20) + (lv33 * rv30),
        (lv30 * rv01) + (lv31 * rv11) + (lv32 * rv21) + (lv33 * rv31),
        (lv30 * rv02) + (lv31 * rv12) + (lv32 * rv22) + (lv33 * rv32),
        (lv30 * rv03) + (lv31 * rv13) + (lv32 * rv23) + (lv33 * rv33)
    )

    fun multiply(
        lv00: Double, lv01: Double, lv02: Double, lv03: Double,
        lv10: Double, lv11: Double, lv12: Double, lv13: Double,
        lv20: Double, lv21: Double, lv22: Double, lv23: Double,
        lv30: Double, lv31: Double, lv32: Double, lv33: Double,

        rv00: Double, rv01: Double, rv02: Double, rv03: Double,
        rv10: Double, rv11: Double, rv12: Double, rv13: Double,
        rv20: Double, rv21: Double, rv22: Double, rv23: Double,
        rv30: Double, rv31: Double, rv32: Double, rv33: Double,
    ) = multiply(
        lv00.toFloat(), lv01.toFloat(), lv02.toFloat(), lv03.toFloat(),
        lv10.toFloat(), lv11.toFloat(), lv12.toFloat(), lv13.toFloat(),
        lv20.toFloat(), lv21.toFloat(), lv22.toFloat(), lv23.toFloat(),
        lv30.toFloat(), lv31.toFloat(), lv32.toFloat(), lv33.toFloat(),
        rv00.toFloat(), rv01.toFloat(), rv02.toFloat(), rv03.toFloat(),
        rv10.toFloat(), rv11.toFloat(), rv12.toFloat(), rv13.toFloat(),
        rv20.toFloat(), rv21.toFloat(), rv22.toFloat(), rv23.toFloat(),
        rv30.toFloat(), rv31.toFloat(), rv32.toFloat(), rv33.toFloat(),
    )

    fun multiply(scale: Float, l: Matrix3D = this) = this.apply {
        for (n in 0 until 16) this.data[n] = l.data[n] * scale
    }

    fun copyFrom(that: Matrix3D): Matrix3D {
        for (n in 0 until 16) this.data[n] = that.data[n]
        return this
    }

    fun transform0(x: Float, y: Float, z: Float, w: Float = 1f): Float = (v00 * x) + (v01 * y) + (v02 * z) + (v03 * w)
    fun transform1(x: Float, y: Float, z: Float, w: Float = 1f): Float = (v10 * x) + (v11 * y) + (v12 * z) + (v13 * w)
    fun transform2(x: Float, y: Float, z: Float, w: Float = 1f): Float = (v20 * x) + (v21 * y) + (v22 * z) + (v23 * w)
    fun transform3(x: Float, y: Float, z: Float, w: Float = 1f): Float = (v30 * x) + (v31 * y) + (v32 * z) + (v33 * w)

    fun transform(x: Float, y: Float, z: Float, w: Float = 1f, out: Vector3D = Vector3D(0, 0, 0, 0)): Vector3D = out.setTo(
        transform0(x, y, z, w),
        transform1(x, y, z, w),
        transform2(x, y, z, w),
        transform3(x, y, z, w)
    )

    fun transform(v: Vector3D, out: Vector3D = Vector3D()): Vector3D = transform(v.x, v.y, v.z, v.w, out)

    fun setToOrtho(left: Float, right: Float, bottom: Float, top: Float, near: Float = 0f, far: Float = 1f): Matrix3D {
        val sx = 2f / (right - left)
        val sy = 2f / (top - bottom)
        val sz = -2f / (far - near)

        val tx = -(right + left) / (right - left)
        val ty = -(top + bottom) / (top - bottom)
        val tz = -(far + near) / (far - near)

        return setRows(
            sx, 0f, 0f, tx,
            0f, sy, 0f, ty,
            0f, 0f, sz, tz,
            0f, 0f, 0f, 1f
        )
    }

    fun setToOrtho(rect: Rectangle, near: Double = 0.0, far: Double = 1.0): Matrix3D = setToOrtho(rect.left, rect.right, rect.bottom, rect.top, near, far)
    fun setToOrtho(rect: Rectangle, near: Float = 0f, far: Float = 1f): Matrix3D = setToOrtho(rect.left, rect.right, rect.bottom, rect.top, near.toDouble(), far.toDouble())
    fun setToOrtho(rect: Rectangle, near: Int = 0, far: Int = 1): Matrix3D = setToOrtho(rect.left, rect.right, rect.bottom, rect.top, near.toDouble(), far.toDouble())
    fun setToOrtho(left: Double, right: Double, bottom: Double, top: Double, near: Double, far: Double): Matrix3D =
        setToOrtho(left.toFloat(), right.toFloat(), bottom.toFloat(), top.toFloat(), near.toFloat(), far.toFloat())
    fun setToOrtho(left: Int, right: Int, bottom: Int, top: Int, near: Int, far: Int): Matrix3D =
        setToOrtho(left.toFloat(), right.toFloat(), bottom.toFloat(), top.toFloat(), near.toFloat(), far.toFloat())

    fun setToFrustum(left: Float, right: Float, bottom: Float, top: Float, zNear: Float = 0f, zFar: Float = 1f): Matrix3D {
        if (zNear <= 0.0f || zFar <= zNear) {
            throw Exception("Error: Required zNear > 0 and zFar > zNear, but zNear $zNear, zFar $zFar")
        }
        if (left == right || top == bottom) {
            throw Exception("Error: top,bottom and left,right must not be equal")
        }

        val zNear2 = 2.0f * zNear
        val dx = right - left
        val dy = top - bottom
        val dz = zFar - zNear
        val A = (right + left) / dx
        val B = (top + bottom) / dy
        val C = -1.0f * (zFar + zNear) / dz
        val D = -2.0f * (zFar * zNear) / dz

        return setRows(
            zNear2 / dx, 0f, A, 0f,
            0f, zNear2 / dy, B, 0f,
            0f, 0f, C, D,
            0f, 0f, -1f, 0f
        )
    }
    fun setToFrustum(rect: Rectangle, zNear: Double = 0.0, zFar: Double = 1.0): Matrix3D = setToFrustum(rect.left, rect.right, rect.bottom, rect.top, zNear.toDouble(), zFar.toDouble())
    fun setToFrustum(rect: Rectangle, zNear: Float = 0f, zFar: Float = 1f): Matrix3D = setToFrustum(rect.left, rect.right, rect.bottom, rect.top, zNear.toDouble(), zFar.toDouble())
    fun setToFrustum(rect: Rectangle, zNear: Int = 0, zFar: Int = 1): Matrix3D = setToFrustum(rect.left, rect.right, rect.bottom, rect.top, zNear.toDouble(), zFar.toDouble())

    fun setToFrustum(left: Double, right: Double, bottom: Double, top: Double, zNear: Double = 0.0, zFar: Double = 1.0): Matrix3D
        = setToFrustum(left.toFloat(), right.toFloat(), bottom.toFloat(), top.toFloat(), zNear.toFloat(), zFar.toFloat())
    fun setToFrustum(left: Int, right: Int, bottom: Int, top: Int, zNear: Int = 0, zFar: Int = 1): Matrix3D
        = setToFrustum(left.toFloat(), right.toFloat(), bottom.toFloat(), top.toFloat(), zNear.toFloat(), zFar.toFloat())


    fun setToPerspective(fovy: Angle, aspect: Float, zNear: Float, zFar: Float): Matrix3D {
        val top = tan(fovy.radians / 2f) * zNear
        val bottom = -1.0f * top
        val left = aspect * bottom
        val right = aspect * top
        return setToFrustum(left.toFloat(), right.toFloat(), bottom.toFloat(), top.toFloat(), zNear, zFar)
    }
    fun setToPerspective(fovy: Angle, aspect: Double, zNear: Double, zFar: Double): Matrix3D
        = setToPerspective(fovy, aspect.toFloat(), zNear.toFloat(), zFar.toFloat())


    override fun equals(other: Any?): Boolean = (other is Matrix3D) && this.data.contentEquals(other.data)
    override fun hashCode(): Int = data.contentHashCode()

    override fun toString(): String = buildString {
        append("Matrix3D(\n")
        for (row in 0 until 4) {
            append("  [ ")
            for (col in 0 until 4) {
                if (col != 0) append(", ")
                val v = get(row, col)
                if (floor(v) == v) append(v.toInt()) else append(v)
            }
            append(" ],\n")
        }
        append(")")
    }

    fun clone(): Matrix3D = Matrix3D().copyFrom(this)
}

fun Matrix3D.copyToFloatWxH(out: FloatArray, rows: Int, columns: Int, order: MajorOrder) {
    copyToFloatWxH(out, rows, columns, order, 0)
}

fun Matrix3D.copyToFloatWxH(out: FloatArray, rows: Int, columns: Int, order: MajorOrder, offset: Int) {
    var n = offset
    if (order == MajorOrder.ROW) {
        for (column in 0 until columns) for (row in 0 until rows) out[n++] = data[Matrix3D.rowMajorIndex(row, column)]
    } else {
        for (column in 0 until columns) for (row in 0 until rows) out[n++] = data[Matrix3D.columnMajorIndex(row, column)]
    }
}

fun Matrix3D.copyToFloat2x2(out: FloatArray, order: MajorOrder) = copyToFloatWxH(out, 2, 2, order, 0)
fun Matrix3D.copyToFloat3x3(out: FloatArray, order: MajorOrder) = copyToFloatWxH(out, 3, 3, order, 0)
fun Matrix3D.copyToFloat4x4(out: FloatArray, order: MajorOrder) = copyToFloatWxH(out, 4, 4, order, 0)

fun Matrix3D.copyToFloat2x2(out: FloatArray, order: MajorOrder, offset: Int) = copyToFloatWxH(out, 2, 2, order, offset)
fun Matrix3D.copyToFloat3x3(out: FloatArray, order: MajorOrder, offset: Int) = copyToFloatWxH(out, 3, 3, order, offset)
fun Matrix3D.copyToFloat4x4(out: FloatArray, order: MajorOrder, offset: Int) = copyToFloatWxH(out, 4, 4, order, offset)

fun Matrix3D.setRows(
    a00: Double, a01: Double, a02: Double, a03: Double,
    a10: Double, a11: Double, a12: Double, a13: Double,
    a20: Double, a21: Double, a22: Double, a23: Double,
    a30: Double, a31: Double, a32: Double, a33: Double
): Matrix3D = setRows(
    a00.toFloat(), a01.toFloat(), a02.toFloat(), a03.toFloat(),
    a10.toFloat(), a11.toFloat(), a12.toFloat(), a13.toFloat(),
    a20.toFloat(), a21.toFloat(), a22.toFloat(), a23.toFloat(),
    a30.toFloat(), a31.toFloat(), a32.toFloat(), a33.toFloat()
)
fun Matrix3D.setRows(
    a00: Float, a01: Float, a02: Float, a03: Float,
    a10: Float, a11: Float, a12: Float, a13: Float,
    a20: Float, a21: Float, a22: Float, a23: Float,
    a30: Float, a31: Float, a32: Float, a33: Float
): Matrix3D = setRows(
    a00, a01, a02, a03,
    a10, a11, a12, a13,
    a20, a21, a22, a23,
    a30, a31, a32, a33
)

fun Matrix3D.setColumns(
    a00: Double, a10: Double, a20: Double, a30: Double,
    a01: Double, a11: Double, a21: Double, a31: Double,
    a02: Double, a12: Double, a22: Double, a32: Double,
    a03: Double, a13: Double, a23: Double, a33: Double
): Matrix3D = setColumns(
    a00.toFloat(), a10.toFloat(), a20.toFloat(), a30.toFloat(),
    a01.toFloat(), a11.toFloat(), a21.toFloat(), a31.toFloat(),
    a02.toFloat(), a12.toFloat(), a22.toFloat(), a32.toFloat(),
    a03.toFloat(), a13.toFloat(), a23.toFloat(), a33.toFloat()
)
fun Matrix3D.setColumns(
    a00: Float, a10: Float, a20: Float, a30: Float,
    a01: Float, a11: Float, a21: Float, a31: Float,
    a02: Float, a12: Float, a22: Float, a32: Float,
    a03: Float, a13: Float, a23: Float, a33: Float
): Matrix3D = setColumns(
    a00, a10, a20, a30,
    a01, a11, a21, a31,
    a02, a12, a22, a32,
    a03, a13, a23, a33
)

fun Matrix3D.setRows3x3(
    a00: Double, a01: Double, a02: Double,
    a10: Double, a11: Double, a12: Double,
    a20: Double, a21: Double, a22: Double
): Matrix3D = setRows(
    a00.toFloat(), a01.toFloat(), a02.toFloat(), 0f,
    a10.toFloat(), a11.toFloat(), a12.toFloat(), 0f,
    a20.toFloat(), a21.toFloat(), a22.toFloat(), 0f,
    0f, 0f, 0f, 1f
)
fun Matrix3D.setRows3x3(
    a00: Float, a01: Float, a02: Float,
    a10: Float, a11: Float, a12: Float,
    a20: Float, a21: Float, a22: Float
): Matrix3D = setRows(
    a00, a01, a02, 0f,
    a10, a11, a12, 0f,
    a20, a21, a22, 0f,
    0f, 0f, 0f, 1f
)

fun Matrix3D.setColumns3x3(
    a00: Double, a10: Double, a20: Double,
    a01: Double, a11: Double, a21: Double,
    a02: Double, a12: Double, a22: Double
): Matrix3D = setColumns(
    a00.toFloat(), a10.toFloat(), a20.toFloat(), 0f,
    a01.toFloat(), a11.toFloat(), a21.toFloat(), 0f,
    a02.toFloat(), a12.toFloat(), a22.toFloat(), 0f,
    0f, 0f, 0f, 1f
)
fun Matrix3D.setColumns3x3(
    a00: Float, a10: Float, a20: Float,
    a01: Float, a11: Float, a21: Float,
    a02: Float, a12: Float, a22: Float
): Matrix3D = setColumns(
    a00, a10, a20, 0f,
    a01, a11, a21, 0f,
    a02, a12, a22, 0f,
    0f, 0f, 0f, 1f
)

fun Matrix3D.setRows2x2(
    a00: Double, a01: Double,
    a10: Double, a11: Double
): Matrix3D = setRows(
    a00.toFloat(), a01.toFloat(), 0f, 0f,
    a10.toFloat(), a11.toFloat(), 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f
)
fun Matrix3D.setRows2x2(
    a00: Float, a01: Float,
    a10: Float, a11: Float
): Matrix3D = setRows(
    a00, a01, 0f, 0f,
    a10, a11, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f
)

fun Matrix3D.Companion.fromRows(
    a00: Double, a01: Double, a02: Double, a03: Double,
    a10: Double, a11: Double, a12: Double, a13: Double,
    a20: Double, a21: Double, a22: Double, a23: Double,
    a30: Double, a31: Double, a32: Double, a33: Double
): Matrix3D = Matrix3D().setRows(
    a00.toFloat(), a01.toFloat(), a02.toFloat(), a03.toFloat(),
    a10.toFloat(), a11.toFloat(), a12.toFloat(), a13.toFloat(),
    a20.toFloat(), a21.toFloat(), a22.toFloat(), a23.toFloat(),
    a30.toFloat(), a31.toFloat(), a32.toFloat(), a33.toFloat()
)
fun Matrix3D.Companion.fromRows(
    a00: Float, a01: Float, a02: Float, a03: Float,
    a10: Float, a11: Float, a12: Float, a13: Float,
    a20: Float, a21: Float, a22: Float, a23: Float,
    a30: Float, a31: Float, a32: Float, a33: Float
): Matrix3D = Matrix3D().setRows(
    a00, a01, a02, a03,
    a10, a11, a12, a13,
    a20, a21, a22, a23,
    a30, a31, a32, a33
)

fun Matrix3D.Companion.fromColumns(
    a00: Double, a10: Double, a20: Double, a30: Double,
    a01: Double, a11: Double, a21: Double, a31: Double,
    a02: Double, a12: Double, a22: Double, a32: Double,
    a03: Double, a13: Double, a23: Double, a33: Double
): Matrix3D = Matrix3D().setColumns(
    a00.toFloat(), a10.toFloat(), a20.toFloat(), a30.toFloat(),
    a01.toFloat(), a11.toFloat(), a21.toFloat(), a31.toFloat(),
    a02.toFloat(), a12.toFloat(), a22.toFloat(), a32.toFloat(),
    a03.toFloat(), a13.toFloat(), a23.toFloat(), a33.toFloat()
)
fun Matrix3D.Companion.fromColumns(
    a00: Float, a10: Float, a20: Float, a30: Float,
    a01: Float, a11: Float, a21: Float, a31: Float,
    a02: Float, a12: Float, a22: Float, a32: Float,
    a03: Float, a13: Float, a23: Float, a33: Float
): Matrix3D = Matrix3D().setColumns(
    a00, a10, a20, a30,
    a01, a11, a21, a31,
    a02, a12, a22, a32,
    a03, a13, a23, a33
)

fun Matrix3D.setColumns2x2(
    a00: Double, a10: Double,
    a01: Double, a11: Double
): Matrix3D = setColumns(
    a00.toFloat(), a10.toFloat(), 0f, 0f,
    a01.toFloat(), a11.toFloat(), 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f
)
fun Matrix3D.setColumns2x2(
    a00: Float, a10: Float,
    a01: Float, a11: Float
): Matrix3D = setColumns(
    a00, a10, 0f, 0f,
    a01, a11, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f
)

fun Matrix3D.Companion.fromRows3x3(
    a00: Double, a01: Double, a02: Double,
    a10: Double, a11: Double, a12: Double,
    a20: Double, a21: Double, a22: Double
): Matrix3D = Matrix3D().setRows3x3(
    a00.toFloat(), a01.toFloat(), a02.toFloat(),
    a10.toFloat(), a11.toFloat(), a12.toFloat(),
    a20.toFloat(), a21.toFloat(), a22.toFloat()
)
fun Matrix3D.Companion.fromRows3x3(
    a00: Float, a01: Float, a02: Float,
    a10: Float, a11: Float, a12: Float,
    a20: Float, a21: Float, a22: Float
): Matrix3D = Matrix3D().setRows3x3(
    a00, a01, a02,
    a10, a11, a12,
    a20, a21, a22
)

fun Matrix3D.Companion.fromColumns3x3(
    a00: Double, a10: Double, a20: Double,
    a01: Double, a11: Double, a21: Double,
    a02: Double, a12: Double, a22: Double
): Matrix3D = Matrix3D().setColumns3x3(
    a00.toFloat(), a10.toFloat(), a20.toFloat(),
    a01.toFloat(), a11.toFloat(), a21.toFloat(),
    a02.toFloat(), a12.toFloat(), a22.toFloat()
)
fun Matrix3D.Companion.fromColumns3x3(
    a00: Float, a10: Float, a20: Float,
    a01: Float, a11: Float, a21: Float,
    a02: Float, a12: Float, a22: Float
): Matrix3D = Matrix3D().setColumns3x3(
    a00, a10, a20,
    a01, a11, a21,
    a02, a12, a22
)

fun Matrix3D.Companion.fromRows2x2(
    a00: Double, a01: Double,
    a10: Double, a11: Double
): Matrix3D = Matrix3D().setRows2x2(
    a00.toFloat(), a01.toFloat(),
    a10.toFloat(), a11.toFloat()
)
fun Matrix3D.Companion.fromRows2x2(
    a00: Float, a01: Float,
    a10: Float, a11: Float
): Matrix3D = Matrix3D().setRows2x2(
    a00, a01,
    a10, a11
)

fun Matrix3D.Companion.fromColumns2x2(
    a00: Double, a10: Double,
    a01: Double, a11: Double
): Matrix3D = Matrix3D().setColumns2x2(
    a00.toFloat(), a10.toFloat(),
    a01.toFloat(), a11.toFloat()
)
fun Matrix3D.Companion.fromColumns2x2(
    a00: Float, a10: Float,
    a01: Float, a11: Float
): Matrix3D = Matrix3D().setColumns2x2(
    a00, a10,
    a01, a11
)

operator fun Matrix3D.times(that: Matrix3D): Matrix3D = Matrix3D().multiply(this, that)
operator fun Matrix3D.times(value: Float): Matrix3D = Matrix3D(this).multiply(value)
operator fun Matrix3D.times(value: Double): Matrix3D = this * value.toFloat()
operator fun Matrix3D.times(value: Int): Matrix3D = this * value.toFloat()

operator fun Matrix3D.div(value: Float): Matrix3D = this * (1f / value)
operator fun Matrix3D.div(value: Double): Matrix3D = this / value.toFloat()
operator fun Matrix3D.div(value: Int): Matrix3D = this / value.toFloat()

fun Matrix3D.multiply(scale: Double, l: Matrix3D = this) = multiply(scale.toFloat(), l)
fun Matrix3D.multiply(scale: Int, l: Matrix3D = this) = multiply(scale.toFloat(), l)


@PublishedApi
@ThreadLocal
internal val tempMat3D = Matrix3D()

fun Matrix3D.translate(x: Float, y: Float, z: Float, w: Float = 1f, temp: Matrix3D = tempMat3D) = this.apply {
    temp.setToTranslation(x, y, z, w)
    this.multiply(this, temp)
}
fun Matrix3D.translate(x: Double, y: Double, z: Double, w: Double = 1.0, temp: Matrix3D = tempMat3D) = this.translate(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat(), temp)
fun Matrix3D.translate(x: Int, y: Int, z: Int, w: Int = 1, temp: Matrix3D = tempMat3D) = this.translate(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat(), temp)

fun Matrix3D.rotate(angle: Angle, x: Float, y: Float, z: Float, temp: Matrix3D = tempMat3D) = this.apply {
    temp.setToRotation(angle, x, y, z)
    this.multiply(this, temp)
}
fun Matrix3D.rotate(angle: Angle, x: Double, y: Double, z: Double, temp: Matrix3D = tempMat3D) = this.rotate(angle, x.toFloat(), y.toFloat(), z.toFloat(), temp)
fun Matrix3D.rotate(angle: Angle, x: Int, y: Int, z: Int, temp: Matrix3D = tempMat3D) = this.rotate(angle, x.toFloat(), y.toFloat(), z.toFloat(), temp)

fun Matrix3D.scale(x: Float, y: Float, z: Float, w: Float = 1f, temp: Matrix3D = tempMat3D) = this.apply {
    temp.setToScale(x, y, z, w)
    this.multiply(this, temp)
}

fun Matrix3D.scale(x: Double, y: Double, z: Double, w: Double = 1.0, temp: Matrix3D = tempMat3D) = this.scale(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat(), temp)
fun Matrix3D.scale(x: Int, y: Int, z: Int, w: Int = 1, temp: Matrix3D = tempMat3D) = this.scale(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat(), temp)

fun Matrix3D.setToRotation(quat: Quaternion, temp: Matrix3D = tempMat3D) = this.apply {
    quat.toMatrix(temp)
    this.multiply(this, temp)
}
fun Matrix3D.setToRotation(euler: EulerRotation, temp: Matrix3D = tempMat3D) = this.apply {
    euler.toMatrix(temp)
    this.multiply(this, temp)
}
fun Matrix3D.rotate(x: Angle, y: Angle, z: Angle, temp: Matrix3D = tempMat3D) = this.apply {
    rotate(x, 1f, 0f, 0f)
    rotate(y, 0f, 1f, 0f)
    rotate(z, 0f, 0f, 1f)
}
fun Matrix3D.rotate(euler: EulerRotation, temp: Matrix3D = tempMat3D) = this.apply {
    temp.setToRotation(euler)
    this.multiply(this, temp)
}
fun Matrix3D.rotate(quat: Quaternion, temp: Matrix3D = tempMat3D) = this.apply {
    temp.setToRotation(quat)
    this.multiply(this, temp)
}

private val tempVec1 = Vector3D()
private val tempVec2 = Vector3D()
private val tempVec3 = Vector3D()

fun Matrix3D.setToLookAt(
    eye: Vector3D,
    target: Vector3D,
    up: Vector3D
): Matrix3D {
    val z = tempVec1.sub(eye, target)
    if (z.length3Squared == 0f) z.z = 1f
    z.normalize()
    val x = tempVec2.cross(up, z)
    if (x.length3Squared == 0f) {
        when {
            abs(up.z) == 1f -> z.x += 0.0001f
            else -> z.z += 0.0001f
        }
        z.normalize()
        x.cross(up, z)
    }
    x.normalize()
    val y = tempVec3.cross(z, x)
    return this.setRows(
        x.x, y.x, z.x, 0f,
        x.y, y.y, z.y, 0f,
        x.z, y.z, z.z, 0f,
        0f, 0f, 0f, 1f
    )
}

inline fun Matrix3D.translate(v: Vector3D, temp: Matrix3D = tempMat3D) = translate(v.x, v.y, v.z, v.w, temp)
inline fun Matrix3D.rotate(angle: Angle, v: Vector3D, temp: Matrix3D = tempMat3D) = rotate(angle, v.x, v.y, v.z, temp)
inline fun Matrix3D.scale(v: Vector3D, temp: Matrix3D = tempMat3D) = scale(v.x, v.y, v.z, v.w, temp)

fun Matrix3D.setTRS(translation: Position3D, rotation: Quaternion, scale: Scale3D): Matrix3D {
    val rx = rotation.x.toFloat()
    val ry = rotation.y.toFloat()
    val rz = rotation.z.toFloat()
    val rw = rotation.w.toFloat()

    val xt = rx + rx
    val yt = ry + ry
    val zt = rz + rz

    val xx = rx * xt
    val xy = rx * yt
    val xz = rx * zt

    val yy = ry * yt
    val yz = ry * zt
    val zz = rz * zt

    val wx = rw * xt
    val wy = rw * yt
    val wz = rw * zt

    return setRows(
        ((1 - (yy + zz)) * scale.x), ((xy - wz) * scale.y), ((xz + wy) * scale.z), translation.x,
        ((xy + wz) * scale.x), ((1 - (xx + zz)) * scale.y), ((yz - wx) * scale.z), translation.y,
        ((xz - wy) * scale.x), ((yz + wx) * scale.y), ((1 - (xx + yy)) * scale.z), translation.z,
        0f, 0f, 0f, 1f
    )
}

private val tempMat1 = Matrix3D()

fun Matrix3D.getTRS(position: Position3D, rotation: Quaternion, scale: Scale3D): Matrix3D = this.apply {
    val det = determinant
    position.setTo(v03, v13, v23, 1f)
    scale.setTo(Vector3D.length(v00, v10, v20) * det.sign, Vector3D.length(v01, v11, v21), Vector3D.length(v02, v12, v22), 1f)
    val invSX = 1f / scale.x
    val invSY = 1f / scale.y
    val invSZ = 1f / scale.z
    rotation.setFromRotationMatrix(tempMat1.setRows(
        v00 * invSX, v01 * invSY, v02 * invSZ, v03,
        v10 * invSX, v11 * invSY, v12 * invSZ, v13,
        v20 * invSX, v21 * invSY, v22 * invSZ, v23,
        v30, v31, v32, v33
    ))
}

fun Matrix3D.invert(m: Matrix3D = this): Matrix3D {
    val target = this
    m.apply {
        val t11 = v12 * v23 * v31 - v13 * v22 * v31 + v13 * v21 * v32 - v11 * v23 * v32 - v12 * v21 * v33 + v11 * v22 * v33
        val t12 = v03 * v22 * v31 - v02 * v23 * v31 - v03 * v21 * v32 + v01 * v23 * v32 + v02 * v21 * v33 - v01 * v22 * v33
        val t13 = v02 * v13 * v31 - v03 * v12 * v31 + v03 * v11 * v32 - v01 * v13 * v32 - v02 * v11 * v33 + v01 * v12 * v33
        val t14 = v03 * v12 * v21 - v02 * v13 * v21 - v03 * v11 * v22 + v01 * v13 * v22 + v02 * v11 * v23 - v01 * v12 * v23

        val det = v00 * t11 + v10 * t12 + v20 * t13 + v30 * t14

        if (det == 0f) {
            println("Matrix doesn't have inverse")
            return this.identity()
        }

        val detInv = 1 / det

        return target.setRows(
            t11 * detInv,
            t12 * detInv,
            t13 * detInv,
            t14 * detInv,

            (v13 * v22 * v30 - v12 * v23 * v30 - v13 * v20 * v32 + v10 * v23 * v32 + v12 * v20 * v33 - v10 * v22 * v33) * detInv,
            (v02 * v23 * v30 - v03 * v22 * v30 + v03 * v20 * v32 - v00 * v23 * v32 - v02 * v20 * v33 + v00 * v22 * v33) * detInv,
            (v03 * v12 * v30 - v02 * v13 * v30 - v03 * v10 * v32 + v00 * v13 * v32 + v02 * v10 * v33 - v00 * v12 * v33) * detInv,
            (v02 * v13 * v20 - v03 * v12 * v20 + v03 * v10 * v22 - v00 * v13 * v22 - v02 * v10 * v23 + v00 * v12 * v23) * detInv,

            (v11 * v23 * v30 - v13 * v21 * v30 + v13 * v20 * v31 - v10 * v23 * v31 - v11 * v20 * v33 + v10 * v21 * v33) * detInv,
            (v03 * v21 * v30 - v01 * v23 * v30 - v03 * v20 * v31 + v00 * v23 * v31 + v01 * v20 * v33 - v00 * v21 * v33) * detInv,
            (v01 * v13 * v30 - v03 * v11 * v30 + v03 * v10 * v31 - v00 * v13 * v31 - v01 * v10 * v33 + v00 * v11 * v33) * detInv,
            (v03 * v11 * v20 - v01 * v13 * v20 - v03 * v10 * v21 + v00 * v13 * v21 + v01 * v10 * v23 - v00 * v11 * v23) * detInv,

            (v12 * v21 * v30 - v11 * v22 * v30 - v12 * v20 * v31 + v10 * v22 * v31 + v11 * v20 * v32 - v10 * v21 * v32) * detInv,
            (v01 * v22 * v30 - v02 * v21 * v30 + v02 * v20 * v31 - v00 * v22 * v31 - v01 * v20 * v32 + v00 * v21 * v32) * detInv,
            (v02 * v11 * v30 - v01 * v12 * v30 - v02 * v10 * v31 + v00 * v12 * v31 + v01 * v10 * v32 - v00 * v11 * v32) * detInv,
            (v01 * v12 * v20 - v02 * v11 * v20 + v02 * v10 * v21 - v00 * v12 * v21 - v01 * v10 * v22 + v00 * v11 * v22) * detInv
        )
    }
}

inline fun Matrix3D.setToMap(filter: (Float) -> Float) = setRows(
    filter(v00), filter(v01), filter(v02), filter(v03),
    filter(v10), filter(v11), filter(v12), filter(v13),
    filter(v20), filter(v21), filter(v22), filter(v23),
    filter(v30), filter(v31), filter(v32), filter(v33)
)

fun Matrix3D.setToInterpolated(a: Matrix3D, b: Matrix3D, ratio: Double) = setColumns(
    ratio.interpolate(a.v00, b.v00), ratio.interpolate(a.v10, b.v10), ratio.interpolate(a.v20, b.v20), ratio.interpolate(a.v30, b.v30),
    ratio.interpolate(a.v01, b.v01), ratio.interpolate(a.v11, b.v11), ratio.interpolate(a.v21, b.v21), ratio.interpolate(a.v31, b.v31),
    ratio.interpolate(a.v02, b.v02), ratio.interpolate(a.v12, b.v12), ratio.interpolate(a.v22, b.v22), ratio.interpolate(a.v32, b.v32),
    ratio.interpolate(a.v03, b.v03), ratio.interpolate(a.v13, b.v13), ratio.interpolate(a.v23, b.v23), ratio.interpolate(a.v33, b.v33)
)
