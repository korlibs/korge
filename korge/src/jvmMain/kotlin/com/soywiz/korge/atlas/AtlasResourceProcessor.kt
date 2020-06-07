package com.soywiz.korge.atlas

import com.soywiz.kmem.nextAlignedTo
import com.soywiz.korge.resources.ResourceProcessor
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.format.ImageEncodingProps
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.dynamic.mapper.Mapper
import com.soywiz.korio.dynamic.serialization.stringifyTyped
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.baseNameWithoutExtension
import com.soywiz.korio.file.extensionLC
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

		val bitmaps = files.map { it to it.readBitmap() }

		val packs = BinPacker.packSeveral(2 * 4096.0, 2 * 4096.0, bitmaps) {
			Size(
				(it.second.width + 4).nextAlignedTo(4),
				(it.second.height + 4).nextAlignedTo(4)
			)
		}
		if (packs.size != 1) {
			println("Atlas packer failed: ${packs.size}")
		}

		val dummyBmp = Bitmap32(2, 2)
		val pack = packs.firstOrNull() ?: BinPacker.Result(
			16.0, 16.0, listOf(
				(MemoryVfsMix("dummy.png" to dummyBmp.encode(PNG))["dummy.png"] to dummyBmp) to Rectangle(2, 2, 4, 4)
			)
		)
		val out = Bitmap32(pack.width.toInt(), pack.height.toInt())

		for (entry in pack.items) {
			val file = entry.first.first
			val bmp = entry.first.second
			val rect = entry.second!!
			out.put(bmp.toBMP32(), rect.x.toInt() + 2, rect.y.toInt() + 2)
		}

		val outputImageFile = outputFile.withCompoundExtension("atlas.png")

		val atlasInfo = AtlasInfo(
			frames = pack.items.map {
				val file = it.first.first
                val bmp = it.first.second
				val rect = Rectangle(it.second!!.x + 2, it.second!!.y + 2, bmp.width, bmp.height)
				val irect = rect.toInt()
                val filename = file.path.trim('/')
                AtlasInfo.Entry(
                    filename = filename,
					frame = AtlasInfo.Rect(irect.x, irect.y, irect.width, irect.height),
					rotated = false,
					sourceSize = AtlasInfo.Size(irect.width, irect.height),
					spriteSourceSize = AtlasInfo.Rect(0, 0, irect.width, irect.height),
					trimmed = false
				)
			},
			meta = AtlasInfo.Meta(
				app = "korge",
				format = "RGBA8888",
				image = outputImageFile.baseName,
				scale = 1.0,
				size = AtlasInfo.Size(out.width, out.height),
				version = AtlasInfo.Meta.VERSION
			)
		)

		//showImageAndWait(out)

		outputImageFile.write(
			PNG.encode(out, ImageEncodingProps(filename = "file.png", quality = 1.0))
		)

		//println(Json.stringify(atlasInfo, pretty = true))

		outputFile.withCompoundExtension("atlas.json")
			.writeString(Json.stringifyTyped(atlasInfo, pretty = true, mapper = Mapper))

		//Atlas.Factory()
		//println(files)
	}
}
