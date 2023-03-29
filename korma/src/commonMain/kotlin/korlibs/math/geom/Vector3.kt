package korlibs.math.geom

import korlibs.memory.pack.*
import korlibs.math.annotations.*
import korlibs.math.internal.*
import korlibs.math.interpolation.*
import korlibs.math.math.*
import kotlin.math.*

inline class Vector3(val data: Float4Pack) {
    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val ONE = Vector3(1f, 1f, 1f)

        val FORWARD	= Vector3(0f, 0f, 1f)
        val BACK = Vector3(0f, 0f, -1f)
        val LEFT = Vector3(-1f, 0f, 0f)
        val RIGHT = Vector3(1f, 0f, 0f)
        val UP = Vector3(0f, 1f, 0f)
        val DOWN = Vector3(0f, -1f, 0f)

        fun cross(a: Vector3, b: Vector3): Vector3 = Vector3(
            (a.y * b.z - a.z * b.y),
            (a.z * b.x - a.x * b.z),
            (a.x * b.y - a.y * b.x),
        )

        fun length(x: Float, y: Float, z: Float): Float = sqrt(lengthSq(x, y, z))
        fun lengthSq(x: Float, y: Float, z: Float): Float = x * x + y * y + z * z
    }

    constructor(x: Float, y: Float, z: Float) : this(float4PackOf(x, y, z, 0f))
    constructor(x: Int, y: Int, z: Int) : this(x.toFloat(), y.toFloat(), z.toFloat())
    constructor(x: Double, y: Double, z: Double) : this(x.toFloat(), y.toFloat(), z.toFloat())

    operator fun component1(): Float = x
    operator fun component2(): Float = y
    operator fun component3(): Float = z

    fun copy(x: Float = this.x, y: Float = this.y, z: Float = this.z): Vector3 = Vector3(x, y, z)

    val x: Float get() = data.f0
    val y: Float get() = data.f1
    val z: Float get() = data.f2

    val lengthSquared: Float get() = (x * x) + (y * y) + (z * z)
    val length: Float get() = sqrt(lengthSquared)
    fun normalized(): Vector3 = this / length

    operator fun get(index: Int): Float = when (index) {
        0 -> x
        1 -> y
        2 -> z
        else -> throw IndexOutOfBoundsException()
    }

    operator fun unaryPlus(): Vector3 = this
    operator fun unaryMinus(): Vector3 = Vector3(-this.x, -this.y, -this.z)

    operator fun plus(v: Vector3): Vector3 = Vector3(this.x + v.x, this.y + v.y, this.z + v.z)
    operator fun minus(v: Vector3): Vector3 = Vector3(this.x - v.x, this.y - v.y, this.z - v.z)

    operator fun times(v: Vector3): Vector3 = Vector3(this.x * v.x, this.y * v.y, this.z * v.z)
    operator fun div(v: Vector3): Vector3 = Vector3(this.x / v.x, this.y / v.y, this.z / v.z)
    operator fun rem(v: Vector3): Vector3 = Vector3(this.x % v.x, this.y % v.y, this.z % v.z)

    operator fun times(v: Float): Vector3 = Vector3(this.x * v, this.y * v, this.z * v)
    operator fun div(v: Float): Vector3 = Vector3(this.x / v, this.y / v, this.z / v)
    operator fun rem(v: Float): Vector3 = Vector3(this.x % v, this.y % v, this.z % v)

    infix fun dot(v: Vector3): Float = (x * v.x) + (y * v.y) + (z * v.z)
    infix fun cross(v: Vector3): Vector3 = cross(this, v)

    /** Vector3 with inverted (1f / v) components to this */
    fun inv(): Vector3 = Vector3(1f / x, 1f / y, 1f / z)

    override fun toString(): String = "Vector3(${x.niceStr}, ${y.niceStr}, ${z.niceStr})"

    fun toVector4(w: Float = 1f): Vector4 = Vector4(x, y, z, w)
    fun isAlmostEquals(other: Vector3, epsilon: Float = 0.00001f): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) && this.y.isAlmostEquals(other.y, epsilon) && this.z.isAlmostEquals(other.z, epsilon)
}

fun vec(x: Float, y: Float, z: Float): Vector3 = Vector3(x, y, z)
fun vec3(x: Float, y: Float, z: Float): Vector3 = Vector3(x, y, z)

@KormaMutableApi
@Deprecated("")
sealed interface IVector3 {
    val x: Float
    val y: Float
    val z: Float

    operator fun get(index: Int): Float = when (index) {
        0 -> x
        1 -> y
        2 -> z
        else -> 0f
    }

}
