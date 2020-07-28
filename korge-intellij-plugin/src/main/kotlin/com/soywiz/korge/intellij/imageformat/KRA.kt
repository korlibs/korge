package com.soywiz.korge.intellij.imageformat

import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

object KRA : ImageFormat("kra") {
	private val mergedImagePng = "mergedimage.png"

	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
		if (s.clone().readBytesExact(2).toString(UTF8) != "PK") return null
		var out: ImageInfo? = null
		runBlockingNoSuspensions {
			val vfs = ZipVfs(s.clone().toAsync())
			val mergedFile = vfs[mergedImagePng]
			val mergedBytes = mergedFile.readRangeBytes(0L..128)
			out = PNG.decodeHeader(mergedBytes.openSync(), props)
		}
		return out
	}

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
		return runBlockingNoSuspensions {
			val vfs = ZipVfs(s.clone().toAsync())
			val mergedBytes = vfs[mergedImagePng].readAll()
			PNG.readImage(mergedBytes.openSync())
		}
	}
}
