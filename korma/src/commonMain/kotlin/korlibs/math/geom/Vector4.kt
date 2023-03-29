package korlibs.math.geom

import korlibs.memory.pack.*
import korlibs.math.annotations.*
import korlibs.math.internal.*
import korlibs.math.interpolation.*
import korlibs.math.math.*
import kotlin.math.*

//@KormaValueApi
inline class Vector4(val data: Float4Pack) {
    companion object {
        val ZERO = Vector4(0f, 0f, 0f, 0f)
        val ONE = Vector4(1f, 1f, 1f, 1f)

        //fun cross(a: Vector4, b: Vector4): Vector4 = Vector4(
        //    (a.y * b.z - a.z * b.y),
        //    (a.z * b.x - a.x * b.z),
        //    (a.x * b.y - a.y * b.x),
        //    1f
        //)

        //fun cross(v1: Vector4, v2: Vector4, v3: Vector4): Vector4 = TODO()
        fun fromArray(array: FloatArray, offset: Int = 0): Vector4 = Vector4(array[offset + 0], array[offset + 1], array[offset + 2], array[offset + 3])

        fun length(x: Float, y: Float, z: Float, w: Float): Float = sqrt(lengthSq(x, y, z, w))
        fun lengthSq(x: Float, y: Float, z: Float, w: Float): Float = x * x + y * y + z * z + w * w
    }

    constructor(xyz: Vector3, w: Float) : this(float4PackOf(xyz.x, xyz.y, xyz.z, w))
    constructor(x: Float, y: Float, z: Float, w: Float) : this(float4PackOf(x, y, z, w))
    constructor(x: Int, y: Int, z: Int, w: Int) : this(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    constructor(x: Double, y: Double, z: Double, w: Double) : this(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    operator fun component1(): Float = x
    operator fun component2(): Float = y
    operator fun component3(): Float = z
    operator fun component4(): Float = w

    val x: Float get() = data.f0
    val y: Float get() = data.f1
    val z: Float get() = data.f2
    val w: Float get() = data.f3

    val xyz: Vector3 get() = Vector3(x, y, z)

    val length3Squared: Float get() = (x * x) + (y * y) + (z * z)
    /** Only taking into accoount x, y, z */
    val length3: Float get() = sqrt(length3Squared)

    val lengthSquared: Float get() = (x * x) + (y * y) + (z * z) + (w * w)
    val length: Float get() = sqrt(lengthSquared)

    fun normalized(): Vector4 = this / length

    operator fun get(index: Int): Float = when (index) {
        0 -> x
        1 -> y
        2 -> z
        3 -> w
        else -> throw IndexOutOfBoundsException()
    }

    operator fun unaryPlus(): Vector4 = this
    operator fun unaryMinus(): Vector4 = Vector4(-x, -y, -z, -w)

    operator fun plus(v: Vector4): Vector4 = Vector4(x + v.x, y + v.y, z + v.z, w + v.w)
    operator fun minus(v: Vector4): Vector4 = Vector4(x - v.x, y - v.y, z - v.z, w - v.w)

    operator fun times(v: Vector4): Vector4 = Vector4(x * v.x, y * v.y, z * v.z, w * v.w)
    operator fun div(v: Vector4): Vector4 = Vector4(x / v.x, y / v.y, z / v.z, w / v.w)
    operator fun rem(v: Vector4): Vector4 = Vector4(x % v.x, y % v.y, z % v.z, w % v.w)

    operator fun times(v: Float): Vector4 = Vector4(x * v, y * v, z * v, w * v)
    operator fun div(v: Float): Vector4 = Vector4(x / v, y / v, z / v, w / v)
    operator fun rem(v: Float): Vector4 = Vector4(x % v, y % v, z % v, w % v)
    
    infix fun dot(v: Vector4): Float = (x * v.x) + (y * v.y) + (z * v.z) + (w * v.w)
    //infix fun cross(v: Vector4): Vector4 = cross(this, v)

    fun copy(x: Float = this.x, y: Float = this.y, z: Float = this.z, w: Float = this.w): Vector4 = Vector4(x, y, z, w)

    fun copyTo(out: FloatArray, offset: Int = 0): FloatArray {
        out[offset + 0] = x
        out[offset + 1] = y
        out[offset + 2] = z
        out[offset + 3] = w
        return out
    }

    /** Vector4 with inverted (1f / v) components to this */
    fun inv(): Vector4 = Vector4(1f / x, 1f / y, 1f / z, 1f / w)

    override fun toString(): String = "Vector4(${x.niceStr}, ${y.niceStr}, ${z.niceStr}, ${w.niceStr})"

    // @TODO: Should we scale Vector3 by w?
    fun toVector3(): Vector3 = Vector3(x, y, z)
    fun isAlmostEquals(other: Vector4, epsilon: Float = 0.00001f): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) && this.y.isAlmostEquals(other.y, epsilon) && this.z.isAlmostEquals(other.z, epsilon) && this.w.isAlmostEquals(other.w, epsilon)
}

fun vec(x: Float, y: Float, z: Float, w: Float): Vector4 = Vector4(x, y, z, w)
fun vec4(x: Float, y: Float, z: Float, w: Float = 1f): Vector4 = Vector4(x, y, z, w)
