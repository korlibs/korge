package com.soywiz.korge.build.swf

import com.soywiz.korge.animate.serialization.AniFile
import com.soywiz.korge.animate.serialization.writeTo
import com.soywiz.korge.build.ResourceProcessor
import com.soywiz.korge.ext.swf.readSWF
import com.soywiz.korge.ext.swf.swfExportConfig
import com.soywiz.korge.ext.swf.toAnLibrarySerializerConfig
import com.soywiz.korge.view.ViewsLog
import com.soywiz.korio.async.EventLoopTest
import com.soywiz.korio.vfs.VfsFile

object SwfResourceProcessor : ResourceProcessor("swf") {
	override val version: Int = AniFile.VERSION
	override val outputExtension: String = "ani"

	suspend override fun processInternal(inputFile: VfsFile, outputFile: VfsFile) {
		val eventLoopTest = EventLoopTest()
		val viewsLog = ViewsLog(eventLoopTest).apply { init() }
		val lib = inputFile.readSWF(viewsLog.views)
		val config = lib.swfExportConfig
		lib.writeTo(outputFile, config.toAnLibrarySerializerConfig(compression = 1.0))
	}
}
