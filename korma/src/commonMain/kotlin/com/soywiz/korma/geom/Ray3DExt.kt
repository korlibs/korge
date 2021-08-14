package com.soywiz.korma.geom

import com.soywiz.korma.geom.ds.*
import kotlin.math.*

fun Ray3D.intersectRayAABox1(box: AABB3D) : Boolean {
    val ray = this
    // r.dir is unit direction vector of ray
    val dirfrac = Vector3()
    dirfrac.x = 1.0f / ray.dir.x
    dirfrac.y = 1.0f / ray.dir.y
    dirfrac.z = 1.0f / ray.dir.z
    // lb is the corner of AABB with minimal coordinates - left bottom, rt is maximal corner
    // r.org is origin of ray
    val t1 = (box.min.x - ray.pos.x) * dirfrac.x
    val t2 = (box.max.x - ray.pos.x) * dirfrac.x
    val t3 = (box.min.y - ray.pos.y) * dirfrac.y
    val t4 = (box.max.y - ray.pos.y) * dirfrac.y
    val t5 = (box.min.z - ray.pos.z) * dirfrac.z
    val t6 = (box.max.z - ray.pos.z) * dirfrac.z

    val tmin = max(max(min(t1, t2), min(t3, t4)), min(t5, t6))
    val tmax = min(min(max(t1, t2), max(t3, t4)), max(t5, t6))

    // if tmax < 0, ray (line) is intersecting AABB, but whole AABB is behing us
    if (tmax < 0) {
        val t = tmax
        return false
    }

    // if tmin > tmax, ray doesn't intersect AABB
    if (tmin > tmax) {
        val t = tmax
        return false
    }

    val t = tmin
    return true

}
