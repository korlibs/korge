package com.soywiz.korge.ext.swf

import com.soywiz.korge.animate.serialization.AniFile
import com.soywiz.korge.animate.serialization.writeTo
import com.soywiz.korge.resources.ResourceProcessor
import com.soywiz.korge.view.ViewsLog
import com.soywiz.korio.file.VfsFile
import kotlin.coroutines.coroutineContext

open class SwfResourceProcessor : ResourceProcessor("swf") {
	companion object : SwfResourceProcessor()

	override val version: Int = AniFile.VERSION
	override val outputExtension: String = "ani"

	override suspend fun processInternal(inputFile: VfsFile, outputFile: VfsFile) {
		val viewsLog = ViewsLog(coroutineContext)
		val lib = inputFile.readSWF(viewsLog.views)
		val config = lib.swfExportConfig
		lib.writeTo(outputFile, config.toAnLibrarySerializerConfig(compression = 1.0))
	}
}
