package com.soywiz.korim.bitmap

import com.soywiz.korma.geom.RectangleInt

/**
 * Finds the rectangle inside this BmpSlice pixels that contains all the non-transparent pixels.
 * The rectangle coordinates are relative to the slice.
 */
fun BmpSlice.findNonTransparentBounds(): RectangleInt {
    var left = width
    var right = width
    var top = height
    var bottom = height
    for (y in 0 until height) {
        for (x in 0 until width) if (getAlpha(x, y) != 0) { left = kotlin.math.min(left, x); break }
        for (x in 0 until width) if (getAlpha(width - x - 1, y) != 0) { right = kotlin.math.min(right, x); break }
    }
    for (x in 0 until width) {
        for (y in 0 until height) if (getAlpha(x, y) != 0) { top = kotlin.math.min(top, y); break }
        for (y in 0 until height) if (getAlpha(x, height - y - 1) != 0) { bottom = kotlin.math.min(bottom, y); break }
    }
    return RectangleInt.fromBounds(left, top, width - right, height - bottom)
}

/**
 * Creates a trimmed version of the [BmpSlice] with a virtual frame included.
 */
fun BmpSlice.trimmed(): BmpSlice {
    val tbounds = findNonTransparentBounds()
    return this.bmp.slice(
        name = name,
        bounds = RectangleInt(
            this.left + tbounds.left, this.top + tbounds.top,
            tbounds.width, tbounds.height
        ),
        virtFrame = RectangleInt(
            tbounds.left, tbounds.top,
            this.width, this.height
        ),
    )
}
