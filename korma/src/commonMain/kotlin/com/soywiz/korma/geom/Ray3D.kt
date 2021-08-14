package com.soywiz.korma.geom

data class Ray3D(val pos: Vector3D, val dir: Vector3D) {
    companion object {
        fun fromPoints(p1: Vector3D, p2: Vector3D): Ray3D = Ray3D(pos = p1, dir = (p2 - p1).normalized())
    }

    fun transformed(mat: Matrix3D): Ray3D =
        Ray3D(mat.transform(pos), mat.extractRotation().transform(dir).normalized())

    override fun toString(): String = "Ray3D(pos=$pos, dir=$dir)"
}
