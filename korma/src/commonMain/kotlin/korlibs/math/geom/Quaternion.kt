package korlibs.math.geom

import korlibs.memory.pack.*
import kotlin.math.*

//@KormaValueApi
inline class Quaternion private constructor(val data: Float4Pack) {
    val x: Float get() = data.f0
    val y: Float get() = data.f1
    val z: Float get() = data.f2
    val w: Float get() = data.f3
    val vector: Vector4 get() = Vector4(data)
    val xyz: Vector3 get() = Vector3(x, y, z)

    operator fun component1(): Float = x
    operator fun component2(): Float = y
    operator fun component3(): Float = z
    operator fun component4(): Float = w

    constructor(vector: Vector4, unit: Unit = Unit) : this(vector.data)
    constructor(x: Float, y: Float, z: Float, w: Float) : this(float4PackOf(x, y, z, w))

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

    companion object {
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
    }
}

operator fun Double.times(scale: MQuaternion): MQuaternion = scale.times(this)
