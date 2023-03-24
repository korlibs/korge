package korlibs.image.bitmap

import korlibs.math.annotations.*
import korlibs.math.geom.*

data class BitmapWithHotspot<T : Bitmap> @KormaValueApi constructor (val bitmap: T, val hotspot: Vector2Int) {
    @KormaMutableApi constructor(bitmap: T, hotspot: MPointInt) : this(bitmap, hotspot.point)
    @KormaMutableApi val mhotspot: MPointInt = hotspot.mutable
}