package korlibs.image.format

import korlibs.image.bitmap.*
import korlibs.image.tiles.*

/**
 * This class is used to store the layer image from e.g. an aseprite image file.
 *
 * @param layer   Stores the index and the name of the layer.
 * @param slice   This is the actual bitmap image.
 * @param targetX X offset in pixel from the top left corner of the overall image.
 * @param targetY Y offset in pixel from the top left corner of the overall image.
 * Offsets are used to crop empty space around the sprite images.
 *
 * [...]
 *
 */
open class ImageFrameLayer(
    val layer: ImageLayer,
    slice: BmpSlice,
    val targetX: Int = 0,
    val targetY: Int = 0,
    val main: Boolean = true,
    val includeInAtlas: Boolean = true,
    val tilemap: TileMapData? = null,
    val ninePatchSlice: NinePatchBmpSlice? = null,
) {
    private var _bitmap: Bitmap? = null
    private var _bitmap32: Bitmap32? = null

    var slice: BmpSlice = slice
        set(value) {
            field = value
            _bitmap = null
            _bitmap32 = null
        }

    val width get() = slice.width
    val height get() = slice.height
    val area: Int get() = slice.area
    val bitmap: Bitmap get() {
        if (slice.bmp.width == width && slice.bmp.height == height) return slice.bmp
        if (_bitmap == null) _bitmap = slice.extract()
        return _bitmap!!
    }
    val bitmap32: Bitmap32 get() {
        //if (_bitmap32 == null) _bitmap32 = bitmap.toBMP32IfRequired()
        if (_bitmap32 == null) _bitmap32 = bitmap.toBMP32()
        return _bitmap32!!
    }
}
