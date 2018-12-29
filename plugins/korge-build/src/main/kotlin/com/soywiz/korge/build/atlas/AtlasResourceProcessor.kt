package com.soywiz.korge.build.atlas

import com.soywiz.kmem.*
import com.soywiz.korge.atlas.*
import com.soywiz.korge.build.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.json.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.binpack.*

object AtlasResourceProcessor : ResourceProcessor("atlas") {
	override val version: Int = 0
	override val outputExtension: String = "atlas.json"

	override suspend fun processInternal(inputFile: VfsFile, outputFile: VfsFile) {
		val atlasPath0 = inputFile.readString().trim()
		val atlasPath = if (atlasPath0.isNotEmpty()) atlasPath0 else inputFile.basename
		val atlasFolder = inputFile.parent[atlasPath].jail()
		val files = atlasFolder.listRecursive { it.extensionLC == "png" || it.extensionLC == "jpg" }.toList()

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
		val pack = packs.first()
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
				val rect = it.second!!.displaced(2.0, 2.0)
				val irect = rect.toInt()
				file.path.trim('/') to AtlasInfo.Entry(
					frame = AtlasInfo.Rect(irect.x, irect.y, irect.width, irect.height),
					rotated = false,
					sourceSize = AtlasInfo.Size(irect.width, irect.height),
					spriteSourceSize = AtlasInfo.Rect(0, 0, irect.width, irect.height),
					trimmed = false
				)
			}.toMap(),
			meta = AtlasInfo.Meta(
				app = "korge",
				format = "RGBA8888",
				image = outputImageFile.basename,
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

		outputFile.withCompoundExtension("atlas.json").writeString(Json.stringify(atlasInfo, pretty = true))

		//Atlas.Factory()
		//println(files)
	}
}
