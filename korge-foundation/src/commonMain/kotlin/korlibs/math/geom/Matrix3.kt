@file:Suppress("NOTHING_TO_INLINE")

package korlibs.math.geom

import korlibs.math.*
import kotlin.math.*

/**
 * Useful for representing rotations and scales.
 */
data class Matrix3 private constructor(
    internal val data: FloatArray,
) : IsAlmostEqualsF<Matrix3> {
    override fun equals(other: Any?): Boolean = other is Matrix3 && this.data.contentEquals(other.data)
    override fun hashCode(): Int = data.contentHashCode()

    private constructor(
        v00: Float, v10: Float, v20: Float,
        v01: Float, v11: Float, v21: Float,
        v02: Float, v12: Float, v22: Float,
    ) : this(
        floatArrayOf(
            v00, v10, v20,
            v01, v11, v21,
            v02, v12, v22,
        )
    )

    init {
        check(data.size == 9)
    }

    val v00: Float get() = data[0]
    val v10: Float get() = data[1]
    val v20: Float get() = data[2]
    val v01: Float get() = data[3]
    val v11: Float get() = data[4]
    val v21: Float get() = data[5]
    val v02: Float get() = data[6]
    val v12: Float get() = data[7]
    val v22: Float get() = data[8]

    val c0: Vector3F get() = Vector3F.fromArray(data, 0)
    val c1: Vector3F get() = Vector3F.fromArray(data, 3)
    val c2: Vector3F get() = Vector3F.fromArray(data, 6)
    fun c(column: Int): Vector3F {
        if (column < 0 || column >= 3) error("Invalid column $column")
        return Vector3F.fromArray(data, column * 3)
    }

    val r0: Vector3F get() = Vector3F(v00, v01, v02)
    val r1: Vector3F get() = Vector3F(v10, v11, v12)
    val r2: Vector3F get() = Vector3F(v20, v21, v22)

    fun v(index: Int): Float = data[index]

    fun r(row: Int): Vector3F = when (row) {
        0 -> r0
        1 -> r1
        2 -> r2
        else -> error("Invalid row $row")
    }

    operator fun get(row: Int, column: Int): Float {
        if (column !in 0..2 || row !in 0..2) error("Invalid index $row,$column")
        return data[row * 3 + column]
    }

    fun transform(v: Vector3F): Vector3F = Vector3F(r0.dot(v), r1.dot(v), r2.dot(v))

    operator fun unaryMinus(): Matrix3 = Matrix3(
        -v00, -v10, -v20,
        -v01, -v11, -v21,
        -v02, -v12, -v22,
    )
    operator fun unaryPlus(): Matrix3 = this

    operator fun minus(other: Matrix3): Matrix3 = Matrix3(
        v00 - other.v00, v10 - other.v10, v20 - other.v20,
        v01 - other.v01, v11 - other.v11, v21 - other.v21,
        v02 - other.v02, v12 - other.v12, v22 - other.v22,
    )
    operator fun plus(other: Matrix3): Matrix3 = Matrix3(
        v00 + other.v00, v10 + other.v10, v20 + other.v20,
        v01 + other.v01, v11 + other.v11, v21 + other.v21,
        v02 + other.v02, v12 + other.v12, v22 + other.v22,
    )

    operator fun times(other: Matrix3): Matrix3 = Matrix3.multiply(this, other)
    operator fun times(scale: Float): Matrix3 = Matrix3(
        v00 * scale, v10 * scale, v20 * scale,
        v01 * scale, v11 * scale, v21 * scale,
        v02 * scale, v12 * scale, v22 * scale,
    )
    operator fun div(scale: Float): Matrix3 = this * (1f / scale)

    fun inv(): Matrix3 = inverted()

    val determinant: Float get() = v00 * (v11 * v22 - v21 * v12) -
        v01 * (v10 * v22 - v12 * v20) +
        v02 * (v10 * v21 - v11 * v20)

    fun inverted(): Matrix3 {
        val determinant = this.determinant

        if (determinant == 0.0f) throw ArithmeticException("Matrix is not invertible")

        val invDet = 1.0f / determinant

        return fromRows(
            (v11 * v22 - v21 * v12) * invDet,
            (v02 * v21 - v01 * v22) * invDet,
            (v01 * v12 - v02 * v11) * invDet,
            (v12 * v20 - v10 * v22) * invDet,
            (v00 * v22 - v02 * v20) * invDet,
            (v10 * v02 - v00 * v12) * invDet,
            (v10 * v21 - v20 * v11) * invDet,
            (v20 * v01 - v00 * v21) * invDet,
            (v00 * v11 - v10 * v01) * invDet,
        )
    }

    override fun toString(): String = buildString {
        append("Matrix3(\n")
        for (row in 0 until 3) {
            append("  [ ")
            for (col in 0 until 3) {
                if (col != 0) append(", ")
                val v = get(row, col)
                if (floor(v) == v) append(v.toInt()) else append(v)
            }
            append(" ],\n")
        }
        append(")")
    }

    fun transposed(): Matrix3 = Matrix3.fromColumns(r0, r1, r2)

    override fun isAlmostEquals(other: Matrix3, epsilon: Float): Boolean = c0.isAlmostEquals(other.c0, epsilon)
        && c1.isAlmostEquals(other.c1, epsilon)
        && c2.isAlmostEquals(other.c2, epsilon)

    companion object {
        const val M00 = 0
        const val M10 = 1
        const val M20 = 2

        const val M01 = 3
        const val M11 = 4
        const val M21 = 5

        const val M02 = 6
        const val M12 = 7
        const val M22 = 8

        const val M03 = 9
        const val M13 = 10
        const val M23 = 11

        val INDICES_BY_COLUMNS = intArrayOf(
            M00, M10, M20,
            M01, M11, M21,
            M02, M12, M22,
        )
        val INDICES_BY_ROWS = intArrayOf(
            M00, M01, M02,
            M10, M11, M12,
            M20, M21, M22,
        )

        val IDENTITY = Matrix3(
            1f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 1f,
        )

        fun fromRows(
            r0: Vector3F, r1: Vector3F, r2: Vector3F
        ): Matrix3 = Matrix3(
            r0.x, r1.x, r2.x,
            r0.y, r1.y, r2.y,
            r0.z, r1.z, r2.z,
        )

        fun fromColumns(
            c0: Vector3F, c1: Vector3F, c2: Vector3F
        ): Matrix3 = Matrix3(
            c0.x, c0.y, c0.z,
            c1.x, c1.y, c1.z,
            c2.x, c2.y, c2.z,
        )

        fun fromColumns(
            v00: Float, v10: Float, v20: Float,
            v01: Float, v11: Float, v21: Float,
            v02: Float, v12: Float, v22: Float,
        ): Matrix3 = Matrix3(
            v00, v10, v20,
            v01, v11, v21,
            v02, v12, v22,
        )

        fun fromRows(
            v00: Float, v01: Float, v02: Float,
            v10: Float, v11: Float, v12: Float,
            v20: Float, v21: Float, v22: Float,
        ): Matrix3 = Matrix3(
            v00, v10, v20,
            v01, v11, v21,
            v02, v12, v22,
        )

        fun multiply(l: Matrix3, r: Matrix3): Matrix3 = Matrix3.fromRows(
            (l.v00 * r.v00) + (l.v01 * r.v10) + (l.v02 * r.v20),
            (l.v00 * r.v01) + (l.v01 * r.v11) + (l.v02 * r.v21),
            (l.v00 * r.v02) + (l.v01 * r.v12) + (l.v02 * r.v22),

            (l.v10 * r.v00) + (l.v11 * r.v10) + (l.v12 * r.v20),
            (l.v10 * r.v01) + (l.v11 * r.v11) + (l.v12 * r.v21),
            (l.v10 * r.v02) + (l.v11 * r.v12) + (l.v12 * r.v22),

            (l.v20 * r.v00) + (l.v21 * r.v10) + (l.v22 * r.v20),
            (l.v20 * r.v01) + (l.v21 * r.v11) + (l.v22 * r.v21),
            (l.v20 * r.v02) + (l.v21 * r.v12) + (l.v22 * r.v22),
        )
    }
}

fun Matrix3.toMatrix4(): Matrix4 = Matrix4.fromRows(
    v00, v01, v02, 0f,
    v10, v11, v12, 0f,
    v20, v21, v22, 0f,
    0f, 0f, 0f, 1f,
)

fun Matrix3.toQuaternion(): Quaternion = Quaternion.fromRotationMatrix(this)

