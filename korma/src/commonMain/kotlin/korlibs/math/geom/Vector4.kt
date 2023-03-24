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

    override fun toString(): String = "Vector4(${x.niceStr}, ${y.niceStr}, ${z.niceStr}, ${w.niceStr})"
}

fun vec(x: Float, y: Float, z: Float, w: Float): Vector4 = Vector4(x, y, z, w)
fun vec4(x: Float, y: Float, z: Float, w: Float = 1f): Vector4 = Vector4(x, y, z, w)

typealias MVector3D = MVector4

/*
sealed interface IVector3 {
    val x: Float
    val y: Float
    val z: Float
}

sealed interface Vector3 : IVector3 {
    override var x: Float
    override var y: Float
    override var z: Float
}

interface IVector4 : IVector3 {
    val w: Float
}

interface Vector4 : Vector3, IVector4 {
    override var w: Float
}
*/

@KormaMutableApi
@Deprecated("")
sealed interface IVector4 {
    val x: Float
    val y: Float
    val z: Float
    val w: Float

    operator fun get(index: Int): Float = when (index) {
        0 -> x
        1 -> y
        2 -> z
        3 -> w
        else -> 0f
    }
}

// @TODO: To inline class wrapping FloatArray?
@KormaMutableApi
@Deprecated("")
class MVector4 : IVector4 {
    val data = floatArrayOf(0f, 0f, 0f, 1f)

    override var x: Float get() = data[0]; set(value) { data[0] = value }
    override var y: Float get() = data[1]; set(value) { data[1] = value }
    override var z: Float get() = data[2]; set(value) { data[2] = value }
    override var w: Float get() = data[3]; set(value) { data[3] = value }

    val lengthSquared: Float get() = (x * x) + (y * y) + (z * z) + (w * w)
    val length: Float get() = sqrt(lengthSquared)

    val length3Squared: Float get() = (x * x) + (y * y) + (z * z)
    val length3: Float get() = sqrt(length3Squared)

    override operator fun get(index: Int): Float = data[index]
    operator fun set(index: Int, value: Float) { data[index] = value }

    companion object {
        operator fun invoke(x: Float, y: Float, z: Float, w: Float = 1f): MVector4 = MVector4().setTo(x, y, z, w)
        operator fun invoke(x: Double, y: Double, z: Double, w: Double = 1.0): MVector4 = MVector4().setTo(x, y, z, w)
        operator fun invoke(x: Int, y: Int, z: Int, w: Int = 1): MVector4 = MVector4().setTo(x, y, z, w)

        fun length(x: Double, y: Double, z: Double, w: Double): Double = sqrt(lengthSq(x, y, z, w))
        fun length(x: Double, y: Double, z: Double): Double = sqrt(lengthSq(x, y, z))
        fun length(x: Float, y: Float, z: Float, w: Float): Float = sqrt(lengthSq(x, y, z, w))
        fun length(x: Float, y: Float, z: Float): Float = sqrt(lengthSq(x, y, z))

        fun lengthSq(x: Double, y: Double, z: Double, w: Double): Double = x * x + y * y + z * z + w * w
        fun lengthSq(x: Double, y: Double, z: Double): Double = x * x + y * y + z * z
        fun lengthSq(x: Float, y: Float, z: Float, w: Float): Float = x * x + y * y + z * z + w * w
        fun lengthSq(x: Float, y: Float, z: Float): Float = x * x + y * y + z * z
    }

    fun copyFrom(other: MVector4) = setTo(other.x, other.y, other.z, other.w)

    fun setTo(x: Float, y: Float, z: Float, w: Float): MVector4 = this.apply { this.x = x; this.y = y; this.z = z; this.w = w }
    fun setTo(x: Double, y: Double, z: Double, w: Double): MVector4 = setTo(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    fun setTo(x: Int, y: Int, z: Int, w: Int): MVector4 = setTo(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    fun setTo(x: Float, y: Float, z: Float): MVector4 = setTo(x, y, z, 1f)
    fun setTo(x: Double, y: Double, z: Double): MVector4 = setTo(x, y, z, 1.0)
    fun setTo(x: Int, y: Int, z: Int): MVector4 = setTo(x, y, z, 1)

    inline fun setToFunc(func: (index: Int) -> Float): MVector4 = setTo(func(0), func(1), func(2), func(3))
    inline fun setToFunc(l: MVector4, r: MVector4, func: (l: Float, r: Float) -> Float) = setTo(
        func(l.x, r.x),
        func(l.y, r.y),
        func(l.z, r.z),
        func(l.w, r.w)
    )
    fun setToInterpolated(left: MVector4, right: MVector4, t: Double): MVector4 = setToFunc { t.toRatio().interpolate(left[it], right[it]) }

    fun scale(scale: Float) = this.setTo(this.x * scale, this.y * scale, this.z * scale, this.w * scale)
    fun scale(scale: Int) = scale(scale.toFloat())
    fun scale(scale: Double) = scale(scale.toFloat())

    fun transform(mat: MMatrix3D) = mat.transform(this, this)

    fun normalize(vector: MVector4 = this): MVector4 = this.apply {
        val norm = 1.0 / vector.length3
        setTo(vector.x * norm, vector.y * norm, vector.z * norm, 1.0)
    }

    fun normalized(out: MVector4 = MVector4()): MVector4 = out.copyFrom(this).normalize()

    fun dot(v2: MVector4): Float = this.x*v2.x + this.y*v2.y + this.z*v2.z

    operator fun plus(that: MVector4): MVector4 = MVector4(this.x + that.x, this.y + that.y, this.z + that.z, this.w + that.w)
    operator fun minus(that: MVector4): MVector4 = MVector4(this.x - that.x, this.y - that.y, this.z - that.z, this.w - that.w)
    operator fun times(scale: Float): MVector4 = MVector4(x * scale, y * scale, z * scale, w * scale)

    fun sub(l: MVector4, r: MVector4): MVector4 = setTo(l.x - r.x, l.y - r.y, l.z - r.z, l.w - r.w)
    fun add(l: MVector4, r: MVector4): MVector4 = setTo(l.x + r.x, l.y + r.y, l.z + r.z, l.w + r.w)
    fun cross(a: MVector4, b: MVector4): MVector4 = setTo(
        (a.y * b.z - a.z * b.y),
        (a.z * b.x - a.x * b.z),
        (a.x * b.y - a.y * b.x),
        1f
    )
    override fun equals(other: Any?): Boolean = (other is MVector4) && almostEquals(this.x, other.x) && almostEquals(this.y, other.y) && almostEquals(this.z, other.z) && almostEquals(this.w, other.w)
    override fun hashCode(): Int = data.contentHashCode()

    override fun toString(): String = if (w == 1f) "(${x.niceStr}, ${y.niceStr}, ${z.niceStr})" else "(${x.niceStr}, ${y.niceStr}, ${z.niceStr}, ${w.niceStr})"
}

@KormaMutableApi
sealed interface IVector4Int {
    val x: Int
    val y: Int
    val z: Int
    val w: Int
}

@KormaMutableApi
inline class MVector4Int(val v: MVector4) : IVector4Int {
    override val x: Int get() = v.x.toInt()
    override val y: Int get() = v.y.toInt()
    override val z: Int get() = v.z.toInt()
    override val w: Int get() = v.w.toInt()
}

fun MVector4.asIntVector3D() = MVector4Int(this)

typealias Position3D = MVector4
typealias Scale3D = MVector4