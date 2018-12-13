package com.soywiz.korge.build.swf

import com.soywiz.korge.animate.serialization.*
import com.soywiz.korge.build.*
import com.soywiz.korge.ext.swf.*
import com.soywiz.korge.view.*
import com.soywiz.korio.file.*

object SwfResourceProcessor : ResourceProcessor("swf") {
	override val version: Int = AniFile.VERSION
	override val outputExtension: String = "ani"

	override suspend fun processInternal(inputFile: VfsFile, outputFile: VfsFile) {
		val viewsLog = ViewsLog()
		val lib = inputFile.readSWF(viewsLog.views)
		val config = lib.swfExportConfig
		lib.writeTo(outputFile, config.toAnLibrarySerializerConfig(compression = 1.0))
	}
}
