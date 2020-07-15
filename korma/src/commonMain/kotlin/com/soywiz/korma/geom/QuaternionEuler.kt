package com.soywiz.korma.geom

import kotlin.math.*

data class EulerRotation(
    var x: Angle = 0.degrees,
    var y: Angle = 0.degrees,
    var z: Angle = 0.degrees
) {
    companion object
}

data class Quaternion(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var z: Double = 0.0,
    var w: Double = 1.0
) {
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

    companion object {
        fun dotProduct(l: Quaternion, r: Quaternion): Double = l.x * r.x + l.y * r.y + l.z * r.z + l.w * r.w
    }

    fun normalize(v: Quaternion = this): Quaternion {
        val length = 1.0 / Vector3D.length(v.x, v.y, v.z, v.w)
        return this.setTo(v.x / length, v.y / length, v.z / length, v.w / length)
    }
}

@Deprecated("Kotlin/Native boxes inline + Number")
inline fun Quaternion(x: Number, y: Number, z: Number, w: Number) = Quaternion(x.toDouble(), y.toDouble(), z.toDouble(), w.toDouble())
fun Quaternion(x: Float, y: Float, z: Float, w: Float) = Quaternion(x.toDouble(), y.toDouble(), z.toDouble(), w.toDouble())
fun Quaternion(x: Int, y: Int, z: Int, w: Int) = Quaternion(x.toDouble(), y.toDouble(), z.toDouble(), w.toDouble())

@Deprecated("Kotlin/Native boxes inline + Number")
inline fun EulerRotation.setQuaternion(x: Number, y: Number, z: Number, w: Number): EulerRotation = quaternionToEuler(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat(), this)
fun EulerRotation.setQuaternion(x: Double, y: Double, z: Double, w: Double): EulerRotation = quaternionToEuler(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat(), this)
fun EulerRotation.setQuaternion(x: Int, y: Int, z: Int, w: Int): EulerRotation = quaternionToEuler(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat(), this)
fun EulerRotation.setQuaternion(x: Float, y: Float, z: Float, w: Float): EulerRotation = quaternionToEuler(x, y, z, w, this)

fun EulerRotation.setQuaternion(quaternion: Quaternion): EulerRotation = quaternionToEuler(quaternion.x, quaternion.y, quaternion.z, quaternion.w, this)
fun EulerRotation.setTo(x: Angle, y: Angle, z: Angle): EulerRotation = this
    .apply { this.x = x }
    .apply { this.y = y }
    .apply { this.z = z }

fun EulerRotation.setTo(other: EulerRotation): EulerRotation = setTo(other.x, other.y, other.z)

fun Quaternion.setEuler(x: Angle, y: Angle, z: Angle): Quaternion = eulerToQuaternion(x, y, z, this)
fun Quaternion.setEuler(euler: EulerRotation): Quaternion = eulerToQuaternion(euler, this)
fun Quaternion.setTo(euler: EulerRotation): Quaternion = eulerToQuaternion(euler, this)

@Deprecated("Kotlin/Native boxes inline + Number")
inline fun Quaternion.setTo(x: Number, y: Number, z: Number, w: Number): Quaternion = setTo(x.toDouble(), y.toDouble(), z.toDouble(), w.toDouble())
fun Quaternion.setTo(x: Int, y: Int, z: Int, w: Int): Quaternion = setTo(x.toDouble(), y.toDouble(), z.toDouble(), w.toDouble())
fun Quaternion.setTo(x: Float, y: Float, z: Float, w: Float): Quaternion = setTo(x.toDouble(), y.toDouble(), z.toDouble(), w.toDouble())

inline fun Quaternion.copyFrom(other: Quaternion): Quaternion = this.setTo(other)

inline fun Quaternion.setTo(other: Quaternion): Quaternion = setTo(other.x, other.y, other.z, other.w)

private val tempQuat = Quaternion()
fun EulerRotation.toMatrix(out: Matrix3D = Matrix3D()): Matrix3D = tempQuat.setEuler(this).toMatrix(out)
fun Quaternion.toMatrix(out: Matrix3D = Matrix3D()): Matrix3D = quaternionToMatrix(this, out)

fun eulerToQuaternion(euler: EulerRotation, quaternion: Quaternion = Quaternion()): Quaternion = eulerToQuaternion(euler.x, euler.y, euler.z, quaternion)

fun quaternionToEuler(q: Quaternion, euler: EulerRotation = EulerRotation()): EulerRotation = quaternionToEuler(q.x, q.y, q.z, q.w, euler)

