package korlibs.image.format

import korlibs.datastructure.*
import korlibs.image.atlas.*
import korlibs.image.bitmap.*
import korlibs.io.file.*
import korlibs.math.geom.slice.*

typealias ImageOrientation = SliceOrientation

suspend fun VfsFile.readBitmapSliceWithOrientation(props: ImageDecodingProps = ImageDecodingProps.DEFAULT, name: String? = null, atlas: MutableAtlasUnit? = null): BmpSlice {
    // @TODO: Support other formats providing orientation information in addition to EXIF?
    val result = kotlin.runCatching { EXIF.readExifFromJpeg(this) }
    val slice = readBitmapSlice(name, atlas, props)
    return when {
        result.isSuccess -> slice.copy(orientation = result.getOrThrow().orientationSure)
        else -> slice
    }
}

var ImageInfo.orientation: ImageOrientation? by Extra.Property { null }
val ImageInfo?.orientationSure: ImageOrientation get() = this?.orientation ?: ImageOrientation.ROTATE_0
