package korlibs.korge.atlas

/*
import korlibs.korge.resources.ResourceProcessor
import korlibs.image.atlas.*
import korlibs.image.bitmap.slice
import korlibs.image.format.ImageEncodingProps
import korlibs.image.format.PNG
import korlibs.image.format.readBitmap
import korlibs.io.file.*

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
                .writeString(
                    atlas.atlasInfo.toJsonString()
                )

            //Atlas.Factory()
            //println(files)
        }
	}
}
*/
