package korlibs.math.geom

import korlibs.memory.pack.*
import kotlin.math.*

// https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles
//@KormaValueApi
data class Quaternion(val x: Float, val y: Float, val z: Float, val w: Float) {
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

    val vector: Vector4 get() = Vector4(x, y, z, w)
    val xyz: Vector3 get() = Vector3(x, y, z)
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

    constructor(vector: Vector4, unit: Unit = Unit) : this(vector.x, vector.y, vector.z, vector.w)
    constructor() : this(0f, 0f, 0f, 1f)
    constructor(x: Double, y: Double, z: Double, w: Double) : this(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    fun toMatrix(): Matrix4 = Matrix4.multiply(
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
        return Quaternion(Vector4(
            (left.xyz * right.w) + (right.xyz * left.w) + Vector3.cross(left.xyz, right.xyz),
            left.w * right.w - left.xyz.dot(right.xyz)
        ))
    }

    fun normalized(): Quaternion {
        val length = 1f / Vector4(x, y, z, w).length
        return Quaternion(x / length, y / length, z / length, w / length)
    }

    fun inverted(): Quaternion {
        val q = this
        val lengthSquared = q.lengthSquared
        return when (lengthSquared) {
            0f -> {
                val num = 1f / lengthSquared
                Quaternion(q.x * -num, q.y * -num, q.z * -num, q.w * num)
            }
            else -> q
        }
    }

    fun transform(v: Vector3): Vector3 {
        // Create a pure quaternion from the vector
        val q = this
        val p = Quaternion(v.x, v.y, v.z, 0f)
        // Multiply q by p, then by the conjugate of q
        val resultQuaternion = q * p * q.conjugate()
        // Return the vector part of the resulting quaternion
        return Vector3(resultQuaternion.x, resultQuaternion.y, resultQuaternion.z)
    }

    fun toEuler(): EulerRotation = toEuler(x, y, z, w)

    companion object {
        val IDENTITY = Quaternion()

        fun dotProduct(l: Quaternion, r: Quaternion): Float = l.x * r.x + l.y * r.y + l.z * r.z + l.w * r.w

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

        fun fromVectors(v1: Vector3, v2: Vector3): Quaternion {
            // Normalize input vectors
            val start = v1.normalized()
            val dest = v2.normalized()

            val dot = start.dot(dest)

            // If vectors are opposite
            when {
                dot < -0.9999999f -> {
                    val tmp = Vector3(start.y, -start.x, 0f).normalized()
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

        fun fromAxisAngle(axis: Vector3, angle: Angle): Quaternion {
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

        fun fromRotationMatrix(m: Matrix4): Quaternion {
            m.apply {
                val t = v00 + v11 + v22
                return when {
                    t > 0 -> {
                        val s = .5f / sqrt(t + 1f)
                        Quaternion(((v21 - v12) * s), ((v02 - v20) * s), ((v10 - v01) * s), (0.25f / s))
                    }
                    v00 > v11 && v00 > v22 -> {
                        val s = 2f * sqrt(1f + v00 - v11 - v22)
                        Quaternion((0.25f * s), ((v01 + v10) / s), ((v02 + v20) / s), ((v21 - v12) / s))
                    }
                    v11 > v22 -> {
                        val s = 2f * sqrt(1f + v11 - v00 - v22)
                        Quaternion(((v01 + v10) / s), (0.25f * s), ((v12 + v21) / s), ((v02 - v20) / s))
                    }
                    else -> {
                        val s = 2f * sqrt(1f + v22 - v00 - v11)
                        Quaternion(((v02 + v20) / s), ((v12 + v21) / s), (0.25f * s), ((v10 - v01) / s))
                    }
                }
            }
        }

        fun fromEuler(e: EulerRotation): Quaternion = e.toQuaternion()
        fun fromEuler(roll: Angle, pitch: Angle, yaw: Angle): Quaternion = EulerRotation(roll, pitch, yaw).toQuaternion()

        fun toEuler(x: Float, y: Float, z: Float, w: Float): EulerRotation {
            val sinrCosp = +2.0 * (w * x + y * z)
            val cosrCosp = +1.0 - 2.0 * (x * x + y * y)
            val roll = atan2(sinrCosp, cosrCosp)
            val sinp = +2.0 * (w * y - z * x)
            val pitch = when {
                abs(sinp) >= 1 -> if (sinp > 0) PI / 2 else -PI / 2
                else -> asin(sinp)
            }
            val sinyCosp = +2.0 * (w * z + x * y)
            val cosyCosp = +1.0 - 2.0 * (y * y + z * z)
            val yaw = atan2(sinyCosp, cosyCosp)
            return EulerRotation(roll.radians, pitch.radians, yaw.radians)
        }
    }
}
