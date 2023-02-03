package com.soywiz.korma.geom.slice

import com.soywiz.korma.geom.*

interface SliceCoords {
    //val premultiplied: Boolean get() = true

    val tlX: Float
    val tlY: Float

    val trX: Float
    val trY: Float

    val brX: Float
    val brY: Float

    val blX: Float
    val blY: Float

    fun x(index: Int): Float = when (index) {
        0 -> tlX
        1 -> trX
        2 -> brX
        3 -> blX
        else -> Float.NaN
    }
    fun y(index: Int): Float = when (index) {
        0 -> tlY
        1 -> trY
        2 -> brY
        3 -> blY
        else -> Float.NaN
    }
}

interface SliceCoordsWithBase<T : ISizeInt> : SliceCoords {
    val name: String? get() = null
    val base: T
    val width: Int
    val height: Int
    val padding: IMarginInt

    val sizeString: String get() = "${width}x${height}"
    val frameOffsetX: Int get() = padding.left
    val frameOffsetY: Int get() = padding.top
    val frameWidth: Int get() = width + padding.leftPlusRight
    val frameHeight: Int get() = height + padding.topPlusBottom

    fun transformed(orientation: SliceOrientation): SliceCoordsWithBase<T> = SliceCoordsWithBase(base, (this as SliceCoords).transformed(orientation), name)
    fun flippedX(): SliceCoordsWithBase<T> = transformed(SliceOrientation.ROTATE_0.flippedX())
    fun flippedY(): SliceCoordsWithBase<T> = transformed(SliceOrientation.ROTATE_0.flippedY())
    fun rotatedLeft(offset: Int = 1): SliceCoordsWithBase<T> = transformed(SliceOrientation.ROTATE_0.rotatedLeft(offset))
    fun rotatedRight(offset: Int = 1): SliceCoordsWithBase<T> = transformed(SliceOrientation.ROTATE_0.rotatedRight(offset))

    companion object {
        operator fun <T : ISizeInt> invoke(
            base: T,
            coords: SliceCoords,
            name: String? = null,
            flippedWidthHeight: Boolean = false,
        ): SliceCoordsImpl<T> = SliceCoordsImpl(
            base, coords, name, flippedWidthHeight
        )
    }
}

interface SliceCoordsWithBaseAndRect<T : ISizeInt> : SliceCoordsWithBase<T> {
    val rect: IRectangleInt

    val left: Int get() = rect.left
    val top: Int get() = rect.top
    val right: Int get() = rect.right
    val bottom: Int get() = rect.bottom
    val area: Int get() = rect.area
}

data class SliceCoordsImpl<T : ISizeInt>(
    /** Data containing [width] & [height] */
    override val base: T,
    /** Coordinates [0-1] based inside the container/base */
    val coords: SliceCoords,
    /** Debug [name] */
    override val name: String? = null,
    val flippedWidthHeight: Boolean = false,
) : SliceCoordsWithBase<T> {
    override val padding get() = IMarginInt.ZERO

    val transformedWidth: Int = if (!flippedWidthHeight) base.width else base.height
    val transformedHeight: Int = if (!flippedWidthHeight) base.height else base.width
    override val width: Int = (Point.distance(coords.tlX, coords.tlY, coords.trX, coords.trY) * transformedWidth).toInt()
    override val height: Int = (Point.distance(coords.tlX, coords.tlY, coords.blX, coords.blY) * transformedHeight).toInt()

    override fun transformed(orientation: SliceOrientation): SliceCoordsWithBase<T> = SliceCoordsWithBase(base, (this as SliceCoords).transformed(orientation), name, flippedWidthHeight)

    override val tlX: Float get() = coords.tlX
    override val tlY: Float get() = coords.tlY
    override val trX: Float get() = coords.trX
    override val trY: Float get() = coords.trY
    override val brX: Float get() = coords.brX
    override val brY: Float get() = coords.brY
    override val blX: Float get() = coords.blX
    override val blY: Float get() = coords.blY
}

fun SliceCoords.transformed(orientation: SliceOrientation): RectCoords {
    val i = orientation.indices
    return RectCoords(
        x(i[0]), y(i[0]),
        x(i[1]), y(i[1]),
        x(i[2]), y(i[2]),
        x(i[3]), y(i[3]),
    )
}

fun SliceCoords.transformed(m: Matrix): RectCoords = RectCoords(
    m.transformXf(tlX, tlY), m.transformYf(tlX, tlY),
    m.transformXf(trX, trY), m.transformYf(trX, trY),
    m.transformXf(brX, brY), m.transformYf(brX, brY),
    m.transformXf(blX, blY), m.transformYf(blX, blY),
)

fun SliceCoords.transformed(m: Matrix3D): RectCoords {
    // @TODO: This allocates
    val v1 = m.transform(tlX, tlY, 0f, 1f)
    val v2 = m.transform(trX, trY, 0f, 1f)
    val v3 = m.transform(brX, brY, 0f, 1f)
    val v4 = m.transform(blX, blY, 0f, 1f)
    return RectCoords(v1.x, v1.y, v2.x, v2.y, v3.x, v3.y, v4.x, v4.y)
}

// Special versions

fun <T : ISizeInt> SliceCoordsWithBase<T>.transformed(m: Matrix): SliceCoordsWithBase<T> {
    val coords = (this as SliceCoords).transformed(m)
    return SliceCoordsImpl(base, coords, name)
}

fun <T : ISizeInt> SliceCoordsWithBase<T>.transformed(m: Matrix3D): SliceCoordsWithBase<T> {
    val coords = (this as SliceCoords).transformed(m)
    return SliceCoordsImpl(base, coords, name)
}

fun <T : ISizeInt> SliceCoordsWithBaseAndRect<T>.transformed(orientation: SliceOrientation, name: String? = this.name): RectSlice<T> {
    return RectSlice(base, rect, orientation, padding, name)
}
