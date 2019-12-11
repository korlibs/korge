package com.soywiz.korge3d

import com.soywiz.korge.resources.*
import com.soywiz.korge3d.format.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.xml.*

open class ColladaResourceProcessor : ResourceProcessor("dae") {
	companion object : ColladaResourceProcessor()

	override val version: Int = 1
	override val outputExtension: String = "ks3d"

	override suspend fun processInternal(inputFile: VfsFile, outputFile: VfsFile) {
		val library = ColladaParser.parse(inputFile.readXml())
		outputFile.writeKs3d(library)
	}
}
