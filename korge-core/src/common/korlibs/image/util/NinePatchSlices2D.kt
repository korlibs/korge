package korlibs.image.util

import korlibs.math.geom.*
import kotlin.math.*

data class NinePatchSlices2D(val x: NinePatchSlices, val y: NinePatchSlices) {
    constructor() : this(NinePatchSlices(), NinePatchSlices())
    fun transform2DInplace(
        positions: PointArrayList, oldSize: Size, newSize: Size,
    ) {
        val widthRatio = newSize.width / oldSize.width
        val heightRatio = newSize.height / oldSize.height
        val iscale: Double = when {
            newSize.width < oldSize.width || newSize.height < oldSize.height -> minOf(widthRatio.absoluteValue, heightRatio.absoluteValue)
            else -> 1.0
        }
        x.transform1DInplace(oldSize.width, newSize.width, positions.size, get = { positions[it].x }, set = { index, value -> positions.setX(index, value) }, iscale = iscale * newSize.width.sign)
        y.transform1DInplace(oldSize.height, newSize.height, positions.size, get = { positions[it].y }, set = { index, value -> positions.setY(index, value) }, iscale = iscale * newSize.height.sign)
    }

    fun transform2D(
        positions: PointArrayList, oldSize: Size, newSize: Size, output: PointArrayList = PointArrayList()
    ): PointArrayList {
        output.clear()
        output.copyFrom(positions)
        transform2DInplace(output, oldSize, newSize)
        return output
    }

    fun transform2D(
        positions: List<PointArrayList>, oldSize: Size, newSize: Size
    ): List<PointArrayList> = positions.map { transform2D(it, oldSize, newSize) }

    fun getScaledPointAt(point: Point, oldSize: Size, newSize: Size): Point {
        val p = pointArrayListOf(point)
        transform2DInplace(p, oldSize, newSize)
        return p.first
    }
}
