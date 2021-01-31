package com.soywiz.korim.format

import com.soywiz.korim.internal.*
import com.soywiz.korim.internal.min2
import com.soywiz.korim.vector.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import kotlin.math.*

object SVG : ImageFormat("svg") {
	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? = kotlin.runCatching {
        val start = s.sliceStart().readString(min2(100, s.length.toInt())).trim().toLowerCase()
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

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
		val content = s.sliceStart().readAll().toString(UTF8).trim()
		val svg = com.soywiz.korim.vector.format.SVG(content)
		return ImageData(listOf(ImageFrame(svg.render().toBMP32())))
	}
}
