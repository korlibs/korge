package korlibs.image.bitmap

import korlibs.math.geom.*

data class BitmapWithHotspot<T : Bitmap> constructor (val bitmap: T, val hotspot: Vector2I) {
}
