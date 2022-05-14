package com.soywiz.korma.triangle.triangulate

import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.IPointArrayList
import com.soywiz.korma.geom.PointArrayList
import com.soywiz.korma.geom.shape.Shape2d
import com.soywiz.korma.geom.shape.toPathList
import com.soywiz.korma.geom.toPoints
import com.soywiz.korma.geom.triangle.Triangle
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.triangle.poly2tri.Poly2Tri
import kotlin.jvm.JvmName

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
