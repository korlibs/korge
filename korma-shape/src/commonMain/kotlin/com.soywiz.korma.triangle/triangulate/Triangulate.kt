package com.soywiz.korma.triangle.triangulate

import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.triangle.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.triangle.internal.*

fun List<IPoint>.triangulate(): List<Triangle> {
    val sc = SweepContext(this)
    val s = Sweep(sc)
    s.triangulate()
    return sc.triangles.toList()
}

fun Shape2d.triangulate(): List<List<Triangle>> = this.paths.map { it.toPoints().triangulate() }
fun Shape2d.triangulateFlat(): List<Triangle> = triangulate().flatMap { it }

fun VectorPath.triangulate(): List<List<Triangle>> = this.toShape2d().triangulate()
fun VectorPath.triangulateFlat(): List<Triangle> = this.toShape2d().triangulateFlat()
