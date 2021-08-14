package com.soywiz.korma.geom

import com.soywiz.korma.internal.*
import com.soywiz.korma.interpolation.*
import com.soywiz.korma.math.*
import kotlin.math.*

interface IVector3 {
    val x: Float
    val y: Float
    val z: Float
}

operator fun IVector3.get(index: Int) = when (index) {
    0 -> x
    1 -> y
    2 -> z
    else -> 0f
}

interface MVector3 : IVector3 {
    override var x: Float
    override var y: Float
    override var z: Float
}

operator fun MVector3.set(index: Int, value: Float) {
    when (index) {
        0 -> x = value
        1 -> y = value
        2 -> z = value
    }
}

fun vec(x: Float, y: Float, z: Float) = Vector3(x, y, z)

class Vector3 : MVector3 {
    val data: FloatArray = FloatArray(3)

    override var x: Float get() = data[0]; set(value) { data[0] = value }
    override var y: Float get() = data[1]; set(value) { data[1] = value }
    override var z: Float get() = data[2]; set(value) { data[2] = value }

    val lengthSquared: Float get() = (x * x) + (y * y) + (z * z)
    val length: Float get() = sqrt(lengthSquared)

    operator fun get(index: Int): Float = data[index]
    operator fun set(index: Int, value: Float) = run { data[index] = value }

    companion object {
        operator fun invoke(x: Float, y: Float, z: Float): Vector3 = Vector3().setTo(x, y, z)
        operator fun invoke(x: Double, y: Double, z: Double): Vector3 = Vector3().setTo(x, y, z)
        operator fun invoke(x: Int, y: Int, z: Int): Vector3 = Vector3().setTo(x, y, z)

        fun length(x: Double, y: Double, z: Double): Double = sqrt(lengthSq(x, y, z))
        fun length(x: Float, y: Float, z: Float): Float = sqrt(lengthSq(x, y, z))

        fun lengthSq(x: Double, y: Double, z: Double): Double = x * x + y * y + z * z
        fun lengthSq(x: Float, y: Float, z: Float): Float = x * x + y * y + z * z
    }

    fun setTo(x: Float, y: Float, z: Float): Vector3 {
        this.x = x
        this.y = y
        this.z = z
        return this
    }
    fun setTo(x: Double, y: Double, z: Double): Vector3 = setTo(x.toFloat(), y.toFloat(), z.toFloat())
    fun setTo(x: Int, y: Int, z: Int): Vector3 = setTo(x.toFloat(), y.toFloat(), z.toFloat())

    inline fun setToFunc(func: (index: Int) -> Float): Vector3 = setTo(func(0), func(1), func(2))
    inline fun setToFunc(l: Vector3D, r: Vector3D, func: (l: Float, r: Float) -> Float) = setTo(
        func(l.x, r.x),
        func(l.y, r.y),
        func(l.z, r.z),
    )
    fun setToInterpolated(left: Vector3D, right: Vector3D, t: Double): Vector3 = setToFunc { t.interpolate(left[it], right[it]) }

    fun copyFrom(other: Vector3) = setTo(other.x, other.y, other.z)

    fun scale(scale: Float) = this.setTo(this.x * scale, this.y * scale, this.z * scale)
    fun scale(scale: Int) = scale(scale.toFloat())
    fun scale(scale: Double) = scale(scale.toFloat())

    fun transform(mat: Matrix3D) = mat.transform(this, this)
    fun transformed(mat: Matrix3D, out: Vector3 = Vector3()) = mat.transform(this, out)

    fun normalize(vector: Vector3 = this): Vector3 {
        val norm = 1.0 / vector.length
        setTo(vector.x * norm, vector.y * norm, vector.z * norm)
        return this
    }

    fun normalized(out: Vector3 = Vector3()): Vector3 = out.copyFrom(this).normalize()

    fun dot(v2: Vector3D): Float = this.x*v2.x + this.y*v2.y + this.z*v2.y

    operator fun plus(that: Vector3D) = Vector3D(this.x + that.x, this.y + that.y, this.z + that.z)
    operator fun minus(that: Vector3D) = Vector3D(this.x - that.x, this.y - that.y, this.z - that.z)
    operator fun times(scale: Float) = Vector3D(x * scale, y * scale, z * scale)

    fun sub(l: Vector3, r: Vector3) = setTo(l.x - r.x, l.y - r.y, l.z - r.z)
    fun add(l: Vector3, r: Vector3) = setTo(l.x + r.x, l.y + r.y, l.z + r.z)
    fun cross(a: Vector3, b: Vector3) = setTo(
        (a.y * b.z - a.z * b.y),
        (a.z * b.x - a.x * b.z),
        (a.x * b.y - a.y * b.x),
    )

    fun clone() = Vector3(x, y, z)

    override fun equals(other: Any?): Boolean = (other is Vector3) && almostEquals(this.x, other.x) && almostEquals(this.y, other.y) && almostEquals(this.z, other.z)
    override fun hashCode(): Int = data.contentHashCode()

    override fun toString(): String = "(${x.niceStr}, ${y.niceStr}, ${z.niceStr})"
}
