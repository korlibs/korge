package korlibs.math.geom

import korlibs.math.*
import korlibs.math.interpolation.*
import korlibs.math.isAlmostZero
import kotlin.math.*

// https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles
//@KormaValueApi
data class Quaternion(val x: Float, val y: Float, val z: Float, val w: Float) : IsAlmostEqualsF<Quaternion> {
//inline class Quaternion private constructor(val data: Float4Pack) {
//    constructor(x: Float, y: Float, z: Float, w: Float) : this(float4PackOf(x, y, z, w))
//    val x: Float get() = data.f0
//    val y: Float get() = data.f1
//    val z: Float get() = data.f2
//    val w: Float get() = data.f3
//    operator fun component1(): Float = x
//    operator fun component2(): Float = y
//    operator fun component3(): Float = z
//    operator fun component4(): Float = w

    val vector: Vector4F get() = Vector4F(x, y, z, w)
    val xyz: Vector3F get() = Vector3F(x, y, z)
    fun conjugate() = Quaternion(-x, -y, -z, w)
    operator fun get(index: Int): Float = when (index) {
        0 -> x
        1 -> y
        2 -> z
        3 -> w
        else -> Float.NaN
    }

    val lengthSquared: Float get() = (x * x) + (y * y) + (z * z) + (w * w)
    val length: Float get() = sqrt(lengthSquared)

