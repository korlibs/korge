package com.soywiz.korim.tiles

import com.soywiz.korma.geom.MMatrix
import com.soywiz.korma.geom.collider.HitTestDirection
import com.soywiz.korma.geom.collider.HitTestDirectionFlags
import com.soywiz.korma.geom.collider.HitTestable
import com.soywiz.korma.geom.shape.Shape2d

interface TileShapeInfo : HitTestable {
    fun hitTestAny(shape2d: Shape2d, matrix: MMatrix, direction: HitTestDirection): Boolean
}

data class TileShapeInfoImpl(
    val type: HitTestDirectionFlags,
    val shape: Shape2d,
    val transform: MMatrix,
    //val path: VectorPath
) : TileShapeInfo {
    val transformInv: MMatrix = transform.inverted()

    override fun hitTestAny(x: Double, y: Double, direction: HitTestDirection): Boolean {
        //return path.containsPoint(x, y) && type.matches(direction)
        //println("CHECK SHAPE: $shape")
        return shape.containsPoint(x, y, transformInv) && type.matches(direction)
    }

    override fun hitTestAny(shape2d: Shape2d, matrix: MMatrix, direction: HitTestDirection): Boolean =
        Shape2d.intersects(shape, transform, shape2d, matrix) && type.matches(direction)
}
