package korlibs.math.geom

import korlibs.math.*
import kotlin.math.*


// @TODO: WIP
// @TODO: value class
// Stored as four consecutive column vectors (effectively stored in column-major order) see https://en.wikipedia.org/wiki/Row-_and_column-major_order
// v[Row][Column]
//@KormaExperimental
//@KormaValueApi
//inline class Matrix4 private constructor(
/**
 * Useful for representing complete transforms: rotations, scales, translations, projections, etc.
 */
data class Matrix4 private constructor(
    private val data: FloatArray,
    //val c0: Vector4, val c1: Vector4, val c2: Vector4, val c3: Vector4,

    //val v00: Float, val v10: Float, val v20: Float, val v30: Float,
    //val v01: Float, val v11: Float, val v21: Float, val v31: Float,
    //val v02: Float, val v12: Float, val v22: Float, val v32: Float,
    //val v03: Float, val v13: Float, val v23: Float, val v33: Float,
) : IsAlmostEqualsF<Matrix4> {
    init {
        check(data.size == 16)
    }
    val v00: Float get() = data[0]; val v10: Float get() = data[1]; val v20: Float get() = data[2]; val v30: Float get() = data[3]
    val v01: Float get() = data[4]; val v11: Float get() = data[5]; val v21: Float get() = data[6]; val v31: Float get() = data[7]
    val v02: Float get() = data[8]; val v12: Float get() = data[9]; val v22: Float get() = data[10]; val v32: Float get() = data[11]
    val v03: Float get() = data[12]; val v13: Float get() = data[13]; val v23: Float get() = data[14]; val v33: Float get() = data[15]

    override fun equals(other: Any?): Boolean = other is Matrix4 && this.data.contentEquals(other.data)
    override fun hashCode(): Int = data.contentHashCode()

    operator fun times(scale: Float): Matrix4 = Matrix4.fromColumns(c0 * scale, c1 * scale, c2 * scale, c3 * scale)
    operator fun times(that: Matrix4): Matrix4 = Matrix4.multiply(this, that)

    fun transformTransposed(v: Vector4F): Vector4F = Vector4F(c0.dot(v), c1.dot(v), c2.dot(v), c3.dot(v))
    fun transform(v: Vector4F): Vector4F = Vector4F(r0.dot(v), r1.dot(v), r2.dot(v), r3.dot(v))
    fun transform(v: Vector3F): Vector3F = transform(v.toVector4()).toVector3()

    fun transposed(): Matrix4 = Matrix4.fromColumns(r0, r1, r2, r3)

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

    // Use toTRS/decompose
    //fun decomposeProjection(): Vector4 = c3
    //fun decomposeTranslation(): Vector4 = r3.copy(w = 1f)
    //fun decomposeScale(): Vector4 {
    //    val x = r0.length3
    //    val y = r1.length3
    //    val z = r2.length3
    //    return Vector4(x, y, z, 1f)
    //}
    fun decomposeRotation(rowNormalise: Boolean = true): Quaternion {
        var v1 = this.r0
        var v2 = this.r1
        var v3 = this.r2
        if (rowNormalise) {
            v1 = v1.normalized()
            v2 = v2.normalized()
            v3 = v3.normalized()
        }
        val d: Float = 0.25f * (v1[0] + v2[1] + v3[2] + 1f)
        val out: Vector4F
        when {
            d > 0f -> {
                val num1: Float = sqrt(d)
                val num2: Float = 1f / (4f * num1)
                out = Vector4F(
                    ((v2[2] - v3[1]) * num2),
                    ((v3[0] - v1[2]) * num2),
                    ((v1[1] - v2[0]) * num2),
                    num1,
                )
            }
            v1[0] > v2[1] && v1[0] > v3[2] -> {
                val num1: Float = 2f * sqrt(1f + v1[0] - v2[1] - v3[2])
                val num2: Float = 1f / num1
                out = Vector4F(
                    (0.25f * num1),
                    ((v2[0] + v1[1]) * num2),
                    ((v3[0] + v1[2]) * num2),
                    ((v3[1] - v2[2]) * num2),
                )
            }
            v2[1] > v3[2] -> {
                val num5: Float = 2f * sqrt(1f + v2[1] - v1[0] - v3[2])
                val num6: Float = 1f / num5
                out = Vector4F(
                    ((v2[0] + v1[1]) * num6),
                    (0.25f * num5),
                    ((v3[1] + v2[2]) * num6),
                    ((v3[0] - v1[2]) * num6),
                )
            }
            else -> {
                val num7: Float = 2f * sqrt(1f + v3[2] - v1[0] - v2[1])
                val num8: Float = 1f / num7
                out = Vector4F(
                    ((v3[0] + v1[2]) * num8),
                    ((v3[1] + v2[2]) * num8),
                    (0.25f * num7),
                    ((v2[0] - v1[1]) * num8),
                )
            }
        }
        return Quaternion(out.normalized())
    }

    fun copyToColumns(out: FloatArray = FloatArray(16), offset: Int = 0): FloatArray {
        this.data.copyInto(out, offset, 0, 16)
        return out
    }
    fun copyToRows(out: FloatArray = FloatArray(16), offset: Int = 0): FloatArray {
        this.r0.copyTo(out, offset + 0)
        this.r1.copyTo(out, offset + 4)
        this.r2.copyTo(out, offset + 8)
        this.r3.copyTo(out, offset + 12)
        return out
    }

    private constructor(
        v00: Float, v10: Float, v20: Float, v30: Float,
        v01: Float, v11: Float, v21: Float, v31: Float,
        v02: Float, v12: Float, v22: Float, v32: Float,
        v03: Float, v13: Float, v23: Float, v33: Float,
    ) : this(floatArrayOf(
        v00, v10, v20, v30,
        v01, v11, v21, v31,
        v02, v12, v22, v32,
        v03, v13, v23, v33,
    ))

    constructor() : this(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f,
    )

    val c0: Vector4F get() = Vector4F.fromArray(data, 0)
    val c1: Vector4F get() = Vector4F.fromArray(data, 4)
    val c2: Vector4F get() = Vector4F.fromArray(data, 8)
    val c3: Vector4F get() = Vector4F.fromArray(data, 12)
    fun c(column: Int): Vector4F {
        if (column < 0 || column >= 4) error("Invalid column $column")
        return Vector4F.fromArray(data, column * 4)
    }

    val r0: Vector4F get() = Vector4F(v00, v01, v02, v03)
    val r1: Vector4F get() = Vector4F(v10, v11, v12, v13)
    val r2: Vector4F get() = Vector4F(v20, v21, v22, v23)
    val r3: Vector4F get() = Vector4F(v30, v31, v32, v33)

    fun r(row: Int): Vector4F = when (row) {
        0 -> r0
        1 -> r1
        2 -> r2
        3 -> r3
        else -> error("Invalid row $row")
    }

    operator fun get(row: Int, column: Int): Float {
        if (column !in 0..3 || row !in 0..3) error("Invalid index $row,$column")
        return data[row * 4 + column]
    }

    fun getAtIndex(index: Int): Float {
        if (index !in data.indices) error("Invalid index $index")
        return data[index]
    }

    override fun toString(): String = buildString {
        append("Matrix4(\n")
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



    fun translated(x: Float, y: Float, z: Float, w: Float = 1f): Matrix4 = this * Matrix4.translation(x, y, z, w)
    fun translated(x: Double, y: Double, z: Double, w: Double = 1.0) = this.translated(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    fun translated(x: Int, y: Int, z: Int, w: Int = 1) = this.translated(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    fun rotated(angle: Angle, x: Float, y: Float, z: Float): Matrix4 = this * Matrix4.rotation(angle, x, y, z)
    fun rotated(angle: Angle, x: Double, y: Double, z: Double): Matrix4 = this.rotated(angle, x.toFloat(), y.toFloat(), z.toFloat())
    fun rotated(angle: Angle, x: Int, y: Int, z: Int): Matrix4 = this.rotated(angle, x.toFloat(), y.toFloat(), z.toFloat())

    fun scaled(x: Float, y: Float, z: Float, w: Float = 1f): Matrix4 = this * Matrix4.scale(x, y, z, w)
    fun scaled(x: Double, y: Double, z: Double, w: Double = 1.0): Matrix4 = this.scaled(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    fun scaled(x: Int, y: Int, z: Int, w: Int = 1): Matrix4 = this.scaled(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    fun rotated(quat: Quaternion): Matrix4 = this * quat.toMatrix()
    fun rotated(euler: EulerRotation): Matrix4 = this * euler.toMatrix()
    fun rotated(x: Angle, y: Angle, z: Angle): Matrix4 = rotated(x, 1f, 0f, 0f).rotated(y, 0f, 1f, 0f).rotated(z, 0f, 0f, 1f)

    fun decompose(): TRS4 = toTRS()
    fun toTRS(): TRS4 {
        val det = determinant
        val translation = Vector4F(v03, v13, v23, 1f)
        val scale = Vector4F(Vector3F.length(v00, v10, v20) * det.sign, Vector3F.length(v01, v11, v21), Vector3F.length(v02, v12, v22), 1f)
        val invSX = 1f / scale.x
        val invSY = 1f / scale.y
        val invSZ = 1f / scale.z
        val rotation = Quaternion.fromRotationMatrix(Matrix4.fromRows(
            v00 * invSX, v01 * invSY, v02 * invSZ, v03,
            v10 * invSX, v11 * invSY, v12 * invSZ, v13,
            v20 * invSX, v21 * invSY, v22 * invSZ, v23,
            v30, v31, v32, v33
        ))
        return TRS4(translation, rotation, scale)
    }

    fun inverted(): Matrix4 {
        val t11 = v12 * v23 * v31 - v13 * v22 * v31 + v13 * v21 * v32 - v11 * v23 * v32 - v12 * v21 * v33 + v11 * v22 * v33
        val t12 = v03 * v22 * v31 - v02 * v23 * v31 - v03 * v21 * v32 + v01 * v23 * v32 + v02 * v21 * v33 - v01 * v22 * v33
        val t13 = v02 * v13 * v31 - v03 * v12 * v31 + v03 * v11 * v32 - v01 * v13 * v32 - v02 * v11 * v33 + v01 * v12 * v33
        val t14 = v03 * v12 * v21 - v02 * v13 * v21 - v03 * v11 * v22 + v01 * v13 * v22 + v02 * v11 * v23 - v01 * v12 * v23

        val det = v00 * t11 + v10 * t12 + v20 * t13 + v30 * t14

        if (det == 0f) {
            println("Matrix doesn't have inverse")
            return Matrix4.IDENTITY
        }

        val detInv = 1 / det

        return Matrix4.fromRows(
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

    override fun isAlmostEquals(other: Matrix4, epsilon: Float): Boolean =
        c0.isAlmostEquals(other.c0, epsilon) &&
            c1.isAlmostEquals(other.c1, epsilon) &&
            c2.isAlmostEquals(other.c2, epsilon) &&
            c3.isAlmostEquals(other.c3, epsilon)

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

        val INDICES_BY_COLUMNS_4x4 get() = MMatrix4.INDICES_BY_COLUMNS_4x4
        val INDICES_BY_ROWS_4x4 get() = MMatrix4.INDICES_BY_ROWS_4x4
        val INDICES_BY_COLUMNS_3x3 get() = MMatrix4.INDICES_BY_COLUMNS_3x3
        val INDICES_BY_ROWS_3x3 get() = MMatrix4.INDICES_BY_ROWS_3x3

        val IDENTITY = Matrix4()

        fun fromColumns(
            c0: Vector4F, c1: Vector4F, c2: Vector4F, c3: Vector4F
        ): Matrix4 = Matrix4(
            c0.x, c0.y, c0.z, c0.w,
            c1.x, c1.y, c1.z, c1.w,
            c2.x, c2.y, c2.z, c2.w,
            c3.x, c3.y, c3.z, c3.w,
        )

        fun fromColumns(v: FloatArray, offset: Int = 0): Matrix4 = Matrix4.fromColumns(
            v[offset + 0], v[offset + 1], v[offset + 2], v[offset + 3],
            v[offset + 4], v[offset + 5], v[offset + 6], v[offset + 7],
            v[offset + 8], v[offset + 9], v[offset + 10], v[offset + 11],
            v[offset + 12], v[offset + 13], v[offset + 14], v[offset + 15],
        )

        fun fromRows(v: FloatArray, offset: Int = 0): Matrix4 = Matrix4.fromRows(
            v[offset + 0], v[offset + 1], v[offset + 2], v[offset + 3],
            v[offset + 4], v[offset + 5], v[offset + 6], v[offset + 7],
            v[offset + 8], v[offset + 9], v[offset + 10], v[offset + 11],
            v[offset + 12], v[offset + 13], v[offset + 14], v[offset + 15],
        )

        fun fromRows(
            r0: Vector4F, r1: Vector4F, r2: Vector4F, r3: Vector4F
        ): Matrix4 = Matrix4(
            r0.x, r1.x, r2.x, r3.x,
            r0.y, r1.y, r2.y, r3.y,
            r0.z, r1.z, r2.z, r3.z,
            r0.w, r1.w, r2.w, r3.w,
        )

        fun fromColumns(
            v00: Float, v10: Float, v20: Float, v30: Float,
            v01: Float, v11: Float, v21: Float, v31: Float,
            v02: Float, v12: Float, v22: Float, v32: Float,
            v03: Float, v13: Float, v23: Float, v33: Float,
        ): Matrix4 = Matrix4(
            v00, v10, v20, v30,
            v01, v11, v21, v31,
            v02, v12, v22, v32,
            v03, v13, v23, v33,
        )

        fun fromRows(
            v00: Float, v01: Float, v02: Float, v03: Float,
            v10: Float, v11: Float, v12: Float, v13: Float,
            v20: Float, v21: Float, v22: Float, v23: Float,
            v30: Float, v31: Float, v32: Float, v33: Float,
        ): Matrix4 = Matrix4(
            v00, v10, v20, v30,
            v01, v11, v21, v31,
            v02, v12, v22, v32,
            v03, v13, v23, v33,
        )

        fun fromRows3x3(
            a00: Float, a01: Float, a02: Float,
            a10: Float, a11: Float, a12: Float,
            a20: Float, a21: Float, a22: Float
        ): Matrix4 = Matrix4.fromRows(
            a00, a01, a02, 0f,
            a10, a11, a12, 0f,
            a20, a21, a22, 0f,
            0f, 0f, 0f, 1f,
        )

        fun fromColumns3x3(
            a00: Float, a10: Float, a20: Float,
            a01: Float, a11: Float, a21: Float,
            a02: Float, a12: Float, a22: Float
        ): Matrix4 = Matrix4.fromColumns(
            a00, a10, a20, 0f,
            a01, a11, a21, 0f,
            a02, a12, a22, 0f,
            0f, 0f, 0f, 1f,
        )

        fun fromTRS(trs: TRS4): Matrix4 = fromTRS(trs.translation, trs.rotation, trs.scale)
        fun fromTRS(translation: Vector4F, rotation: Quaternion, scale: Vector4F): Matrix4 {
            val rx = rotation.x
            val ry = rotation.y
            val rz = rotation.z
            val rw = rotation.w

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

            return Matrix4.fromRows(
                ((1 - (yy + zz)) * scale.x), ((xy - wz) * scale.y), ((xz + wy) * scale.z), translation.x,
                ((xy + wz) * scale.x), ((1 - (xx + zz)) * scale.y), ((yz - wx) * scale.z), translation.y,
                ((xz - wy) * scale.x), ((yz + wx) * scale.y), ((1 - (xx + yy)) * scale.z), translation.z,
                0f, 0f, 0f, 1f
            )
        }

        fun translation(x: Float, y: Float, z: Float, w: Float = 1f): Matrix4 = Matrix4.fromRows(
            1f, 0f, 0f, x,
            0f, 1f, 0f, y,
            0f, 0f, 1f, z,
            0f, 0f, 0f, w
        )
        fun translation(x: Double, y: Double, z: Double, w: Double = 1.0): Matrix4 = translation(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
        fun translation(x: Int, y: Int, z: Int, w: Int = 1): Matrix4 = translation(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

        fun scale(x: Float, y: Float, z: Float, w: Float = 1f): Matrix4 = Matrix4.fromRows(
            x, 0f, 0f, 0f,
            0f, y, 0f, 0f,
            0f, 0f, z, 0f,
            0f, 0f, 0f, w
        )
        fun scale(x: Double, y: Double, z: Double, w: Double = 1.0): Matrix4 = scale(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
        fun scale(x: Int, y: Int, z: Int, w: Int = 1): Matrix4 = scale(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

        fun shear(x: Float, y: Float, z: Float): Matrix4 = fromRows(
            1f, y, z, 0f,
            x, 1f, z, 0f,
            x, y, 1f, 0f,
            0f, 0f, 0f, 1f
        )
        fun shear(x: Double, y: Double, z: Double): Matrix4 = shear(x.toFloat(), y.toFloat(), z.toFloat())
        fun shear(x: Int, y: Int, z: Int): Matrix4 = shear(x.toFloat(), y.toFloat(), z.toFloat())

        fun rotationX(angle: Angle): Matrix4 {
            val c = angle.cosine.toFloat()
            val s = angle.sine.toFloat()
            return Matrix4.fromRows(
                1f, 0f, 0f, 0f,
                0f, c, -s, 0f,
                0f, s, c, 0f,
                0f, 0f, 0f, 1f
            )
        }

        fun rotationY(angle: Angle): Matrix4 {
            val c = angle.cosine.toFloat()
            val s = angle.sine.toFloat()
            return Matrix4.fromRows(
                c, 0f, s, 0f,
                0f, 1f, 0f, 0f,
                -s, 0f, c, 0f,
                0f, 0f, 0f, 1f
            )
        }

        fun rotationZ(angle: Angle): Matrix4 {
            val c = angle.cosine.toFloat()
            val s = angle.sine.toFloat()
            return Matrix4.fromRows(
                c, -s, 0f, 0f,
                s, c, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f
            )
        }

        fun rotation(angle: Angle, x: Float, y: Float, z: Float): Matrix4 {
            val mag = sqrt(x * x + y * y + z * z)
            val norm = 1f / mag

            val nx = x * norm
            val ny = y * norm
            val nz = z * norm
            val c = angle.cosine.toFloat()
            val s = angle.sine.toFloat()
            val t = 1 - c
            val tx = t * nx
            val ty = t * ny

            return Matrix4.fromRows(
                tx * nx + c, tx * ny - s * nz, tx * nz + s * ny, 0f,
                tx * ny + s * nz, ty * ny + c, ty * nz - s * nx, 0f,
                tx * nz - s * ny, ty * nz + s * nx, t * nz * nz + c, 0f,
                0f, 0f, 0f, 1f
            )
        }
        fun rotation(angle: Angle, direction: Vector3F): Matrix4 = rotation(angle, direction.x, direction.y, direction.z)
        fun rotation(angle: Angle, x: Double, y: Double, z: Double): Matrix4 = rotation(angle, x.toFloat(), y.toFloat(), z.toFloat())
        fun rotation(angle: Angle, x: Int, y: Int, z: Int): Matrix4 = rotation(angle, x.toFloat(), y.toFloat(), z.toFloat())

        // @TODO: Use Vector4 operations, and use columns instead of rows for faster set
        fun multiply(l: Matrix4, r: Matrix4): Matrix4 = Matrix4.fromRows(
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
        ): Matrix4 = Matrix4.fromRows(
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

        fun ortho(left: Float, right: Float, bottom: Float, top: Float, near: Float = 0f, far: Float = 1f): Matrix4 {
            val sx = 2f / (right - left)
            val sy = 2f / (top - bottom)
            val sz = -2f / (far - near)

            val tx = -(right + left) / (right - left)
            val ty = -(top + bottom) / (top - bottom)
            val tz = -(far + near) / (far - near)

            return Matrix4.fromRows(
                sx, 0f, 0f, tx,
                0f, sy, 0f, ty,
                0f, 0f, sz, tz,
                0f, 0f, 0f, 1f
            )
        }
        fun ortho(left: Double, right: Double, bottom: Double, top: Double, near: Double, far: Double): Matrix4 =
            ortho(left.toFloat(), right.toFloat(), bottom.toFloat(), top.toFloat(), near.toFloat(), far.toFloat())
        fun ortho(left: Int, right: Int, bottom: Int, top: Int, near: Int, far: Int): Matrix4 =
            ortho(left.toFloat(), right.toFloat(), bottom.toFloat(), top.toFloat(), near.toFloat(), far.toFloat())

        fun ortho(rect: Rectangle, near: Float = 0f, far: Float = 1f): Matrix4 = ortho(rect.left, rect.right, rect.bottom, rect.top, near.toDouble(), far.toDouble())
        fun ortho(rect: Rectangle, near: Double = 0.0, far: Double = 1.0): Matrix4 = ortho(rect, near.toFloat(), far.toFloat())
        fun ortho(rect: Rectangle, near: Int = 0, far: Int = 1): Matrix4 = ortho(rect, near.toFloat(), far.toFloat())

        fun frustum(left: Float, right: Float, bottom: Float, top: Float, zNear: Float = 0f, zFar: Float = 1f): Matrix4 {
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

            return Matrix4.fromRows(
                zNear2 / dx, 0f, A, 0f,
                0f, zNear2 / dy, B, 0f,
                0f, 0f, C, D,
                0f, 0f, -1f, 0f
            )
        }
        fun frustum(left: Double, right: Double, bottom: Double, top: Double, zNear: Double = 0.0, zFar: Double = 1.0): Matrix4
            = frustum(left.toFloat(), right.toFloat(), bottom.toFloat(), top.toFloat(), zNear.toFloat(), zFar.toFloat())
        fun frustum(left: Int, right: Int, bottom: Int, top: Int, zNear: Int = 0, zFar: Int = 1): Matrix4
            = frustum(left.toFloat(), right.toFloat(), bottom.toFloat(), top.toFloat(), zNear.toFloat(), zFar.toFloat())

        fun frustum(rect: Rectangle, zNear: Float = 0f, zFar: Float = 1f): Matrix4 = frustum(rect.left, rect.right, rect.bottom, rect.top, zNear.toDouble(), zFar.toDouble())
        fun frustum(rect: Rectangle, zNear: Double = 0.0, zFar: Double = 1.0): Matrix4 = frustum(rect, zNear.toFloat(), zFar.toFloat())
        fun frustum(rect: Rectangle, zNear: Int = 0, zFar: Int = 1): Matrix4 = frustum(rect, zNear.toFloat(), zFar.toFloat())


        fun perspective(fovy: Angle, aspect: Float, zNear: Float, zFar: Float): Matrix4 {
            val top = tan(fovy.radians.toFloat() / 2f) * zNear
            val bottom = -1.0f * top
            val left = aspect * bottom
            val right = aspect * top
            return frustum(left, right, bottom, top, zNear, zFar)
        }
        fun perspective(fovy: Angle, aspect: Double, zNear: Double, zFar: Double): Matrix4
            = perspective(fovy, aspect.toFloat(), zNear.toFloat(), zFar.toFloat())

        fun lookAt(
            eye: Vector3F,
            target: Vector3F,
            up: Vector3F
        ): Matrix4 {
            var z = eye - target
            if (z.lengthSquared == 0f) z = z.copy(z = 1f)
            z = z.normalized()
            var x = Vector3F.cross(up, z)
            if (x.lengthSquared == 0f) {
                z = when {
                    abs(up.z) == 1f -> z.copy(x = z.x + 0.0001f)
                    else -> z.copy(z = z.z + 0.0001f)
                }
                z = z.normalized()
                x = Vector3F.cross(up, z)
            }
            x = x.normalized()
            val y = Vector3F.cross(z, x)
            return Matrix4.fromRows(
                x.x, y.x, z.x, 0f,
                x.y, y.y, z.y, 0f,
                x.z, y.z, z.z, 0f,
                //-x.dot(eye), -y.dot(eye), -z.dot(eye), 1f // @TODO: Check why is this making other tests to fail
                0f, 0f, 0f, 1f
            )
        }
    }
}

data class TRS4(val translation: Vector4F, val rotation: Quaternion, val scale: Vector4F)

fun Matrix4.toMatrix3(): Matrix3 = Matrix3.fromRows(
    v00, v01, v02,
    v10, v11, v12,
    v20, v21, v22
)
