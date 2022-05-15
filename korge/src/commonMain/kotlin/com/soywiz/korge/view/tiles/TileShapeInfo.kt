package com.soywiz.korge.view.tiles

import com.soywiz.korge.view.HitTestDirection
import com.soywiz.korge.view.HitTestDirectionFlags
import com.soywiz.korge.view.HitTestable
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.shape.Shape2d

interface TileShapeInfo : HitTestable {
    fun hitTestAny(shape2d: Shape2d, matrix: Matrix, direction: HitTestDirection): Boolean
}

data class TileShapeInfoImpl(
    val type: HitTestDirectionFlags,
    val shape: Shape2d,
    val transform: Matrix,
    //val path: VectorPath
) : TileShapeInfo {
    val transformInv: Matrix = transform.inverted()

    override fun hitTestAny(x: Double, y: Double, direction: HitTestDirection): Boolean {
        //return path.containsPoint(x, y) && type.matches(direction)
        //println("CHECK SHAPE: $shape")
        return shape.containsPoint(x, y, transformInv) && type.matches(direction)
    }

    override fun hitTestAny(shape2d: Shape2d, matrix: Matrix, direction: HitTestDirection): Boolean =
        Shape2d.intersects(shape, transform, shape2d, matrix) && type.matches(direction)
}
