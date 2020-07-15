package com.soywiz.korma.geom

fun Matrix3D.copyFrom(that: IMatrix): Matrix3D = that.toMatrix3D(this)

fun IMatrix.toMatrix3D(out: Matrix3D = Matrix3D()): Matrix3D = out.setRows(
    a, c, 0, tx,
    b, d, 0, ty,
    0, 0, 1, 0,
    0, 0, 0, 1
)
