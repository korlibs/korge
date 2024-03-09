package korlibs.image.bitmap

import korlibs.memory.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.geom.slice.*

typealias BmpCoords = SliceCoords
typealias BmpSlice32 = RectSlice<out Bitmap32>
typealias BmpSlice = RectSlice<out Bitmap>
typealias BitmapSlice<T> = RectSlice<T>
typealias BaseBmpSlice = RectSlice<out Bitmap>
typealias BitmapCoords = SliceCoordsWithBase<out Bitmap>
typealias ImageRotation = SliceRotation
typealias ImageOrientation = SliceOrientation

val <T : SizeableInt> RectSlice<T>.bounds: RectangleInt get() = rect

val <T: Bitmap> RectSlice<T>.bmp: T get() = container

val <T : SizeableInt> SliceCoordsWithBase<T>.container: T get() = base

// @TODO: This should do an inversion
fun BmpSlice.getRgbaOriented(x: Int, y: Int): RGBA = getRgba(invOrientation.getX(width, height, x, y), invOrientation.getY(width, height, x, y))
fun BmpSlice.setRgbaOriented(x: Int, y: Int, value: RGBA) = setRgba(invOrientation.getX(width, height, x, y), invOrientation.getY(width, height, x, y), value)

/** Gets a pixel in [RGBA] from the slice in [x] and [y] WITHOUT having into account [orientation] and [padding] */
fun BmpSlice.getRgba(x: Int, y: Int): RGBA {
    check(x in 0 until this.rect.width && y in 0 until this.rect.height)
    return this.container.getRgba(this.rect.x + x, this.rect.y + y)
}

/** Sets a pixel in [RGBA] from the slice in [x] and [y] with the color [value] WITHOUT having into account [orientation] and [padding] */
fun BmpSlice.setRgba(x: Int, y: Int, value: RGBA) {
    check(x in 0 until this.rect.width && y in 0 until this.rect.height)
    this.container.setRgba(this.rect.x + x, this.rect.y + y, value)
}

/** Same as [getRgba] but ignoring premultiplication and bound checks */
fun BmpSlice.getRgbaRawUnsafe(x: Int, y: Int): RGBA = this.container.getRgbaRaw(this.rect.x + x, this.rect.y + y)
/** Same as [setRgba] but ignoring premultiplication and bound checks */
fun BmpSlice.setRgbaRawUnsafe(x: Int, y: Int, value: RGBA) = this.container.setRgbaRaw(this.rect.x + x, this.rect.y + y, value)

/** Reads pixels in region without having into account [RectSlice.orientation] or [RectSlice.padding] */
fun BmpSlice.readPixels(x: Int, y: Int, width: Int, height: Int, out: RgbaArray = RgbaArray(width * height), offset: Int = 0): RgbaArray {
    return RgbaArray(readPixelsUnsafe(x, y, width, height, out.ints, offset))
}

/** Sets a pixel in [RGBA] from the slice in [x] and [y] with the color [RectSlice.value] WITHOUT having into account [RectSlice.orientation] and [RectSlice.padding] */
fun BmpSlice.readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: IntArray = IntArray(width * height), offset: Int = 0): IntArray {
    check(x in 0 until this.rect.width)
    check(y in 0 until this.rect.height)
    this.container.readPixelsUnsafe(this.rect.x + x, this.rect.y + y, width, height, out, offset)
    return out
}

/** Extract pixels in the same Bitmap format as the [RectSlice.container], without having into account [RectSlice.padding] nor [RectSlice.orientation] */
fun <T : Bitmap> RectSlice<T>.extractUntransformed(): T {
    val out = container.createWithThisFormat(rect.width, rect.height) as T
    this.container.copy(rect.x, rect.y, out, 0, 0, rect.width, rect.height)
    return out
}

/** Extract pixels in the same Bitmap format as the [RectSlice.container], having into account [RectSlice.padding] and [RectSlice.orientation] */
fun <T : Bitmap> RectSlice<T>.extract(): T {
    val w = if (!orientation.isRotatedDeg90CwOrCcw) rect.width else rect.height
    val h = if (!orientation.isRotatedDeg90CwOrCcw) rect.height else rect.width
    val out = container.createWithThisFormat(w + padding.leftPlusRight, h + padding.topPlusBottom) as T
    val raw = extractUntransformed().oriented(orientation)
    raw.copyUnchecked(0, 0, out, padding.left, padding.top, raw.width, raw.height)
    return out
}

fun <T : Bitmap> RectSlice<T>.toBitmap(): T = extract()

fun <T : SizeableInt> SliceCoordsWithBase<T>.slice(bounds: RectangleInt = RectangleInt(0, 0, width, height), name: String? = null, orientation: ImageOrientation = ImageOrientation.ROTATE_0, padding: MarginInt = MarginInt.ZERO): RectSlice<T> =
    RectSlice(
        this.base,
        // @TODO: This shouldn't be necessary. But ASE test fails without this
        RectangleInt.fromBounds(
            bounds.left.clamp(0, width),
            bounds.top.clamp(0, height),
            bounds.right.clamp(0, width),
            bounds.bottom.clamp(0, height),
        ),
        orientation,
        padding,
        name
    )
//fun <T : ISizeInt> CoordsWithContainer<T>.sliceWithBounds(left: Int, top: Int, right: Int, bottom: Int, name: String? = null, orientation: ImageOrientation = ImageOrientation.ORIGINAL, padding: MarginInt = MarginInt.ZERO): RectSlice<T> =
//    slice(RectangleInt(left, top, right - left, bottom - top), name, orientation, padding)
//fun <T : ISizeInt> CoordsWithContainer<T>.sliceWithSize(x: Int, y: Int, width: Int, height: Int, name: String? = null, orientation: ImageOrientation = ImageOrientation.ORIGINAL, padding: MarginInt = MarginInt.ZERO): RectSlice<T> =
//    slice(RectangleInt(x, y, width, height), name, orientation, padding)

fun <T : Bitmap> T.slice(bounds: RectangleInt = RectangleInt(0, 0, width, height), name: String? = null, orientation: ImageOrientation = ImageOrientation.ROTATE_0, padding: MarginInt = MarginInt.ZERO): RectSlice<T> {
    val left = bounds.left.clamp(0, width)
    val top = bounds.top.clamp(0, height)

    return RectSlice(
        this, RectangleInt.fromBounds(
            left,
            top,
            bounds.right.clamp(left, width),
            bounds.bottom.clamp(top, height),
        ), orientation, padding, name
    )
}
fun <T : Bitmap> T.sliceWithBounds(left: Int, top: Int, right: Int, bottom: Int, name: String? = null, orientation: ImageOrientation = ImageOrientation.ROTATE_0, padding: MarginInt = MarginInt.ZERO): RectSlice<T> =
    slice(RectangleInt(left, top, right - left, bottom - top), name, orientation, padding)
fun <T : Bitmap> T.sliceWithSize(x: Int, y: Int, width: Int, height: Int, name: String? = null, orientation: ImageOrientation = ImageOrientation.ROTATE_0, padding: MarginInt = MarginInt.ZERO): RectSlice<T> =
    slice(RectangleInt(x, y, width, height), name, orientation, padding)
