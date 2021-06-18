package com.soywiz.korma.triangle.triangulate

import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.triangle.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.triangle.poly2tri.*
import kotlin.jvm.*

fun List<IPoint>.triangulate(): List<Triangle> = listOf(PointArrayList(this)).triangulate()

@JvmName("triangulateListPointArrayList")
fun List<IPointArrayList>.triangulate(): List<Triangle> {
    val sc = Poly2Tri.SweepContext()
    sc.addHoles(this)
    sc.triangulate()
    return sc.getTriangles().toList()
}

fun Shape2d.triangulate(): List<List<Triangle>> = this.paths.map { it.toPoints().triangulate() }
fun Shape2d.triangulateFlat(): List<Triangle> = triangulate().flatMap { it }

//fun VectorPath.triangulate(): List<List<Triangle>> = this.toPathList().triangulate()
fun VectorPath.triangulate(): List<Triangle> = this.toPathList().triangulate()
