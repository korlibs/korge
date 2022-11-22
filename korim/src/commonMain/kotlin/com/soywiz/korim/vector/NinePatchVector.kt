package com.soywiz.korim.vector

import com.soywiz.korim.util.NinePatchSlices2D
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.ISize
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.PointArrayList
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.vector.VectorPath

class NinePatchVector(
    val path: VectorPath,
    val slices: NinePatchSlices2D,
    oldSize: ISize? = null
) {
    val size = oldSize ?: path.getBounds().let { Size(it.right, it.bottom) }
    private val tempPoints = PointArrayList()

    fun getScaledPointAt(point: IPoint, newSize: ISize, out: Point = Point()): IPoint =
        slices.getScaledPointAt(point, size, newSize, out)

    private inline fun transformPoints(newSize: ISize, gen: PointArrayList.() -> Unit): PointArrayList {
        tempPoints.clear()
        gen(tempPoints)
        slices.transform2DInplace(tempPoints, size, newSize)
        return tempPoints
    }

    fun transform(newSize: ISize, out: VectorPath = VectorPath()): VectorPath {
        out.clear()
        path.visitCmds(
            moveTo = { x, y ->
                val p = transformPoints(newSize) { add(x, y) }
                out.moveTo(p.getX(0), p.getY(0))
            },
            lineTo = { x, y ->
                val p = transformPoints(newSize) { add(x, y) }
                out.lineTo(p.getX(0), p.getY(0))
            },
            quadTo = { x1, y1, x2, y2 ->
                val p = transformPoints(newSize) { add(x1, y1).add(x2, y2) }
                out.quadTo(p.getX(0), p.getY(0), p.getX(1), p.getY(1))
            },
            cubicTo = { x1, y1, x2, y2, x3, y3 ->
                val p = transformPoints(newSize) { add(x1, y1).add(x2, y2).add(x3, y3) }
                out.cubicTo(p.getX(0), p.getY(0), p.getX(1), p.getY(1), p.getX(2), p.getY(2))
            },
            close = {
                out.close()
            }
        )
        return out
    }
}

fun VectorPath.scaleNinePatch(newSize: ISize, slices: NinePatchSlices2D = NinePatchSlices2D(), oldSize: ISize? = null, out: VectorPath = VectorPath()): VectorPath {
    out.clear()
    return NinePatchVector(this, slices, oldSize).transform(newSize, out)
}
