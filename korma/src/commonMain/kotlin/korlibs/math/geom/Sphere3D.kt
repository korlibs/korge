package korlibs.math.geom

import korlibs.memory.pack.*

inline class Sphere3D private constructor(private val data: Float4Pack) {
    constructor(center: Vector3, radius: Float) : this(Float4Pack(center.x, center.y, center.z, radius))

    val center: Vector3 get() = Vector3(data.x, data.y, data.z)
    val radius: Float get() = data.w
    val volume get() = ((4f / 3f) * PIF) * radius * radius * radius
}
