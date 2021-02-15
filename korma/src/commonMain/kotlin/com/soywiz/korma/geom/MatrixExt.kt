package com.soywiz.korma.geom

import kotlin.jvm.*

fun Matrix3D.copyFrom(that: Matrix): Matrix3D = that.toMatrix3D(this)

fun Matrix.toMatrix3D(out: Matrix3D = Matrix3D()): Matrix3D = out.setRows(
    a, c, 0.0, tx,
    b, d, 0.0, ty,
    0.0, 0.0, 1.0, 0.0,
    0.0, 0.0, 0.0, 1.0
)

@JvmName("multiplyNullable")
fun Matrix.multiply(l: Matrix?, r: Matrix?): Matrix {
    when {
        l != null && r != null -> multiply(l, r)
        l != null -> copyFrom(l)
        r != null -> copyFrom(r)
        else -> identity()
    }
    return this
}
