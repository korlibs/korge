package korlibs.image.format

import korlibs.image.bitmap.*

open class ImageDataContainer(
    val imageDatas: List<ImageData>
) {
    constructor(bitmap: Bitmap) : this(listOf(ImageData(bitmap)))

    val imageDatasByName: Map<String?, ImageData> = imageDatas.associateBy { it.name }
    val default: ImageData = imageDatasByName[null] ?: imageDatas.first()
    val mainBitmap: Bitmap get() = default.mainBitmap

    operator fun get(name: String?): ImageData? = imageDatasByName[name]
}
