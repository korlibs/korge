package korlibs.math.geom

import korlibs.math.annotations.KormaMutableApi

@KormaMutableApi
sealed interface IRay3D {
    val pos: IVector4
    val dir: IVector4
}

@KormaMutableApi
data class MRay3D(override val pos: MVector4, override val dir: MVector4) : IRay3D {
    companion object {
        fun fromPoints(p1: MVector4, p2: MVector4): MRay3D = MRay3D(pos = p1, dir = (p2 - p1).normalized())
    }

    fun transformed(mat: MMatrix3D): MRay3D =
        MRay3D(mat.transform(pos), mat.extractRotation().transform(dir).normalized())

    override fun toString(): String = "Ray3D(pos=$pos, dir=$dir)"
}