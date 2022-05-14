package com.soywiz.korma.triangle.pathfind

import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.IPointArrayList
import com.soywiz.korma.geom.shape.Shape2d
import com.soywiz.korma.geom.triangle.Triangle
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.triangle.poly2tri.triangulateSafe
import com.soywiz.korma.triangle.triangulate.triangulateFlat

fun Iterable<Triangle>.toSpatialMesh(): SpatialMesh = SpatialMesh(this)
fun Iterable<Triangle>.pathFind(): SpatialMeshFind = SpatialMeshFind(this.toSpatialMesh())

fun SpatialMeshFind.funnel(p0: IPoint, p1: IPoint): IPointArrayList = find(p0, p1)

fun Iterable<Triangle>.funnel(p0: IPoint, p1: IPoint): IPointArrayList = this.pathFind().funnel(p0, p1)
fun Iterable<Triangle>.pathFind(p0: IPoint, p1: IPoint): IPointArrayList = this.pathFind().funnel(p0, p1)

fun Shape2d.toSpatialMesh(): SpatialMesh = SpatialMesh(this.triangulateFlat())
fun Shape2d.pathFind(): SpatialMeshFind = this.triangulateFlat().pathFind()
fun Shape2d.pathFind(p0: IPoint, p1: IPoint): IPointArrayList = this.triangulateFlat().pathFind().funnel(p0, p1)

fun VectorPath.toSpatialMesh(): SpatialMesh = SpatialMesh(this.triangulateSafe())
fun VectorPath.pathFind(): SpatialMeshFind = this.triangulateSafe().pathFind()
fun VectorPath.pathFind(p0: IPoint, p1: IPoint): IPointArrayList = this.triangulateSafe().pathFind().funnel(p0, p1)
