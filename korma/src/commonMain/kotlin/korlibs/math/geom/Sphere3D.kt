package korlibs.math.geom

import korlibs.math.geom.shape.*
import korlibs.memory.pack.*

inline class Sphere3D private constructor(private val data: Float4Pack) : Shape3D {
    constructor(center: Vector3, radius: Float) : this(Float4Pack(center.x, center.y, center.z, radius))

    override val center: Vector3 get() = Vector3(data.x, data.y, data.z)
    val radius: Float get() = data.w
    override val volume: Float get() = ((4f / 3f) * PIF) * (radius * radius * radius)
}
