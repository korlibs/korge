package com.soywiz.korim.format

import com.soywiz.kds.*
import com.soywiz.korim.atlas.MutableAtlasUnit
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.file.VfsFile
import com.soywiz.korma.geom.slice.*
import kotlin.native.concurrent.ThreadLocal

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

@ThreadLocal
var ImageInfo.orientation: ImageOrientation? by Extra.Property { null }
val ImageInfo?.orientationSure: ImageOrientation get() = this?.orientation ?: ImageOrientation.ROTATE_0

