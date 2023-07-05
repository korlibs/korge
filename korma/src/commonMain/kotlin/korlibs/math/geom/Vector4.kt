package korlibs.math.geom

import korlibs.memory.pack.*
import korlibs.math.internal.*
import korlibs.math.isAlmostEquals
import korlibs.memory.*
import kotlin.math.*

//@KormaValueApi
//inline class Vector4(val data: Float4) {
data class Vector4(val x: Float, val y: Float, val z: Float, val w: Float) {
    //operator fun component1(): Float = x
    //operator fun component2(): Float = y
    //operator fun component3(): Float = z
    //operator fun component4(): Float = w
    //val x: Float get() = data.f0
    //val y: Float get() = data.f1
    //val z: Float get() = data.f2
    //val w: Float get() = data.f3
    //fun copy(x: Float = this.x, y: Float = this.y, z: Float = this.z, w: Float = this.w): Vector4 = Vector4(x, y, z, w)

    companion object {
        val ZERO = Vector4(0f, 0f, 0f, 0f)
        val ONE = Vector4(1f, 1f, 1f, 1f)

        operator fun invoke(): Vector4 = Vector4.ZERO

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

        inline fun func(func: (index: Int) -> Float): Vector4 = Vector4(func(0), func(1), func(2), func(3))
    }

    constructor(xyz: Vector3, w: Float) : this(xyz.x, xyz.y, xyz.z, w)
    //constructor(x: Float, y: Float, z: Float, w: Float) : this(float4PackOf(x, y, z, w))
    constructor(x: Int, y: Int, z: Int, w: Int) : this(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    constructor(x: Double, y: Double, z: Double, w: Double) : this(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    val xyz: Vector3 get() = Vector3(x, y, z)

    val length3Squared: Float get() = (x * x) + (y * y) + (z * z)
    /** Only taking into accoount x, y, z */
    val length3: Float get() = sqrt(length3Squared)

    val lengthSquared: Float get() = (x * x) + (y * y) + (z * z) + (w * w)
    val length: Float get() = sqrt(lengthSquared)

    fun normalized(): Vector4 {
        val length = this.length
        if (length == 0f) return Vector4.ZERO
        return this / length
    }

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

    fun copyTo(out: FloatArray, offset: Int = 0): FloatArray {
        out[offset + 0] = x
        out[offset + 1] = y
        out[offset + 2] = z
        out[offset + 3] = w
        return out
    }

    /** Vector4 with inverted (1f / v) components to this */
    fun inv(): Vector4 = Vector4(1f / x, 1f / y, 1f / z, 1f / w)

    fun isNaN(): Boolean = this.x.isNaN() && this.y.isNaN() && this.z.isNaN() && this.w.isNaN()
    val absoluteValue: Vector4 get() = Vector4(abs(x), abs(y), abs(z), abs(w))

    override fun toString(): String = "Vector4(${x.niceStr}, ${y.niceStr}, ${z.niceStr}, ${w.niceStr})"

    // @TODO: Should we scale Vector3 by w?
    fun toVector3(): Vector3 = Vector3(x, y, z)
    fun isAlmostEquals(other: Vector4, epsilon: Float = 0.00001f): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) && this.y.isAlmostEquals(other.y, epsilon) && this.z.isAlmostEquals(other.z, epsilon) && this.w.isAlmostEquals(other.w, epsilon)
}

fun vec(x: Float, y: Float, z: Float, w: Float): Vector4 = Vector4(x, y, z, w)
fun vec4(x: Float, y: Float, z: Float, w: Float = 1f): Vector4 = Vector4(x, y, z, w)

fun abs(a: Vector4): Vector4 = a.absoluteValue
fun min(a: Vector4, b: Vector4): Vector4 = Vector4(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z), min(a.w, b.w))
fun max(a: Vector4, b: Vector4): Vector4 = Vector4(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z), max(a.w, b.w))
fun Vector4.clamp(min: Float, max: Float): Vector4 = Vector4(x.clamp(min, max), y.clamp(min, max), z.clamp(min, max), w.clamp(min, max))
fun Vector4.clamp(min: Double, max: Double): Vector4 = clamp(min.toFloat(), max.toFloat())
fun Vector4.clamp(min: Vector4, max: Vector4): Vector4 = Vector4(x.clamp(min.x, max.x), y.clamp(min.y, max.y), z.clamp(min.z, max.z), w.clamp(min.w, max.w))