    constructor(vector: Vector4F, unit: Unit = Unit) : this(vector.x, vector.y, vector.z, vector.w)
    constructor() : this(0f, 0f, 0f, 1f)
    constructor(x: Double, y: Double, z: Double, w: Double) : this(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    fun toMatrix(): Matrix4 {
        val v = _toMatrix()
        return Matrix4.fromRows(
            v[0], v[1], v[2], 0f,
            v[3], v[4], v[5], 0f,
            v[6], v[7], v[8], 0f,
            0f, 0f, 0f, 1f,
        )
    }

    fun toMatrix3(): Matrix3 {
        val v = _toMatrix()
        return Matrix3.fromRows(
            v[0], v[1], v[2],
            v[3], v[4], v[5],
            v[6], v[7], v[8],
        )
    }

    private fun _toMatrix(): FloatArray {
        val xx = x * x
        val xy = x * y
        val xz = x * z
        val xw = x * w
        val yy = y * y
        val yz = y * z
        val yw = y * w
        val zz = z * z
        val zw = z * w

        return floatArrayOf(
            1 - 2 * (yy + zz), 2 * (xy - zw), 2 * (xz + yw),
            2 * (xy + zw), 1 - 2 * (xx + zz), 2 * (yz - xw),
            2 * (xz - yw), 2 * (yz + xw), 1 - 2 * (xx + yy),
        )
    }

    @Deprecated("Use toMatrix instead")
    fun toMatrixInverted(): Matrix4 = Matrix4.multiply(
        // Left
        w, z, -y, x,
        -z, w, x, y,
        y, -x, w, z,
        -x, -y, -z, w,
        // Right
        w, z, -y, -x,
        -z, w, x, -y,
        y, -x, w, -z,
        x, y, z, w,
    )

    operator fun unaryMinus(): Quaternion = Quaternion(-x, -y, -z, -w)
    operator fun plus(other: Quaternion): Quaternion = Quaternion(x + other.x, y + other.y, z + other.z, w + other.w)
    operator fun minus(other: Quaternion): Quaternion = Quaternion(x - other.x, y - other.y, z - other.z, w - other.w)

    fun scaled(scale: Float): Quaternion = Quaternion.interpolated(Quaternion.IDENTITY, this, scale)
    fun scaled(scale: Double): Quaternion = scaled(scale.toFloat())
    fun scaled(scale: Int): Quaternion = scaled(scale.toFloat())

    operator fun times(scale: Float): Quaternion = Quaternion(x * scale, y * scale, z * scale, w * scale)
    operator fun times(scale: Double): Quaternion = times(scale.toFloat())
    operator fun times(other: Quaternion): Quaternion {
        val left = this
        val right = other
        return Quaternion(Vector4F(
            (left.xyz * right.w) + (right.xyz * left.w) + Vector3F.cross(left.xyz, right.xyz),
            left.w * right.w - left.xyz.dot(right.xyz)
        ))
    }

    fun normalized(): Quaternion {
        val length = 1f / Vector4F(x, y, z, w).length
        return Quaternion(x / length, y / length, z / length, w / length)
    }

    /** Also known as conjugate */
    fun inverted(): Quaternion {
        val q = this
        val lengthSquared = q.lengthSquared
        if (lengthSquared.isAlmostZero()) error("Zero quaternion doesn't have invesrse")
        val num = 1f / lengthSquared
        return Quaternion(q.x * -num, q.y * -num, q.z * -num, q.w * num)
    }

    fun transform(v: Vector3F): Vector3F {
        // Create a pure quaternion from the vector
        val q = this
        val p = Quaternion(v.x, v.y, v.z, 0f)
        // Multiply q by p, then by the conjugate of q
        val resultQuaternion = q * p * q.conjugate()
        // Return the vector part of the resulting quaternion
        return Vector3F(resultQuaternion.x, resultQuaternion.y, resultQuaternion.z)
    }

    fun toEuler(config: EulerRotation.Config = EulerRotation.Config.DEFAULT): EulerRotation = EulerRotation.fromQuaternion(this, config)
    override fun isAlmostEquals(other: Quaternion, epsilon: Float): Boolean =
        this.x.isAlmostEquals(other.x, epsilon)
            && this.y.isAlmostEquals(other.y, epsilon)
            && this.z.isAlmostEquals(other.z, epsilon)
            && this.w.isAlmostEquals(other.w, epsilon)

    fun interpolated(other: Quaternion, t: Float): Quaternion = interpolated(this, other, t)
    fun interpolated(other: Quaternion, t: Ratio): Quaternion = interpolated(this, other, t.toFloat())
    fun angleTo(other: Quaternion): Angle = angleBetween(this, other)

    companion object {
        val IDENTITY = Quaternion()

        fun dotProduct(l: Quaternion, r: Quaternion): Float = l.x * r.x + l.y * r.y + l.z * r.z + l.w * r.w

        fun angleBetween(a: Quaternion, b: Quaternion): Angle {
            val dot = dotProduct(a, b)
            return Angle.arcCosine(2 * (dot * dot) - 1)
        }

        inline fun func(callback: (Int) -> Float) = Quaternion(callback(0), callback(1), callback(2), callback(3))
        inline fun func(l: Quaternion, r: Quaternion, func: (l: Float, r: Float) -> Float) = Quaternion(
            func(l.x, r.x),
            func(l.y, r.y),
            func(l.z, r.z),
            func(l.w, r.w)
        )
        fun slerp(left: Quaternion, right: Quaternion, t: Float): Quaternion {
            var tleft = left.normalized()
            var tright = right.normalized()

            var dot = Quaternion.dotProduct(tleft, right)

            if (dot < 0.0f) {
                tright = -tright
                dot = -dot
            }

            if (dot > 0.99995f) return func(tleft, tright) { l, r -> l + t * (r - l) }

            val angle0 = acos(dot)
            val angle1 = angle0 * t

            val s1 = sin(angle1) / sin(angle0)
            val s0 = cos(angle1) - dot * s1

            return func(tleft, tright) { l, r -> ((s0 * l) + (s1 * r)) }
        }

        fun nlerp(left: Quaternion, right: Quaternion, t: Double): Quaternion {
            val sign = if (Quaternion.dotProduct(left, right) < 0) -1 else +1
            return func { ((1f - t) * left[it] + t * right[it] * sign).toFloat() }.normalized()
        }

        fun interpolated(left: Quaternion, right: Quaternion, t: Float): Quaternion = slerp(left, right, t)

        fun fromVectors(from: Vector3F, to: Vector3F): Quaternion {
            // Normalize input vectors
            val start = from.normalized()
            val dest = to.normalized()

            val dot = start.dot(dest)

            // If vectors are opposite
            when {
                dot < -0.9999999f -> {
                    val tmp = Vector3F(start.y, -start.x, 0f).normalized()
                    return Quaternion(tmp.x, tmp.y, tmp.z, 0f)
                }

                dot > 0.9999999f -> {
                    // If vectors are same
                    return Quaternion()
                }

                else -> {
                    val s = kotlin.math.sqrt((1 + dot) * 2)
                    val invs = 1 / s

                    val c = start.cross(dest)

                    return Quaternion(
                        c.x * invs,
                        c.y * invs,
                        c.z * invs,
                        s * 0.5f,
                    ).normalized()
                }
            }
        }

        fun fromAxisAngle(axis: Vector3F, angle: Angle): Quaternion {
            val naxis = axis.normalized()
            val angle2 = angle / 2
            val s = sin(angle2)
            return Quaternion(
                naxis.x * s,
                naxis.y * s,
                naxis.z * s,
                cos(angle2)
            )
        }

        // @TODO: Check
        fun lookRotation(forward: Vector3F, up: Vector3F = Vector3F.UP): Quaternion {
            //if (up == Vector3.UP) return fromVectors(Vector3.FORWARD, forward.normalized())
            val z = forward.normalized()
            val x = (up.normalized() cross z).normalized()

            //println("x=$x, z=$z")
            if (x.lengthSquared.isAlmostZero()) {
                // COLLINEAR
                return Quaternion.fromVectors(Vector3F.FORWARD, z)
            }

            val y = z cross x
            return fromRotationMatrix(Matrix3.fromColumns(x, y, z))
        }

        fun fromRotationMatrix(m: Matrix4): Quaternion = fromRotationMatrix(
            m.v00, m.v10, m.v20,
            m.v01, m.v11, m.v21,
            m.v02, m.v12, m.v22,
        )

        fun fromRotationMatrix(m: Matrix3): Quaternion = fromRotationMatrix(
            m.v00, m.v10, m.v20,
            m.v01, m.v11, m.v21,
            m.v02, m.v12, m.v22,
        )

        fun fromRotationMatrix(
            v00: Float, v10: Float, v20: Float,
            v01: Float, v11: Float, v21: Float,
            v02: Float, v12: Float, v22: Float,
        ): Quaternion {
            val t = v00 + v11 + v22
            //println("t=$t, v00=$v00, v11=$v11, v22=$v22")
            return when {
                t >= 0 -> {
                    val s = .5f / sqrt(t + 1f)
                    //println("[0]")
                    Quaternion(((v21 - v12) * s), ((v02 - v20) * s), ((v10 - v01) * s), (0.25f / s))
                }
                v00 > v11 && v00 > v22 -> {
                    val s = 2f * sqrt(1f + v00 - v11 - v22)
                    //println("[1]")
                    Quaternion((0.25f * s), ((v01 + v10) / s), ((v02 + v20) / s), ((v21 - v12) / s))
                }
                v11 > v22 -> {
                    val s = 2f * sqrt(1f + v11 - v00 - v22)
                    //println("[2]")
                    Quaternion(((v01 + v10) / s), (.25f * s), ((v12 + v21) / s), ((v02 - v20) / s))
                }
                else -> {
                    val s = 2f * sqrt(1f + v22 - v00 - v11)
                    //println("[3]")
                    Quaternion(((v02 + v20) / s), ((v12 + v21) / s), (.25f * s), ((v10 - v01) / s))
                }
            }
        }

        fun fromEuler(e: EulerRotation): Quaternion = e.toQuaternion()
        fun fromEuler(roll: Angle, pitch: Angle, yaw: Angle): Quaternion = EulerRotation(roll, pitch, yaw).toQuaternion()

        fun toEuler(x: Float, y: Float, z: Float, w: Float, config: EulerRotation.Config = EulerRotation.Config.DEFAULT): EulerRotation {
            return EulerRotation.Companion.fromQuaternion(x, y, z, w, config)
            /*
            val t = y * x + z * w
            // Gimbal lock, if any: positive (+1) for north pole, negative (-1) for south pole, zero (0) when no gimbal lock
            val pole = if (t > 0.499f) 1 else if (t < -0.499f) -1 else 0
            return EulerRotation(
                roll = when (pole) {
                    0 -> Angle.asin((2f * (w * x - z * y)).clamp(-1f, +1f))
                    else -> (pole.toFloat() * PIF * .5f).radians
                },
                pitch = when (pole) {
                    0 -> Angle.atan2(2f * (y * w + x * z), 1f - 2f * (y * y + x * x))
                    else -> Angle.ZERO
                },
                yaw = when (pole) {
                    0 -> Angle.atan2(2f * (w * z + y * x), 1f - 2f * (x * x + z * z))
                    else -> Angle.atan2(y, w) * pole.toFloat() * 2f
                },
            )

             */
        }
    }
}

fun Angle.Companion.between(a: Quaternion, b: Quaternion): Angle = Quaternion.angleBetween(a, b)
