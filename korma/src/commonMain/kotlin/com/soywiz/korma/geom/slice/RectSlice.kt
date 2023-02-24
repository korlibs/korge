package com.soywiz.korma.geom.slice

import com.soywiz.korma.geom.*

/**
 * Represents a 2D slice in an integral space inside a [base] (or atlas) that has a [width] and [height].
 *
 * The [rect] property represents the slice inside that base/container, as it is stored without any kind of transformation.
 *
 * Then there is a [orientation] that described how that slice should be interpreted. Should it be rotated or flipped later?
 *
 * The [coords] represents a ratio (0-1) coordinates inside the base/container with the swapped coordinates applying the [orientation].
 * The [unorientedCoords] represents a ratio (0-1) coordinates inside the base/container without applying the [orientation].
 *
 * Then the [padding] describes, the padding that will have the [rect] after applying the orientation to represent the original
 * sliced region.
 * [virtFrame] represents the region including the oriented [width] and [height] and the [padding].
 *
 * Typically, this slice represents a slice inside a Bitmap or a Texture atlas, that might have been stored rotated, flipped, etc.
 * and sometimes it might have been cropped.
 */
data class RectSlice<T : ISizeInt>(
    /** Data containing [width] & [height] */
    override val base: T,
    /** [rect] of the slice, without the [orientation] applied */
    override val rect: IRectangleInt,
    /** An orientation describing how the slice is going to be rotated and flipped */
    val orientation: SliceOrientation = SliceOrientation.ROTATE_0,
    /** Extra empty pixels that will be considered for this slice, for tightly packed images */
    override val padding: com.soywiz.korma.geom.IMarginInt = IMarginInt.ZERO,
    /** Debug [name] describing this slice */
    override val name: String? = null,
) : SliceCoordsWithBaseAndRect<T> { //, Resourceable<RectSlice<T>> {
    val invOrientation: SliceOrientation get() = orientation.inverted()
    val trimmed: Boolean get() = padding.top != 0 || padding.bottom != 0 || padding.left != 0 || padding.right != 0

    val baseWidth: Int = base.width
    val baseHeight: Int = base.height

    /** [width] of the slice without applying the [orientation] */
    val unorientedWidth: Int get() = rect.width
    /** [height] of the slice without applying the [orientation] */
    val unorientedHeight: Int get() = rect.height

    /** [width] of the slice after applying the [orientation] transformation. ie. if this is rotated by 90 or 270, this can be the [height] */
    override val width: Int get() = if (!orientation.isRotatedDeg90CwOrCcw) rect.width else rect.height
    /** [height] of the slice after applying the [orientation] transformation. ie. if this is rotated by 90 or 270, this can be the [width] */
    override val height: Int get() = if (!orientation.isRotatedDeg90CwOrCcw) rect.height else rect.width

    init {
        //check(
        //    (rect.left    in 0 .. baseWidth) &&
        //    (rect.top     in 0 .. baseHeight) &&
        //    (rect.right   in 0 .. baseWidth) &&
        //    (rect.bottom  in 0 .. baseHeight)
        //) {
        //    "Invalid rect=$rect, size=$baseWidth,$baseHeight"
        //}
    }

    private val lx: Float = rect.left.toFloat() / baseWidth
    private val rx: Float = rect.right.toFloat() / baseWidth
    private val ty: Float = rect.top.toFloat() / baseHeight
    private val by: Float = rect.bottom.toFloat() / baseHeight

    /** Coordinates [0-1] based inside the container/base based on the [rect] without applying the [orientation] */
    val unorientedCoords: RectCoords = RectCoords(
        lx, ty,
        rx, ty,
        rx, by,
        lx, by
    )

    /** Coordinates [0-1] based inside the container/base based on the [rect] after applying the [orientation] */
    val coords: RectCoords = unorientedCoords.transformed(orientation)

    override fun transformed(orientation: SliceOrientation): RectSlice<T> = copy(orientation = this.orientation.transformed(orientation))
    override fun flippedX(): RectSlice<T> = transformed(SliceOrientation.ROTATE_0.flippedX())
    override fun flippedY(): RectSlice<T> = transformed(SliceOrientation.ROTATE_0.flippedY())
    override fun rotatedLeft(offset: Int): RectSlice<T> = transformed(SliceOrientation.ROTATE_0.rotatedLeft(offset))
    override fun rotatedRight(offset: Int): RectSlice<T> = transformed(SliceOrientation.ROTATE_0.rotatedRight(offset))

    fun sliceWithBounds(left: Int, top: Int, right: Int, bottom: Int, name: String? = null, clamped: Boolean = true, orientation: SliceOrientation = this.orientation): RectSlice<T> =
        RectSlice(base, rect.sliceWithBounds(left, top, right, bottom, clamped = clamped), orientation, padding, name ?: this.name)
    fun sliceWithSize(x: Int, y: Int, width: Int, height: Int, name: String? = null, clamped: Boolean = true, orientation: SliceOrientation = this.orientation): RectSlice<T> =
        sliceWithBounds(x, y, x + width, y + height, name = name, clamped = clamped, orientation = orientation)
    fun slice(rect: IRectangleInt, name: String? = null, clamped: Boolean = true, orientation: SliceOrientation = this.orientation): RectSlice<T> =
        sliceWithBounds(rect.left, rect.top, rect.right, rect.bottom, name = name, clamped = clamped, orientation = orientation)

    val virtFrame: IRectangleInt?
        get() {
            if (padding.left == 0 && padding.right == 0 && padding.top == 0 && padding.bottom == 0) return null
            return MRectangleInt.fromBounds(padding.left, padding.top, width + padding.leftPlusRight, height + padding.topPlusBottom)
        }

    fun virtFrame(x: Int, y: Int, width: Int, height: Int): RectSlice<T> {
        return copy(padding = IMarginInt(y, width - this.width - x, height - this.height - y, x))
        //val rotated = orientation.isRotatedDeg90CwOrCcw
        //return copy(padding = when {
        //    !rotated -> IMarginInt(y, width - this.width - x, height - this.height - y, x)
        //    else -> IMarginInt(x, height - this.height - y, width - this.width - x, y)
        //})
    }

    fun virtFrame(frame: IRectangleInt?): RectSlice<T> = when (frame) {
        null -> copy(padding = IMarginInt.ZERO)
        else -> virtFrame(frame.x, frame.y, frame.width, frame.height)
    }

    ///////////////

    override val tlX: Float = coords.tlX
    override val tlY: Float = coords.tlY
    override val trX: Float = coords.trX
    override val trY: Float = coords.trY
    override val brX: Float = coords.brX
    override val brY: Float = coords.brY
    override val blX: Float = coords.blX
    override val blY: Float = coords.blY

    //override fun getOrNull(): RectSlice<T>? = this
    //override suspend fun get(): RectSlice<T> = this

    override fun toString(): String {
        return buildString {
            append("RectSlice(")
            append(name)
            append(":")
            append(rect)
            if (orientation != SliceOrientation.ROTATE_0) {
                append(":")
                append(orientation)
            }
            if (padding.isNotZero) {
                append(":")
                append(padding)
            }
            append(")")
        }
    }
}

fun <T : ISizeInt> RectSlice<T>.split(width: Int, height: Int, inRows: Boolean): List<RectSlice<T>> {
    val nheight = this.height / height
    val nwidth = this.width / width
    return arrayListOf<RectSlice<T>>().also {
        if (inRows) {
            for (y in 0 until nheight) for (x in 0 until nwidth) it.add(this.sliceWithSize(x * width, y * height, width, height))
        } else {
            for (x in 0 until nwidth) for (y in 0 until nheight) it.add(this.sliceWithSize(x * width, y * height, width, height))
        }
    }
}

fun <T : ISizeInt> RectSlice<T>.splitInRows(width: Int, height: Int): List<RectSlice<T>> = split(width, height, inRows = true)
fun <T : ISizeInt> RectSlice<T>.splitInCols(width: Int, height: Int): List<RectSlice<T>> = split(width, height, inRows = false)
