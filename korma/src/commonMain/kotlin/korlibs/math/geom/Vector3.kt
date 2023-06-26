package korlibs.math.geom

import korlibs.math.*
import korlibs.math.annotations.*
import korlibs.math.internal.*
import korlibs.memory.*
import kotlin.math.*

//inline class Vector3(val data: Float4Pack) {
//data class Vector3(val x: Float, val y: Float, val z: Float, val w: Float) {
data class Vector3(val x: Float, val y: Float, val z: Float) {
    //operator fun component1(): Float = x
    //operator fun component2(): Float = y
    //operator fun component3(): Float = z
    //fun copy(x: Float = this.x, y: Float = this.y, z: Float = this.z): Vector3 = Vector3(x, y, z)
    //val x: Float get() = data.f0
    //val y: Float get() = data.f1
    //val z: Float get() = data.f2

    companion object {
        val NaN = Vector3(Float.NaN, Float.NaN, Float.NaN)

        val ZERO = Vector3(0f, 0f, 0f)
        val ONE = Vector3(1f, 1f, 1f)

        val FORWARD	= Vector3(0f, 0f, 1f)
        val BACK = Vector3(0f, 0f, -1f)
        val LEFT = Vector3(-1f, 0f, 0f)
        val RIGHT = Vector3(1f, 0f, 0f)
        val UP = Vector3(0f, 1f, 0f)
        val DOWN = Vector3(0f, -1f, 0f)

        operator fun invoke(): Vector3 = ZERO

        fun cross(a: Vector3, b: Vector3): Vector3 = Vector3(
            ((a.y * b.z) - (a.z * b.y)),
            ((a.z * b.x) - (a.x * b.z)),
            ((a.x * b.y) - (a.y * b.x)),
        )

        fun length(x: Float, y: Float, z: Float): Float = sqrt(lengthSq(x, y, z))
        fun lengthSq(x: Float, y: Float, z: Float): Float = x * x + y * y + z * z

        fun fromArray(array: FloatArray, offset: Int): Vector3 =
            Vector3(array[offset + 0], array[offset + 1], array[offset + 2])

        inline fun func(func: (index: Int) -> Float): Vector3 = Vector3(func(0), func(1), func(2))
    }

    //constructor(x: Float, y: Float, z: Float) : this(float4PackOf(x, y, z, 0f))
    constructor(x: Int, y: Int, z: Int) : this(x.toFloat(), y.toFloat(), z.toFloat())
    constructor(x: Double, y: Double, z: Double) : this(x.toFloat(), y.toFloat(), z.toFloat())

    val lengthSquared: Float get() = (x * x) + (y * y) + (z * z)
    val length: Float get() = sqrt(lengthSquared)
    fun normalized(): Vector3 {
        val length = this.length
        //if (length.isAlmostZero()) return Vector3.ZERO
        if (length == 0f) return Vector3.ZERO
        return this / length
    }

    // https://math.stackexchange.com/questions/13261/how-to-get-a-reflection-vector
    // 𝑟=𝑑−2(𝑑⋅𝑛)𝑛
    fun reflected(surfaceNormal: Vector3): Vector3 {
        val d = this
        val n = surfaceNormal
        return d - 2f * (d dot n) * n
    }

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

    operator fun times(v: Int): Vector3 = this * v.toFloat()
    operator fun div(v: Int): Vector3 = this / v.toFloat()
    operator fun rem(v: Int): Vector3 = this % v.toFloat()

    operator fun times(v: Double): Vector3 = this * v.toFloat()
    operator fun div(v: Double): Vector3 = this / v.toFloat()
    operator fun rem(v: Double): Vector3 = this % v.toFloat()

    infix fun dot(v: Vector3): Float = (x * v.x) + (y * v.y) + (z * v.z)
    infix fun cross(v: Vector3): Vector3 = cross(this, v)

    /** Vector3 with inverted (1f / v) components to this */
    fun inv(): Vector3 = Vector3(1f / x, 1f / y, 1f / z)

    fun isNaN(): Boolean = this.x.isNaN() && this.y.isNaN() && this.z.isNaN()
    val absoluteValue: Vector3 get() = Vector3(abs(x), abs(y), abs(z))

    override fun toString(): String = "Vector3(${x.niceStr}, ${y.niceStr}, ${z.niceStr})"

    fun toVector4(w: Float = 1f): Vector4 = Vector4(x, y, z, w)
    fun isAlmostEquals(other: Vector3, epsilon: Float = 0.00001f): Boolean =
        this.x.isAlmostEquals(other.x, epsilon) && this.y.isAlmostEquals(other.y, epsilon) && this.z.isAlmostEquals(other.z, epsilon)
}

operator fun Int.times(v: Vector3): Vector3 = v * this
operator fun Float.times(v: Vector3): Vector3 = v * this
operator fun Double.times(v: Vector3): Vector3 = v * this

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

fun abs(a: Vector3): Vector3 = a.absoluteValue
fun min(a: Vector3, b: Vector3): Vector3 = Vector3(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z))
fun max(a: Vector3, b: Vector3): Vector3 = Vector3(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z))
fun Vector3.clamp(min: Float, max: Float): Vector3 = Vector3(x.clamp(min, max), y.clamp(min, max), z.clamp(min, max))
fun Vector3.clamp(min: Double, max: Double): Vector3 = clamp(min.toFloat(), max.toFloat())
fun Vector3.clamp(min: Vector3, max: Vector3): Vector3 = Vector3(x.clamp(min.x, max.x), y.clamp(min.y, max.y), z.clamp(min.z, max.z))

//fun Vector3.toInt(): Vector3Int = Vector3Int(x.toInt(), y.toInt(), z.toInt())
//fun Vector3.toIntCeil(): Vector3Int = Vector3Int(x.toIntCeil(), y.toIntCeil(), z.toIntCeil())
//fun Vector3.toIntRound(): Vector3Int = Vector3Int(x.toIntRound(), y.toIntRound(), z.toIntRound())
//fun Vector3.toIntFloor(): Vector3Int = Vector3Int(x.toIntFloor(), y.toIntFloor(), z.toIntFloor())
