package com.soywiz.korma.triangle.pathfind

import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.triangle.*
import com.soywiz.korma.triangle.triangulate.*

fun List<Triangle>.toSpatialMesh(): SpatialMesh = SpatialMesh(this)
fun List<Triangle>.pathFind(): SpatialMeshFind = SpatialMeshFind(this.toSpatialMesh())

fun SpatialMeshFind.funnel(p0: IPoint, p1: IPoint): List<IPoint> {
    val pf = this
    val sm = pf.spatialMesh
    val pointStart = IPoint(p0.x, p0.y)
    val pointEnd = IPoint(p1.x, p1.y)
    val pathNodes = pf.find(sm.spatialNodeFromPoint(pointStart), sm.spatialNodeFromPoint(pointEnd))
    val portals = SpatialMeshFind.Channel.channelToPortals(pointStart, pointEnd, pathNodes)
    return portals.path.map { IPoint(it.x, it.y) }
}

fun List<Triangle>.funnel(p0: IPoint, p1: IPoint): List<IPoint> = this.pathFind().funnel(p0, p1)
fun List<Triangle>.pathFind(p0: IPoint, p1: IPoint): List<IPoint> = this.pathFind().funnel(p0, p1)

fun Shape2d.toSpatialMesh(): SpatialMesh = SpatialMesh(this.triangulateFlat())
fun Shape2d.pathFind(): SpatialMeshFind = this.triangulateFlat().pathFind()
fun Shape2d.pathFind(p0: IPoint, p1: IPoint): List<IPoint> = this.triangulateFlat().pathFind().funnel(p0, p1)
