package com.soywiz.korge.atlas

import com.soywiz.korge.resources.ResourceProcessor
import com.soywiz.korim.atlas.*
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.format.ImageEncodingProps
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.dynamic.mapper.*
import com.soywiz.korio.dynamic.serialization.stringifyTyped
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.util.*

open class AtlasResourceProcessor : ResourceProcessor("atlas") {
	companion object : AtlasResourceProcessor()

	override val version: Int = 0
	override val outputExtension: String = "atlas.json"

	override suspend fun processInternal(inputFile: VfsFile, outputFile: VfsFile) {
        val mapper = ObjectMapper().jvmFallback()

		// @TODO: Ignored file content. Use atlas to store information like max width/height, scale, etc.
		//val atlasPath0 = inputFile.readString().trim()
		//val atlasPath0 = ""
		//val atlasPath = if (atlasPath0.isNotEmpty()) atlasPath0 else inputFile.baseName
		val atlasPath = inputFile.baseNameWithoutExtension
		val atlasFolder = inputFile.parent[atlasPath].jail()
		//println("inputFile=$inputFile")
		//println("outputFile=$outputFile")
		//println("atlasPath=$atlasPath, atlasFolder=$atlasFolder")
		val files = atlasFolder.listRecursiveSimple { it.extensionLC == "png" || it.extensionLC == "jpg" }
		//println("atlasFiles=$files")

        if (files.isNotEmpty()) {

            val bitmaps = files.map { it.readBitmap().slice(name = it.baseName) }

            val outputImageFile = outputFile.withCompoundExtension("atlas.png")

            //println("outputImageFile=$outputImageFile")

            val atlases = AtlasPacker.pack(bitmaps, fileName = outputImageFile.baseName)
            val atlas = atlases.atlases.first()

            outputImageFile.write(
                PNG.encode(atlas.tex, ImageEncodingProps(filename = "file.png", quality = 1.0))
            )

            //println(Json.stringify(atlasInfo, pretty = true))

            outputFile.withCompoundExtension("atlas.json")
                .writeString(Json.stringifyTyped(atlas.atlasInfo, pretty = true, mapper = mapper))

            //Atlas.Factory()
            //println(files)
        }
	}
}
