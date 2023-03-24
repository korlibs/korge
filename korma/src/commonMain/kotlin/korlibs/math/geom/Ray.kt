package korlibs.math.geom

import korlibs.memory.pack.*

inline class Ray(val data: Float4Pack) {
    val point: Point get() = Point(data.f0, data.f1)
    val direction: Point get() = Point(data.f2, data.f3)

    constructor(point: Point, direction: Point) : this(float4PackOf(point.x, point.y, direction.x, direction.y))
}
