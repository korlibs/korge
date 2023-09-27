package korlibs.image.format

import korlibs.image.vector.render
import korlibs.io.lang.UTF8
import korlibs.io.lang.toString
import korlibs.io.stream.SyncStream
import korlibs.io.stream.readAll
import korlibs.io.stream.readString
import korlibs.io.stream.sliceStart
import kotlin.math.min

object SVG : ImageFormat("svg") {
	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? = kotlin.runCatching {
        val start = s.sliceStart().readString(min(100, s.length.toInt())).trim().toLowerCase()
        if (start.startsWith("<svg", ignoreCase = true) || start.startsWith("<?xml", ignoreCase = true) || start.startsWith("<!--")) {
            try {
                val content = s.sliceStart().readAll().toString(UTF8).trim()
                val svg = korlibs.image.vector.format.SVG(content)
                ImageInfo().apply {
                    width = svg.width
                    height = svg.height
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }.getOrNull()

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
		val content = s.sliceStart().readAll().toString(UTF8).trim()
		val svg = korlibs.image.vector.format.SVG(content)
		return ImageData(svg.render().toBMP32())
	}
}
