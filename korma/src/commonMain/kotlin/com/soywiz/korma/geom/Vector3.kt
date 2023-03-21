package com.soywiz.korma.geom

import com.soywiz.kmem.pack.*
import com.soywiz.korma.annotations.*
import com.soywiz.korma.internal.*
import com.soywiz.korma.interpolation.*
import com.soywiz.korma.math.*
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

    override fun toString(): String = "Vector3(${x.niceStr}, ${y.niceStr}, ${z.niceStr})"
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

@KormaMutableApi
@Deprecated("")
fun mvec(x: Float, y: Float, z: Float): MVector3 = MVector3(x, y, z)

@KormaMutableApi
@Deprecated("")
class MVector3 : IVector3 {
    val data: FloatArray = FloatArray(3)

    override var x: Float get() = data[0]; set(value) { data[0] = value }
    override var y: Float get() = data[1]; set(value) { data[1] = value }
    override var z: Float get() = data[2]; set(value) { data[2] = value }

    val vector: Vector3 get() = Vector3(x, y, z)

    val lengthSquared: Float get() = (x * x) + (y * y) + (z * z)
    val length: Float get() = sqrt(lengthSquared)

    override operator fun get(index: Int): Float = data[index]
    operator fun set(index: Int, value: Float) { data[index] = value }

    companion object {
        operator fun invoke(x: Float, y: Float, z: Float): MVector3 = MVector3().setTo(x, y, z)
        operator fun invoke(x: Double, y: Double, z: Double): MVector3 = MVector3().setTo(x, y, z)
        operator fun invoke(x: Int, y: Int, z: Int): MVector3 = MVector3().setTo(x, y, z)

        fun length(x: Double, y: Double, z: Double): Double = sqrt(lengthSq(x, y, z))
        fun length(x: Float, y: Float, z: Float): Float = sqrt(lengthSq(x, y, z))

        fun lengthSq(x: Double, y: Double, z: Double): Double = x * x + y * y + z * z
        fun lengthSq(x: Float, y: Float, z: Float): Float = x * x + y * y + z * z
    }

    fun setTo(x: Float, y: Float, z: Float): MVector3 {
        this.x = x
        this.y = y
        this.z = z
        return this
    }
    fun setTo(x: Double, y: Double, z: Double): MVector3 = setTo(x.toFloat(), y.toFloat(), z.toFloat())
    fun setTo(x: Int, y: Int, z: Int): MVector3 = setTo(x.toFloat(), y.toFloat(), z.toFloat())

    inline fun setToFunc(func: (index: Int) -> Float): MVector3 = setTo(func(0), func(1), func(2))
    inline fun setToFunc(l: MVector4, r: MVector4, func: (l: Float, r: Float) -> Float) = setTo(
        func(l.x, r.x),
        func(l.y, r.y),
        func(l.z, r.z),
    )
    fun setToInterpolated(left: MVector4, right: MVector4, t: Double): MVector3 = setToFunc { t.toRatio().interpolate(left[it], right[it]) }

    fun copyFrom(other: MVector3) = setTo(other.x, other.y, other.z)

    fun scale(scale: Float) = this.setTo(this.x * scale, this.y * scale, this.z * scale)
    fun scale(scale: Int) = scale(scale.toFloat())
    fun scale(scale: Double) = scale(scale.toFloat())

    fun transform(mat: MMatrix3D) = mat.transform(this, this)
    fun transformed(mat: MMatrix3D, out: MVector3 = MVector3()) = mat.transform(this, out)

    fun normalize(vector: MVector3 = this): MVector3 {
        val norm = 1.0 / vector.length
        setTo(vector.x * norm, vector.y * norm, vector.z * norm)
        return this
    }

    fun normalized(out: MVector3 = MVector3()): MVector3 = out.copyFrom(this).normalize()

    fun dot(v2: MVector3): Float = (this.x * v2.x) + (this.y * v2.y) + (this.z * v2.y)

    operator fun plus(that: MVector3): MVector3 = MVector3(this.x + that.x, this.y + that.y, this.z + that.z)
    operator fun minus(that: MVector3): MVector3 = MVector3(this.x - that.x, this.y - that.y, this.z - that.z)
    operator fun times(scale: Float): MVector3 = MVector3(x * scale, y * scale, z * scale)

    fun sub(l: MVector3, r: MVector3): MVector3 = setTo(l.x - r.x, l.y - r.y, l.z - r.z)
    fun add(l: MVector3, r: MVector3): MVector3 = setTo(l.x + r.x, l.y + r.y, l.z + r.z)
    fun cross(a: MVector3, b: MVector3): MVector3 = setTo(
        (a.y * b.z - a.z * b.y),
        (a.z * b.x - a.x * b.z),
        (a.x * b.y - a.y * b.x),
    )

    fun clone() = MVector3(x, y, z)

    override fun equals(other: Any?): Boolean = (other is MVector3) && almostEquals(this.x, other.x) && almostEquals(this.y, other.y) && almostEquals(this.z, other.z)
    override fun hashCode(): Int = data.contentHashCode()

    override fun toString(): String = "(${x.niceStr}, ${y.niceStr}, ${z.niceStr})"
}
