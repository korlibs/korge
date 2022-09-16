package com.soywiz.korim.vector

import com.soywiz.korim.util.NinePatchSlices
import com.soywiz.korim.util.NinePatchSlices2D
import com.soywiz.korma.geom.ISize
import com.soywiz.korma.geom.asSize
import com.soywiz.korma.geom.bottomRight

class NinePatchShape(val shape: Shape, val slices: NinePatchSlices2D) {
    val size: ISize = shape.bounds.bottomRight.asSize()

    fun transform(newSize: ISize): Shape {
        return shape.scaleNinePatch(newSize, slices)
    }

    private fun Shape.scaleNinePatch(newSize: ISize, slices: NinePatchSlices2D, oldSize: ISize? = this.bounds.bottomRight.asSize()): Shape {
        return when (this) {
            EmptyShape -> EmptyShape
            is CompoundShape -> CompoundShape(this.components.map { it.scaleNinePatch(newSize, slices, oldSize) })
            is FillShape -> FillShape(path.scaleNinePatch(newSize, slices, oldSize), clip?.scaleNinePatch(newSize, slices, oldSize), paint, transform, globalAlpha)
            is PolylineShape -> PolylineShape(path.scaleNinePatch(newSize, slices, oldSize), clip?.scaleNinePatch(newSize, slices, oldSize), paint, transform, strokeInfo, globalAlpha)
            is TextShape -> TextShape(text, x, y, font, fontSize, clip?.scaleNinePatch(newSize, slices, oldSize), fill, stroke, halign, valign, transform, globalAlpha)
            else -> TODO()
        }
    }
}

fun Shape.ninePatch(slices: NinePatchSlices2D): NinePatchShape = NinePatchShape(this, slices)
fun Shape.ninePatch(x: NinePatchSlices, y: NinePatchSlices): NinePatchShape = NinePatchShape(this, NinePatchSlices2D(x, y))
