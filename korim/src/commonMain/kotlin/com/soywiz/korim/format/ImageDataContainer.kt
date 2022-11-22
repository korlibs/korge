package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*

open class ImageDataContainer(
    val imageDatas: List<ImageData>
) {
    constructor(bitmap: Bitmap) : this(listOf(ImageData(bitmap)))

    val imageDatasByName = imageDatas.associateBy { it.name }
    val default = imageDatasByName[null] ?: imageDatas.first()
    val mainBitmap get() = default.mainBitmap

    operator fun get(name: String?): ImageData? = imageDatasByName[name]
}
