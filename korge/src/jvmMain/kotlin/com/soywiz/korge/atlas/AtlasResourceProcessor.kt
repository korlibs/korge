package com.soywiz.korge.atlas

import com.soywiz.kmem.nextAlignedTo
import com.soywiz.korge.resources.ResourceProcessor
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.format.ImageEncodingProps
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.dynamic.mapper.Mapper
import com.soywiz.korio.dynamic.serialization.stringifyTyped
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.MemoryVfsMix
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.binpack.BinPacker
import kotlinx.coroutines.channels.toList

open class AtlasResourceProcessor : ResourceProcessor("atlas") {
	companion object : AtlasResourceProcessor()

	override val version: Int = 0
	override val outputExtension: String = "atlas.json"

	override suspend fun processInternal(inputFile: VfsFile, outputFile: VfsFile) {
		// @TODO: Ignored file content. Use atlas to store information like max width/height, scale, etc.
		//val atlasPath0 = inputFile.readString().trim()
		//val atlasPath0 = ""
		//val atlasPath = if (atlasPath0.isNotEmpty()) atlasPath0 else inputFile.baseName
		val atlasPath = inputFile.baseNameWithoutExtension
		val atlasFolder = inputFile.parent[atlasPath].jail()
		//println("inputFile=$inputFile")
		//println("outputFile=$outputFile")
		//println("atlasPath=$atlasPath, atlasFolder=$atlasFolder")
		val files = atlasFolder.listRecursive { it.extensionLC == "png" || it.extensionLC == "jpg" }.toList()
		//println("atlasFiles=$files")

		val bitmaps = files.map { it.readBitmap().slice(name = it.baseName) }

		val outputImageFile = outputFile.withCompoundExtension("atlas.png")

        val atlases = AtlasPacker.pack(bitmaps)
        val atlas = atlases.atlases.first()

		outputImageFile.write(
			PNG.encode(atlas.tex, ImageEncodingProps(filename = "file.png", quality = 1.0))
		)

		//println(Json.stringify(atlasInfo, pretty = true))

		outputFile.withCompoundExtension("atlas.json")
			.writeString(Json.stringifyTyped(atlas.atlasInfo, pretty = true, mapper = Mapper))

		//Atlas.Factory()
		//println(files)
	}
}
