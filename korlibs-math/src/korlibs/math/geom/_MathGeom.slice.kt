@file:Suppress("PackageDirectoryMismatch")

package korlibs.math.geom.slice

import korlibs.datastructure.*
import korlibs.math.*
import korlibs.math.geom.*

data class RectCoords(
    override val tlX: Float, override val tlY: Float,
    override val trX: Float, override val trY: Float,
    override val brX: Float, override val brY: Float,
    override val blX: Float, override val blY: Float,
) : SliceCoords {
    fun flippedX(): RectCoords = transformed(SliceOrientation.ORIGINAL.flippedX())
    fun flippedY(): RectCoords = transformed(SliceOrientation.ORIGINAL.flippedY())
    fun rotatedLeft(offset: Int = +1): RectCoords = transformed(SliceOrientation(rotation = SliceRotation.R0.rotatedLeft(offset)))
    fun rotatedRight(offset: Int = +1): RectCoords = transformed(SliceOrientation(rotation = SliceRotation.R0.rotatedRight(offset)))
}


//inline class RectCoords(val data: FloatArray) {
//    constructor(
//        tlX: Float, tlY: Float,
//        trX: Float, trY: Float,
//        brX: Float, brY: Float,
//        blX: Float, blY: Float,
//    ) : this(floatArrayOf(tlX, tlY, trX, trY, brX, brY, blX, blY))
//
//    init { check(data.size == 8) }
//    fun x(index: Int): Float = data[index * 2]
//    fun y(index: Int): Float = data[index * 2 + 1]
//    val tlX: Float get() = x(0)
//    val tlY: Float get() = y(0)
//    val trX: Float get() = x(1)
//    val trY: Float get() = y(1)
//    val brX: Float get() = x(2)
//    val brY: Float get() = y(2)
//    val blX: Float get() = x(3)
//    val blY: Float get() = y(3)
//
//    fun transform(orientation: ImageOrientation): RBmpCoords {
//        val I = orientation.indices
//        return RBmpCoords(
//            x(I[0]), y(I[0]),
//            x(I[1]), y(I[1]),
//            x(I[2]), y(I[2]),
//            x(I[3]), y(I[3]),
//        )
//    }
//}

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
data class RectSlice<T : SizeableInt>(
    /** Data containing [width] & [height] */
    override val base: T,
    /** [rect] of the slice, without the [orientation] applied */
    override val rect: RectangleInt,
    /** An orientation describing how the slice is going to be rotated and flipped */
    val orientation: SliceOrientation = SliceOrientation.ROTATE_0,
    /** Extra empty pixels that will be considered for this slice, for tightly packed images */
    override val padding: MarginInt = MarginInt.ZERO,
    /** Debug [name] describing this slice */
    override val name: String? = null,
) : SliceCoordsWithBaseAndRect<T> { //, Resourceable<RectSlice<T>> {
    val invOrientation: SliceOrientation get() = orientation.inverted()
    val trimmed: Boolean get() = padding.top != 0 || padding.bottom != 0 || padding.left != 0 || padding.right != 0

    val baseWidth: Int = base.size.width
    val baseHeight: Int = base.size.height

    /** [width] of the slice without applying the [orientation] */
    val unorientedWidth: Int = rect.width
    /** [height] of the slice without applying the [orientation] */
    val unorientedHeight: Int = rect.height

    /** [width] of the slice after applying the [orientation] transformation. ie. if this is rotated by 90 or 270, this can be the [height] */
    override val width: Int = if (!orientation.isRotatedDeg90CwOrCcw) rect.width else rect.height
    /** [height] of the slice after applying the [orientation] transformation. ie. if this is rotated by 90 or 270, this can be the [width] */
    override val height: Int = if (!orientation.isRotatedDeg90CwOrCcw) rect.height else rect.width

    override val frameWidth: Int = width + padding.leftPlusRight
    override val frameHeight: Int = height + padding.topPlusBottom

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
    fun slice(rect: RectangleInt, name: String? = null, clamped: Boolean = true, orientation: SliceOrientation = this.orientation): RectSlice<T> =
        sliceWithBounds(rect.left, rect.top, rect.right, rect.bottom, name = name, clamped = clamped, orientation = orientation)

    val virtFrame: RectangleInt?
        get() {
            if (padding.left == 0 && padding.right == 0 && padding.top == 0 && padding.bottom == 0) return null
            return RectangleInt.fromBounds(padding.left, padding.top, width + padding.leftPlusRight, height + padding.topPlusBottom)
        }

    fun virtFrame(x: Int, y: Int, width: Int, height: Int): RectSlice<T> {
        return copy(padding = MarginInt(y, width - this.width - x, height - this.height - y, x))
        //val rotated = orientation.isRotatedDeg90CwOrCcw
        //return copy(padding = when {
        //    !rotated -> IMarginInt(y, width - this.width - x, height - this.height - y, x)
        //    else -> IMarginInt(x, height - this.height - y, width - this.width - x, y)
        //})
    }

    fun virtFrame(frame: RectangleInt?): RectSlice<T> = when (frame) {
        null -> copy(padding = MarginInt.ZERO)
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

fun <T : SizeableInt> RectSlice<T>.split(width: Int, height: Int, inRows: Boolean): List<RectSlice<T>> {
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

fun <T : SizeableInt> RectSlice<T>.splitInRows(width: Int, height: Int): List<RectSlice<T>> = split(width, height, inRows = true)
fun <T : SizeableInt> RectSlice<T>.splitInCols(width: Int, height: Int): List<RectSlice<T>> = split(width, height, inRows = false)


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

interface SliceCoordsWithBase<T : SizeableInt> : SliceCoords {
    val name: String? get() = null
    val base: T
    val width: Int
    val height: Int
    val padding: MarginInt

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
        operator fun <T : SizeableInt> invoke(
            base: T,
            coords: SliceCoords,
            name: String? = null,
            flippedWidthHeight: Boolean = false,
        ): SliceCoordsImpl<T> = SliceCoordsImpl(
            base, coords, name, flippedWidthHeight
        )
    }
}

interface SliceCoordsWithBaseAndRect<T : SizeableInt> : SliceCoordsWithBase<T> {
    val rect: RectangleInt

    val left: Int get() = rect.left
    val top: Int get() = rect.top
    val right: Int get() = rect.right
    val bottom: Int get() = rect.bottom
    val area: Int get() = rect.area
}

data class SliceCoordsImpl<T : SizeableInt>(
    /** Data containing [width] & [height] */
    override val base: T,
    /** Coordinates [0-1] based inside the container/base */
    val coords: SliceCoords,
    /** Debug [name] */
    override val name: String? = null,
    val flippedWidthHeight: Boolean = false,
) : SliceCoordsWithBase<T> {
    override val padding = MarginInt.ZERO

    val transformedWidth: Int = if (!flippedWidthHeight) base.size.width else base.size.height
    val transformedHeight: Int = if (!flippedWidthHeight) base.size.height else base.size.width
    override val width: Int = (Point.distance(coords.tlX, coords.tlY, coords.trX, coords.trY) * transformedWidth).toInt()
    override val height: Int = (Point.distance(coords.tlX, coords.tlY, coords.blX, coords.blY) * transformedHeight).toInt()
    override val frameWidth: Int = width + padding.leftPlusRight
    override val frameHeight: Int = height + padding.topPlusBottom

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
    m.transformX(tlX, tlY), m.transformY(tlX, tlY),
    m.transformX(trX, trY), m.transformY(trX, trY),
    m.transformX(brX, brY), m.transformY(brX, brY),
    m.transformX(blX, blY), m.transformY(blX, blY),
)

fun SliceCoords.transformed(m: Matrix4): RectCoords {
    // @TODO: This allocates
    val v1 = m.transform(Vector4F(tlX, tlY, 0f, 1f))
    val v2 = m.transform(Vector4F(trX, trY, 0f, 1f))
    val v3 = m.transform(Vector4F(brX, brY, 0f, 1f))
    val v4 = m.transform(Vector4F(blX, blY, 0f, 1f))
    return RectCoords(v1.x, v1.y, v2.x, v2.y, v3.x, v3.y, v4.x, v4.y)
}

// Special versions

fun <T : SizeableInt> SliceCoordsWithBase<T>.transformed(m: Matrix): SliceCoordsWithBase<T> {
    val coords = (this as SliceCoords).transformed(m)
    return SliceCoordsImpl(base, coords, name)
}

fun <T : SizeableInt> SliceCoordsWithBase<T>.transformed(m: Matrix4): SliceCoordsWithBase<T> {
    val coords = (this as SliceCoords).transformed(m)
    return SliceCoordsImpl(base, coords, name)
}

fun <T : SizeableInt> SliceCoordsWithBaseAndRect<T>.transformed(orientation: SliceOrientation, name: String? = this.name): RectSlice<T> {
    return RectSlice(base, rect, orientation, padding, name)
}

enum class SliceRotation {
    R0, R90, R180, R270;

    val angle: Int = ordinal * 90

    fun rotatedLeft(offset: Int = 1): SliceRotation = SliceRotation[(ordinal - offset) umod 4]
    fun rotatedRight(offset: Int = 1): SliceRotation = SliceRotation[(ordinal + offset) umod 4]
    fun complementary(): SliceRotation = SliceRotation[-ordinal umod 4]
    internal fun _comp2(): SliceRotation = SliceRotation[(-ordinal + 2) umod 4]

    companion object {
        val VALUES = values()
        operator fun get(index: Int): SliceRotation = VALUES[index umod 4]
    }
}

/**
 * Represents an orientation where:
 * [flipX] and [flipY] is applied first, and then [rotation].
 */
inline class SliceOrientation(
    val raw: Int,
) {
    val rotation: SliceRotation get() = SliceRotation[raw.extract(0, 2)]
    val flipX: Boolean get() = raw.extract(2, 1) != 0

    constructor(rotation: SliceRotation = SliceRotation.R0, flipX: Boolean = false) : this(0.insert(rotation.ordinal, 0, 2).insert(flipX, 2))

    /** Indices represent TL, TR, BR, BL */
    val indices: IntArray get() = INDICES[raw.extract(0, 3)]

    val isRotatedDeg90CwOrCcw: Boolean get() = rotation == SliceRotation.R90 || rotation == SliceRotation.R270

    // @TODO: Check this
    fun inverted(): SliceOrientation {
        if (flipX && rotation.ordinal % 2 == 1) return SliceOrientation(rotation._comp2(), flipX)
        return SliceOrientation(rotation.complementary(), flipX)
    }

    fun flippedX(): SliceOrientation = SliceOrientation(flipX = !flipX, rotation = rotation.complementary())
    fun flippedY(): SliceOrientation = SliceOrientation(flipX = !flipX, rotation = if (isRotatedDeg90CwOrCcw) rotation else rotation.rotatedRight(2))
    fun rotatedLeft(offset: Int = 1): SliceOrientation = SliceOrientation(rotation.rotatedLeft(offset), flipX)
    fun rotatedRight(offset: Int = 1): SliceOrientation = SliceOrientation(rotation.rotatedRight(offset), flipX)

    fun transformed(orientation: SliceOrientation): SliceOrientation {
        var out = this
        if (orientation.flipX) out = out.flippedX()
        out = out.rotatedRight(orientation.rotation.ordinal)
        return out
    }
    override fun toString(): String = NAMES[raw.extract(0, 3)]

    fun getX(width: Int, height: Int, x: Int, y: Int): Int {
        val w1 = width - 1
        val h1 = height - 1
        val x = if (flipX) w1 - x else x
        return when (rotation) {
            SliceRotation.R0 -> x
            SliceRotation.R90 -> h1 - y
            SliceRotation.R180 -> w1 - x
            SliceRotation.R270 -> y
        }
    }
    fun getY(width: Int, height: Int, x: Int, y: Int): Int {
        val w1 = width - 1
        val h1 = height - 1
        val x = if (flipX) w1 - x else x
        return when (rotation) {
            SliceRotation.R0 -> y // done
            SliceRotation.R90 -> x
            SliceRotation.R180 -> (h1 - y)
            SliceRotation.R270 -> (w1 - x) // done
        }
    }
    fun getXY(width: Int, height: Int, x: Int, y: Int): PointInt =
        PointInt(getX(width, height, x, y), getY(width, height, x, y))

    object Indices {
        const val TL = 0
        const val TR = 1
        const val BR = 2
        const val BL = 3
    }

    companion object {
        private val INDICES = Array(8) { index ->
            intArrayOf(0, 1, 2, 3).also { out ->
                val orientation = SliceOrientation(index)
                if (orientation.flipX) {
                    out.swap(Indices.TL, Indices.TR)
                    out.swap(Indices.BL, Indices.BR)
                }
                out.rotateRight(orientation.rotation.ordinal)
            }
        }
        private val NAMES = Array(8) { index ->
            val orientation = SliceOrientation(index)

            buildString {
                append(if (orientation.flipX) "MIRROR_HORIZONTAL_ROTATE_" else "ROTATE_")
                append(orientation.rotation.angle)
            }
        }

        val ROTATE_0: SliceOrientation = SliceOrientation(flipX = false, rotation = SliceRotation.R0)
        val ROTATE_90: SliceOrientation = SliceOrientation(flipX = false, rotation = SliceRotation.R90)
        val ROTATE_180: SliceOrientation = SliceOrientation(flipX = false, rotation = SliceRotation.R180)
        val ROTATE_270: SliceOrientation = SliceOrientation(flipX = false, rotation = SliceRotation.R270)
        val MIRROR_HORIZONTAL_ROTATE_0: SliceOrientation = SliceOrientation(flipX = true)
        val MIRROR_HORIZONTAL_ROTATE_90: SliceOrientation = SliceOrientation(flipX = true, rotation = SliceRotation.R90)
        val MIRROR_HORIZONTAL_ROTATE_180: SliceOrientation = SliceOrientation(flipX = true, rotation = SliceRotation.R180)
        val MIRROR_HORIZONTAL_ROTATE_270: SliceOrientation = SliceOrientation(flipX = true, rotation = SliceRotation.R270)

        // Aliases
        inline val NORMAL: SliceOrientation get() = ROTATE_0
        inline val ORIGINAL: SliceOrientation get() = ROTATE_0
        inline val MIRROR_HORIZONTAL: SliceOrientation get() = MIRROR_HORIZONTAL_ROTATE_0
        inline val MIRROR_VERTICAL: SliceOrientation get() = MIRROR_HORIZONTAL_ROTATE_180

        val VALUES: List<SliceOrientation> = listOf(
            ROTATE_0, ROTATE_90, ROTATE_180, ROTATE_270,
            MIRROR_HORIZONTAL_ROTATE_0, MIRROR_HORIZONTAL_ROTATE_90, MIRROR_HORIZONTAL_ROTATE_180, MIRROR_HORIZONTAL_ROTATE_270,
        )
    }
}

private fun Int.mask(): Int = (1 shl this) - 1
private fun Int.extract(offset: Int, bits: Int): Int = (this ushr offset) and bits.mask()
private fun Int.insert(value: Boolean, offset: Int): Int {
    val bits = (1 shl offset)
    return if (value) this or bits else this and bits.inv()
}
private fun Int.insert(value: Int, offset: Int, bits: Int): Int {
    val mask = bits.mask() shl offset
    val ovalue = (value shl offset) and mask
    return (this and mask.inv()) or ovalue
}