fun quaternionToEuler(x: Double, y: Double, z: Double, w: Double, euler: EulerRotation = EulerRotation()): EulerRotation =
    quaternionToEuler(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat(), euler)
fun quaternionToEuler(x: Int, y: Int, z: Int, w: Int, euler: EulerRotation = EulerRotation()): EulerRotation =
    quaternionToEuler(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat(), euler)

@Deprecated("Kotlin/Native boxes inline + Number")
inline fun quaternionToEuler(x: Number, y: Number, z: Number, w: Number, euler: EulerRotation = EulerRotation()): EulerRotation =
    quaternionToEuler(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat(), euler)

// https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles

fun eulerToQuaternion(roll: Angle, pitch: Angle, yaw: Angle, quaternion: Quaternion = Quaternion()): Quaternion {
    val cr = cos(roll * 0.5)
    val sr = sin(roll * 0.5)
    val cp = cos(pitch * 0.5)
    val sp = sin(pitch * 0.5)
    val cy = cos(yaw * 0.5)
    val sy = sin(yaw * 0.5)
    return quaternion.setTo(
        (cy * cp * sr - sy * sp * cr),
        (sy * cp * sr + cy * sp * cr),
        (sy * cp * cr - cy * sp * sr),
        (cy * cp * cr + sy * sp * sr)
    )
}

fun quaternionToEuler(x: Float, y: Float, z: Float, w: Float, euler: EulerRotation = EulerRotation()): EulerRotation {
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
    return euler.setTo(roll.radians, pitch.radians, yaw.radians)
}

private val tempMat1 = Matrix3D()
private val tempMat2 = Matrix3D()
fun quaternionToMatrix(quat: Quaternion, out: Matrix3D = Matrix3D(), temp1: Matrix3D = tempMat1, temp2: Matrix3D = tempMat2): Matrix3D {
    temp1.setRows(
        quat.w, quat.z, -quat.y, quat.x,
        -quat.z, quat.w, quat.x, quat.y,
        quat.y, -quat.x, quat.w, quat.z,
        -quat.x, -quat.y, -quat.z, quat.w
    )
    temp2.setRows(
        quat.w, quat.z, -quat.y, -quat.x,
        -quat.z, quat.w, quat.x, -quat.y,
        quat.y, -quat.x, quat.w, -quat.z,
        quat.x, quat.y, quat.z, quat.w
    )
    return out.multiply(temp1, temp2)
}

fun Quaternion.setFromRotationMatrix(m: Matrix3D) = this.apply {
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

operator fun Quaternion.unaryMinus(): Quaternion = Quaternion(-x, -y, -z, -w)
operator fun Quaternion.plus(other: Quaternion): Quaternion = Quaternion(x + other.x, y + other.y, z + other.z, w + other.w)
operator fun Quaternion.minus(other: Quaternion): Quaternion = Quaternion(x - other.x, y - other.y, z - other.z, w - other.w)
operator fun Quaternion.times(scale: Double): Quaternion = Quaternion(x * scale, y * scale, z * scale, w * scale)
operator fun Double.times(scale: Quaternion): Quaternion = scale.times(this)

fun Quaternion.negate() = this.setTo(-x, -y, -z, -w)

fun Quaternion.setToFunc(l: Quaternion, r: Quaternion, func: (l: Double, r: Double) -> Double) = setTo(
    func(l.x, r.x),
    func(l.y, r.y),
    func(l.z, r.z),
    func(l.w, r.w)
)

fun Vector3D.setToFunc(l: Vector3D, r: Vector3D, func: (l: Float, r: Float) -> Float) = setTo(
    func(l.x, r.x),
    func(l.y, r.y),
    func(l.z, r.z),
    func(l.w, r.w)
)

// @TODO: Allocations and temps!
private val tleft: Quaternion = Quaternion()
private val tright: Quaternion = Quaternion()
fun Quaternion.setToSlerp(left: Quaternion, right: Quaternion, t: Double): Quaternion {
    val tleft = tleft.copyFrom(left).normalize()
    val tright = tright.copyFrom(right).normalize()

    var dot = Quaternion.dotProduct(tleft, right)

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

fun Quaternion.setToNlerp(left: Quaternion, right: Quaternion, t: Double): Quaternion {
    val sign = if (Quaternion.dotProduct(left, right) < 0) -1 else +1
    return setToFunc { (1f - t) * left[it] + t * right[it] * sign }.normalize()
}

fun Quaternion.setToInterpolated(left: Quaternion, right: Quaternion, t: Double): Quaternion = setToSlerp(left, right, t)
