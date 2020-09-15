package com.soywiz.korma.geom

import kotlin.math.*

// https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles
data class Quaternion(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var z: Double = 0.0,
    var w: Double = 1.0
) {
    companion object {
        fun dotProduct(l: Quaternion, r: Quaternion): Double = l.x * r.x + l.y * r.y + l.z * r.z + l.w * r.w
        operator fun invoke(x: Float, y: Float, z: Float, w: Float) = Quaternion(x.toDouble(), y.toDouble(), z.toDouble(), w.toDouble())
        operator fun invoke(x: Int, y: Int, z: Int, w: Int) = Quaternion(x.toDouble(), y.toDouble(), z.toDouble(), w.toDouble())
        fun toEuler(q: Quaternion, out: EulerRotation = EulerRotation()): EulerRotation = toEuler(q.x, q.y, q.z, q.w, out)
        fun toEuler(x: Double, y: Double, z: Double, w: Double, euler: EulerRotation = EulerRotation()): EulerRotation =
            toEuler(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat(), euler)
        fun toEuler(x: Int, y: Int, z: Int, w: Int, euler: EulerRotation = EulerRotation()): EulerRotation =
            toEuler(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat(), euler)

        fun toEuler(x: Float, y: Float, z: Float, w: Float, out: EulerRotation = EulerRotation()): EulerRotation {
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
            return out.setTo(roll.radians, pitch.radians, yaw.radians)
        }
    }

    operator fun get(index: Int): Double = when (index) {
        0 -> x
        1 -> y
        2 -> z
        3 -> w
        else -> Double.NaN
    }
    inline fun setToFunc(callback: (Int) -> Double) = setTo(callback(0), callback(1), callback(2), callback(3))
    fun setTo(x: Double, y: Double, z: Double, w: Double): Quaternion {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }
    fun setTo(x: Int, y: Int, z: Int, w: Int): Quaternion = setTo(x.toDouble(), y.toDouble(), z.toDouble(), w.toDouble())
    fun setTo(x: Float, y: Float, z: Float, w: Float): Quaternion = setTo(x.toDouble(), y.toDouble(), z.toDouble(), w.toDouble())
    fun setTo(euler: EulerRotation): Quaternion = EulerRotation.toQuaternion(euler, this)
    fun setTo(other: Quaternion): Quaternion = setTo(other.x, other.y, other.z, other.w)

    fun setEuler(x: Angle, y: Angle, z: Angle): Quaternion = EulerRotation.toQuaternion(x, y, z, this)
    fun setEuler(euler: EulerRotation): Quaternion = EulerRotation.toQuaternion(euler, this)

    fun copyFrom(other: Quaternion): Quaternion = this.setTo(other)

    operator fun unaryMinus(): Quaternion = Quaternion(-x, -y, -z, -w)
    operator fun plus(other: Quaternion): Quaternion = Quaternion(x + other.x, y + other.y, z + other.z, w + other.w)
    operator fun minus(other: Quaternion): Quaternion = Quaternion(x - other.x, y - other.y, z - other.z, w - other.w)
    operator fun times(scale: Double): Quaternion = Quaternion(x * scale, y * scale, z * scale, w * scale)

    fun negate() = this.setTo(-x, -y, -z, -w)

    inline fun setToFunc(l: Quaternion, r: Quaternion, func: (l: Double, r: Double) -> Double) = setTo(
        func(l.x, r.x),
        func(l.y, r.y),
        func(l.z, r.z),
        func(l.w, r.w)
    )
    fun setToSlerp(left: Quaternion, right: Quaternion, t: Double, tleft: Quaternion = Quaternion(), tright: Quaternion = Quaternion()): Quaternion {
        val tleft = tleft.copyFrom(left).normalize()
        val tright = tright.copyFrom(right).normalize()

        var dot = dotProduct(tleft, right)

        if (dot < 0.0f) {
            tright.negate()
            dot = -dot
        }

        if (dot > 0.99995f) return setToFunc(tleft, tright) { l, r -> l + t * (r - l) }

        val angle0 = acos(dot)
        val angle1 = angle0 * t

        val s1 = sin(angle1) / sin(angle0)
        val s0 = cos(angle1) - dot * s1

        return setToFunc(tleft, tright) { l, r -> (s0 * l) + (s1 * r) }
    }

    fun setToNlerp(left: Quaternion, right: Quaternion, t: Double): Quaternion {
        val sign = if (dotProduct(left, right) < 0) -1 else +1
        return setToFunc { (1f - t) * left[it] + t * right[it] * sign }.normalize()
    }

    fun setToInterpolated(left: Quaternion, right: Quaternion, t: Double): Quaternion = setToSlerp(left, right, t)

    fun setFromRotationMatrix(m: Matrix3D) = this.apply {
        val q = this
        m.apply {
            val t = v00 + v11 + v22
            when {
                t > 0 -> {
                    val s = 0.5 / sqrt(t + 1.0)
                    q.setTo(((v21 - v12) * s), ((v02 - v20) * s), ((v10 - v01) * s), (0.25 / s))
                }
                v00 > v11 && v00 > v22 -> {
                    val s = 2.0 * sqrt(1.0 + v00 - v11 - v22)
                    q.setTo((0.25 * s), ((v01 + v10) / s), ((v02 + v20) / s), ((v21 - v12) / s))
                }
                v11 > v22 -> {
                    val s = 2.0 * sqrt(1.0 + v11 - v00 - v22)
                    q.setTo(((v01 + v10) / s), (0.25 * s), ((v12 + v21) / s), ((v02 - v20) / s))
                }
                else -> {
                    val s = 2.0 * sqrt(1.0 + v22 - v00 - v11)
                    q.setTo(((v02 + v20) / s), ((v12 + v21) / s), (0.25f * s), ((v10 - v01) / s))
                }
            }
        }
    }

    fun normalize(v: Quaternion = this): Quaternion {
        val length = 1.0 / Vector3D.length(v.x, v.y, v.z, v.w)
        return this.setTo(v.x / length, v.y / length, v.z / length, v.w / length)
    }
    fun toMatrix(out: Matrix3D = Matrix3D()): Matrix3D = out.multiply(
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
}

operator fun Double.times(scale: Quaternion): Quaternion = scale.times(this)
