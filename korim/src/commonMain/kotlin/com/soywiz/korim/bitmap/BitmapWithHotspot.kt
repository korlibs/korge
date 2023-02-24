package com.soywiz.korim.bitmap

import com.soywiz.korma.annotations.*
import com.soywiz.korma.geom.*

data class BitmapWithHotspot<T : Bitmap> @KormaValueApi constructor (val bitmap: T, val hotspot: PointInt) {
    @KormaMutableApi constructor(bitmap: T, hotspot: IPointInt) : this(bitmap, hotspot.point)
    @KormaMutableApi val mhotspot: IPointInt = hotspot.mutable
}
