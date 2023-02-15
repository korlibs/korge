package com.soywiz.korma.geom

import com.soywiz.korma.annotations.*
import com.soywiz.korma.internal.niceStr
import com.soywiz.korma.interpolation.interpolate
import com.soywiz.korma.math.almostEquals
import kotlin.math.sqrt

@KormaValueApi
data class Vector4(val x: Float, val y: Float, val z: Float, val w: Float)

//@Deprecated("Use Vector4")
typealias MVector3D = MVector4

/*
interface IVector3 {
    val x: Float
    val y: Float
    val z: Float
}

interface Vector3 : IVector3 {
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
interface IVector4 {
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
    fun setToInterpolated(left: MVector4, right: MVector4, t: Double): MVector4 = setToFunc { t.interpolate(left[it], right[it]) }

    fun scale(scale: Float) = this.setTo(this.x * scale, this.y * scale, this.z * scale, this.w * scale)
    fun scale(scale: Int) = scale(scale.toFloat())
    fun scale(scale: Double) = scale(scale.toFloat())

    fun transform(mat: MMatrix3D) = mat.transform(this, this)

    fun normalize(vector: MVector4 = this): MVector4 = this.apply {
        val norm = 1.0 / vector.length3
        setTo(vector.x * norm, vector.y * norm, vector.z * norm, 1.0)
    }

    fun normalized(out: MVector4 = MVector4()): MVector4 = out.copyFrom(this).normalize()

    fun dot(v2: MVector4): Float = this.x*v2.x + this.y*v2.y + this.z*v2.y

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
interface IVector4Int {
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
