package com.soywiz.korim.vector

import com.soywiz.korim.util.NinePatchSlices2D
import com.soywiz.korma.geom.MPoint
import com.soywiz.korma.geom.PointArrayList
import com.soywiz.korma.geom.MSize
import com.soywiz.korma.geom.vector.*

class NinePatchVector(
    val path: VectorPath,
    val slices: NinePatchSlices2D,
    oldSize: MSize? = null
) {
    val size = oldSize ?: path.getBounds().let { MSize(it.right, it.bottom) }
    private val tempPoints = PointArrayList()

    fun getScaledPointAt(point: MPoint, newSize: MSize, out: MPoint = MPoint()): MPoint =
        slices.getScaledPointAt(point, size, newSize, out)

    private inline fun transformPoints(newSize: MSize, gen: PointArrayList.() -> Unit): PointArrayList {
        tempPoints.clear()
        gen(tempPoints)
        slices.transform2DInplace(tempPoints, size, newSize)
        return tempPoints
    }

    fun transform(newSize: MSize, out: VectorPath = VectorPath()): VectorPath {
        out.clear()
        path.visitCmds(
            moveTo = { (x, y) ->
                val p = transformPoints(newSize) { add(x, y) }
                out.moveTo(p[0])
            },
            lineTo = { (x, y) ->
                val p = transformPoints(newSize) { add(x, y) }
                out.lineTo(p[0])
            },
            quadTo = { (x1, y1), (x2, y2) ->
                val p = transformPoints(newSize) { add(x1, y1).add(x2, y2) }
                out.quadTo(p[0], p[1])
            },
            cubicTo = { (x1, y1), (x2, y2), (x3, y3) ->
                val p = transformPoints(newSize) { add(x1, y1).add(x2, y2).add(x3, y3) }
                out.cubicTo(p[0], p[1], p[2])
            },
            close = {
                out.close()
            }
        )
        return out
    }
}

fun VectorPath.scaleNinePatch(newSize: MSize, slices: NinePatchSlices2D = NinePatchSlices2D(), oldSize: MSize? = null, out: VectorPath = VectorPath()): VectorPath {
    out.clear()
    return NinePatchVector(this, slices, oldSize).transform(newSize, out)
}
