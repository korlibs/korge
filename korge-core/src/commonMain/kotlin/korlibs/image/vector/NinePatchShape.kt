package korlibs.image.vector

import korlibs.datastructure.*
import korlibs.image.color.*
import korlibs.image.util.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*

class NinePatchShape(val shape: Shape, val slices: NinePatchSlices2D) {
    val size: Size = shape.bounds.bottomRight.toSize()

    fun getScaledPointAt(point: Point, newSize: Size): Point =
        slices.getScaledPointAt(point, size, newSize)

    fun transform(newSize: Size): Shape = shape.scaleNinePatch(newSize, slices)

    private fun Shape.scaleNinePatch(newSize: Size, slices: NinePatchSlices2D, oldSize: Size? = this.bounds.bottomRight.toSize()): Shape {
        return when (this) {
            EmptyShape -> EmptyShape
            is CompoundShape -> CompoundShape(this.components.map { it.scaleNinePatch(newSize, slices, oldSize) })
            is FillShape -> FillShape(path.scaleNinePatch(newSize, slices, oldSize), clip?.scaleNinePatch(newSize, slices, oldSize), paint, transform, globalAlpha)
            is PolylineShape -> PolylineShape(path.scaleNinePatch(newSize, slices, oldSize), clip?.scaleNinePatch(newSize, slices, oldSize), paint, transform, strokeInfo, globalAlpha)
            is TextShape -> TextShape(text, pos, font, fontSize, clip?.scaleNinePatch(newSize, slices, oldSize), fill, stroke, align, transform, globalAlpha)
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
    val horizontalSlices = guidesBounds.filter { it.height > it.width }.map { it.x }.sorted().toDoubleArray()
    val verticalSlices = guidesBounds.filter { it.width > it.height }.map { it.y }.sorted().toDoubleArray()
    if (verticalSlices.size % 2 != 0) error("Vertical guides are not a pair number")
    if (horizontalSlices.size % 2 != 0) error("Horizontal guides are not a pair number")
    val slices = NinePatchSlices2D(
        NinePatchSlices(*horizontalSlices),
        NinePatchSlices(*verticalSlices),
    )
    return NinePatchShape(shapeWithoutGuides, slices)
}
