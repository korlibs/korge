package com.soywiz.korim.format

open class ImageDataContainer(
    val imageDatas: List<ImageData>
) {
    val imageDatasByName = imageDatas.associateBy { it.name }
    val default = imageDatasByName[null] ?: imageDatas.first()

    operator fun get(name: String?): ImageData? = imageDatasByName[name]
}
