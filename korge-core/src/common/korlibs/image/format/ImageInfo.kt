package korlibs.image.format

import korlibs.datastructure.*
import korlibs.math.geom.*

open class ImageInfo : Sizeable, Extra by Extra.Mixin() {
	var width: Int = 0
	var height: Int = 0
	var bitsPerPixel: Int = 8

	override val size: Size get() = Size(width, height)

    override fun toString(): String = "ImageInfo(width=$width, height=$height, bpp=$bitsPerPixel, extra=${extra?.toMap()})"
}

fun ImageInfo(block: ImageInfo.() -> Unit): ImageInfo = ImageInfo().apply(block)
