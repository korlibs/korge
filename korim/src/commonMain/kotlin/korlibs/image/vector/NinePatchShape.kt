package korlibs.image.vector

import korlibs.datastructure.fastArrayListOf
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.util.NinePatchSlices
import korlibs.image.util.NinePatchSlices2D
import korlibs.math.geom.*
import korlibs.math.geom.vector.VectorPath

class NinePatchShape(val shape: Shape, val slices: NinePatchSlices2D) {
    val size: MSize = shape.bounds.bottomRight.toSize().mutable

    fun getScaledPointAt(point: MPoint, newSize: MSize, out: MPoint = MPoint()): MPoint =
        slices.getScaledPointAt(point, size, newSize, out)

    fun transform(newSize: MSize): Shape = shape.scaleNinePatch(newSize, slices)

    private fun Shape.scaleNinePatch(newSize: MSize, slices: NinePatchSlices2D, oldSize: MSize? = this.bounds.bottomRight.toSize().mutable): Shape {
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

fun Shape.toNinePatchFromGuides(guideColor: RGBA = Colors.FUCHSIA, optimizeShape: Boolean = true): NinePatchShape {
    val guides = fastArrayListOf<VectorPath>()
    val shapeWithoutGuides = filterShape {
        if (it is PolylineShape && it.paint == guideColor) {
            guides += it.path
            false
        } else {
            true
        }
    }.let { if (optimizeShape) it.optimize() else it }
    val guidesBounds = guides.map { it.getBounds() }
    val horizontalSlices = guidesBounds.filter { it.height > it.width }.map { it.xD }.sorted().toDoubleArray()
    val verticalSlices = guidesBounds.filter { it.width > it.height }.map { it.yD }.sorted().toDoubleArray()
    if (verticalSlices.size % 2 != 0) error("Vertical guides are not a pair number")
    if (horizontalSlices.size % 2 != 0) error("Horizontal guides are not a pair number")
    val slices = NinePatchSlices2D(
        NinePatchSlices(*horizontalSlices),
        NinePatchSlices(*verticalSlices),
    )
    return NinePatchShape(shapeWithoutGuides, slices)
}
