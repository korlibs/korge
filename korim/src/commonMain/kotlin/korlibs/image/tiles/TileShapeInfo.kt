package korlibs.image.tiles

import korlibs.math.geom.*
import korlibs.math.geom.collider.HitTestDirection
import korlibs.math.geom.collider.HitTestDirectionFlags
import korlibs.math.geom.collider.HitTestable
import korlibs.math.geom.shape.*

interface TileShapeInfo : HitTestable {
    fun hitTestAny(shape2d: Shape2D, matrix: Matrix, direction: HitTestDirection): Boolean
}

data class TileShapeInfoImpl(
    val type: HitTestDirectionFlags,
    val shape: Shape2D,
    val transform: Matrix,
    //val path: VectorPath
) : TileShapeInfo {
    val transformInv: Matrix = transform.inverted()

    override fun hitTestAny(p: Point, direction: HitTestDirection): Boolean {
        //return path.containsPoint(x, y) && type.matches(direction)
        //println("CHECK SHAPE: $shape")
        return shape.containsPoint(transformInv.transform(p)) && type.matches(direction)
    }

    override fun hitTestAny(shape2d: Shape2D, matrix: Matrix, direction: HitTestDirection): Boolean =
        Shape2D.intersects(shape, transform, shape2d, matrix) && type.matches(direction)
}
