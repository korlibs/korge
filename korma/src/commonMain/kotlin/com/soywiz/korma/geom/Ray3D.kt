package com.soywiz.korma.geom

data class Ray3D(val pos: MVector4, val dir: MVector4) {
    companion object {
        fun fromPoints(p1: MVector4, p2: MVector4): Ray3D = Ray3D(pos = p1, dir = (p2 - p1).normalized())
    }

    fun transformed(mat: MMatrix3D): Ray3D =
        Ray3D(mat.transform(pos), mat.extractRotation().transform(dir).normalized())

    override fun toString(): String = "Ray3D(pos=$pos, dir=$dir)"
}
