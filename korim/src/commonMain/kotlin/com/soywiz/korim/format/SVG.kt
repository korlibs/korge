package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.vector.render
import com.soywiz.korio.lang.UTF8
import com.soywiz.korio.lang.toString
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.stream.readString
import com.soywiz.korio.stream.sliceStart
import kotlin.math.min

object SVG : ImageFormat("svg") {
	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? = kotlin.runCatching {
        val start = s.sliceStart().readString(min(100, s.length.toInt())).trim().toLowerCase()
        if (start.startsWith("<svg", ignoreCase = true) || start.startsWith("<?xml", ignoreCase = true) || start.startsWith("<!--")) {
            try {
                val content = s.sliceStart().readAll().toString(UTF8).trim()
                val svg = com.soywiz.korim.vector.format.SVG(content)
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

	override fun readImage(s: SyncStream, props: ImageDecodingProps, out: Bitmap?): ImageData {
		val content = s.sliceStart().readAll().toString(UTF8).trim()
		val svg = com.soywiz.korim.vector.format.SVG(content)
		return ImageData(listOf(ImageFrame(svg.render().toBMP32())))
	}
}
