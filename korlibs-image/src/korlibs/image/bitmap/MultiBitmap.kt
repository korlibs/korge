package korlibs.image.bitmap

open class MultiBitmap constructor(
    width: Int,
    height: Int,
    val bitmaps: List<Bitmap>,
    premultiplied: Boolean = true
) : Bitmap(width, height, 32, premultiplied, null) {
    constructor(bitmaps: List<Bitmap>, defaultWidth: Int = 0, defaultHeight: Int = 0, defaultPremultiplied: Boolean = true) : this(
        bitmaps.firstOrNull()?.width ?: defaultWidth,
        bitmaps.firstOrNull()?.height ?: defaultHeight,
        bitmaps,
        bitmaps.firstOrNull()?.premultiplied ?: defaultPremultiplied
    )
}
