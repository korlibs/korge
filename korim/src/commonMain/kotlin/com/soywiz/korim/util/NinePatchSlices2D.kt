package com.soywiz.korim.util

import com.soywiz.korma.geom.ISize
import com.soywiz.korma.geom.PointArrayList

class NinePatchSlices2D(val x: NinePatchSlices, val y: NinePatchSlices) {
    constructor() : this(NinePatchSlices(), NinePatchSlices())
    fun transform2DInplace(
        positions: PointArrayList, oldSize: ISize, newSize: ISize,
    ) {
        x.transform1DInplace(oldSize.width, newSize.width, positions.size, get = { positions.getX(it) }, set = { index, value -> positions.setX(index, value) })
        y.transform1DInplace(oldSize.height, newSize.height, positions.size, get = { positions.getY(it) }, set = { index, value -> positions.setY(index, value) })
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
}
