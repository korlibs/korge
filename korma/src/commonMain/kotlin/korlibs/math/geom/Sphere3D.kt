package korlibs.math.geom

import korlibs.math.annotations.*

@KormaValueApi
data class Sphere3D(val origin: Vector3, val radius: Float)

@KormaMutableApi
sealed interface ISphere3D {
    val origin: IVector3
    val radius: Float
}

@KormaMutableApi
data class MSphere3D(override var origin: MVector3, override var radius: Float) : ISphere3D