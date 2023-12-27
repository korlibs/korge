package korlibs.math.geom

import korlibs.math.*
import korlibs.math.geom.shape.*

//inline class Sphere3D private constructor(private val data: Float4) : Shape3D {
data class Sphere3D(override val center: Vector3F, val radius: Float) : Shape3D {
    //constructor(center: Vector3, radius: Float) : this(Float4(center.x, center.y, center.z, radius))
    //override val center: Vector3 get() = Vector3(data.x, data.y, data.z)
    //val radius: Float get() = data.w

    override val volume: Float get() = ((4f / 3f) * PIF) * (radius * radius * radius)
}
