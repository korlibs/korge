package com.soywiz.korim.bitmap

import com.soywiz.korma.geom.IPointInt

data class BitmapWithHotspot<T : Bitmap>(val bitmap: T, val hotspot: IPointInt)
