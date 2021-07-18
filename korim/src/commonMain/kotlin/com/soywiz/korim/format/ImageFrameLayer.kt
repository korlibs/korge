package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*

open class ImageFrameLayer constructor(
    val layer: ImageLayer,
    slice: BmpSlice,
    val targetX: Int = 0,
    val targetY: Int = 0,
    val main: Boolean = true,
    val includeInAtlas: Boolean = true,
    val linkedFrameLayer: ImageFrameLayer? = null,
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
        if (_bitmap == null) _bitmap = slice.extract()
        return _bitmap!!
    }
    val bitmap32: Bitmap32 get() {
        //if (_bitmap32 == null) _bitmap32 = bitmap.toBMP32IfRequired()
        if (_bitmap32 == null) _bitmap32 = bitmap.toBMP32()
        return _bitmap32!!
    }
}
