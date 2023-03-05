package com.soywiz.korim.util

import com.soywiz.korma.geom.*
import kotlin.math.absoluteValue
import kotlin.math.sign

data class NinePatchSlices2D(val x: NinePatchSlices, val y: NinePatchSlices) {
    constructor() : this(NinePatchSlices(), NinePatchSlices())
    fun transform2DInplace(
        positions: PointArrayList, oldSize: ISize, newSize: ISize,
    ) {
        val widthRatio = newSize.width / oldSize.width
        val heightRatio = newSize.height / oldSize.height
        val iscale = when {
            newSize.width < oldSize.width || newSize.height < oldSize.height -> minOf(widthRatio.absoluteValue, heightRatio.absoluteValue)
            else -> 1.0
        }
        x.transform1DInplace(oldSize.width, newSize.width, positions.size, get = { positions.getX(it).toDouble() }, set = { index, value -> positions.setX(index, value) }, iscale = iscale * newSize.width.sign)
        y.transform1DInplace(oldSize.height, newSize.height, positions.size, get = { positions.getY(it).toDouble() }, set = { index, value -> positions.setY(index, value) }, iscale = iscale * newSize.height.sign)
    }

    fun transform2D(
        positions: PointArrayList, oldSize: ISize, newSize: ISize, output: PointArrayList = PointArrayList()
    ): PointArrayList {
        output.clear()
        output.copyFrom(positions)
        transform2DInplace(output, oldSize, newSize)
        return output
    }

    fun transform2D(
        positions: List<PointArrayList>, oldSize: ISize, newSize: ISize
    ): List<PointArrayList> = positions.map { transform2D(it, oldSize, newSize) }

    fun getScaledPointAt(point: IPoint, oldSize: ISize, newSize: ISize, out: MPoint = MPoint()): IPoint {
        val p = pointArrayListOf(point)
        transform2DInplace(p, oldSize, newSize)
        out.setTo(p.first)
        return out
    }
}
