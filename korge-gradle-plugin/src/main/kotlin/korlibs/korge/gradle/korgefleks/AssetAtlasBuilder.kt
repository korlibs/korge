package korlibs.korge.gradle.korgefleks

import com.android.build.gradle.internal.cxx.json.jsonStringOf
import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.IMAGES
import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.NINE_PATCHES
import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.PARALLAX_IMAGES
import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.PIXEL_FONTS
import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.TEXTURES
import korlibs.korge.gradle.texpacker.NewTexturePacker
import java.io.File
import java.util.ArrayList
import kotlin.String


class AssetAtlasBuilder(
    private val exportTilesDir: File,
    private val exportTilesetDir: File,
    private val gameResourcesDir: File,
    private val assetInfo: LinkedHashMap<String, Any>
) {

    fun buildAtlases(
        textureAtlasName: String,
        tilesetAtlasName: String,
        textureAtlasWidth: Int,
        textureAtlasHeight: Int,
        simplifyJson: Boolean
    ) {

        if (exportTilesDir.listFiles() != null && exportTilesDir.listFiles().isNotEmpty()) {
            // First build texture atlas
            val atlasInfoList = NewTexturePacker.packImages(exportTilesDir,
                enableRotation = false,
                enableTrimming = true,
                padding = 1,
                trimFileName = true,
                removeDuplicates = true,
                textureAtlasWidth = textureAtlasWidth,
                textureAtlasHeight = textureAtlasHeight
            )

            // Go through each generated atlas entry and map frames to asset info list
            val textures = assetInfo[TEXTURES] as ArrayList<String>
            val imagesInfo = assetInfo[IMAGES] as LinkedHashMap<String, Any>
            val ninePatchesInfo = assetInfo[NINE_PATCHES] as LinkedHashMap<String, Any>
            val pixelFontsInfo = assetInfo[PIXEL_FONTS] as LinkedHashMap<String, Any>
            val parallaxImageInfo = assetInfo[PARALLAX_IMAGES] as LinkedHashMap<String, Any>

            atlasInfoList.forEachIndexed { idx, atlasInfo ->
                val atlasOutputFile = gameResourcesDir.resolve("${textureAtlasName}_${idx}.atlas.png")
                atlasInfo.writeImage(atlasOutputFile)
                textures.add(atlasOutputFile.name)

                val frames = atlasInfo.info["frames"] as Map<String, Any>
                frames.forEach { (frameName, frameEntry) ->
                    frameEntry as Map<String, Any>
                    // Split frameName into frameTag and animation index number
                    val regex = "_(\\d+)$".toRegex()
                    val frameTag = frameName.replace(regex, "")
                    val animIndex = regex.find(frameName)?.groupValues?.get(1)?.toInt() ?: 0

                    // Check if this frame is an image (animation)
                    imagesInfo[frameTag]?.let { imageInfo ->
                        saveImageInfo(imageInfo as LinkedHashMap<String, Any>, frameEntry as Map<String, Int>, frameTag, idx, animIndex)
                    }

                    // Check if this frame is a nine-patch image
                    ninePatchesInfo[frameTag]?.let { ninePatchInfo ->
                        ninePatchInfo as LinkedHashMap<String, Any>
                        val frame = frameEntry["frame"] as Map<String, Int>

                        // Set frame info: [textureIndex, x, y, width, height]
                        ninePatchInfo["f"] = arrayOf(
                            idx,
                            frame["x"] ?: error("AssetConfig - frame x is null for sprite '${frameName}'!"),
                            frame["y"] ?: error("AssetConfig - frame y is null for sprite '${frameName}'!"),
                            frame["w"] ?: error("AssetConfig - frame w is null for sprite '${frameName}'!"),
                            frame["h"] ?: error("AssetConfig - frame h is null for sprite '${frameName}'!")
                        )
                        // Nine-patch center info was already set by AssetImageLoader during export
                    }

                    // Check if this frame is a pixel font map image
                    pixelFontsInfo[frameTag]?.let { pixelFontInfo ->
                        pixelFontInfo as LinkedHashMap<String, Any>
                        val frame = frameEntry["frame"] as Map<String, Int>

                        // Set frame info: [textureIndex, x, y, width, height]
                        pixelFontInfo["f"] = arrayOf(
                            idx,
                            frame["x"] ?: error("AssetConfig - frame x is null for sprite '${frameName}'!"),
                            frame["y"] ?: error("AssetConfig - frame y is null for sprite '${frameName}'!"),
                            frame["w"] ?: error("AssetConfig - frame w is null for sprite '${frameName}'!"),
                            frame["h"] ?: error("AssetConfig - frame h is null for sprite '${frameName}'!")
                        )
                    }

                    parallaxImageInfo[frameTag]?.let { parallaxInfo ->
                        saveImageInfo(parallaxInfo as LinkedHashMap<String, Any>, frameEntry as Map<String, Int>, frameTag, idx, animIndex)
                    }
                }
            }

            // Finally, write out the asset info as JSON file
            val assetInfoJsonFile = gameResourcesDir.resolve("${textureAtlasName}.atlas.json")
            assetInfoJsonFile.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) error("Failed to create directory: ${parent.path}")
                val jsonString = jsonStringOf(assetInfo)
                // Simplify JSON string by removing unnecessary spaces and line breaks
                val simplifiedJsonString = if (simplifyJson) jsonString.replace(Regex("\\s+"), "")
                else jsonString
                assetInfoJsonFile.writeText(simplifiedJsonString)
            }
        }

        // Then build tilesets atlas
        if (exportTilesetDir.listFiles() != null && exportTilesetDir.listFiles().isNotEmpty()) {
            val atlasInfoList = NewTexturePacker.packTilesets(exportTilesetDir)
            atlasInfoList.forEachIndexed { idx, atlasInfo ->
                val atlasOutputFile = gameResourcesDir.resolve("${tilesetAtlasName}_${idx}.atlas")
                atlasInfo.writeImage(atlasOutputFile)
            }
        }
    }

    private fun saveImageInfo(imageInfo: LinkedHashMap<String, Any>, frameEntry: Map<String, Int>, frameTag: String, idx: Int, animIndex: Int) {
        // Ensure the frames list is large enough and set the frame at the correct index
        val framesInfo = imageInfo["f"] as MutableList<LinkedHashMap<String, Any>>
        if (animIndex >= framesInfo.size) error("AssetConfig - Animation index ${animIndex} out of bounds for sprite '${frameTag}' with ${framesInfo.size} frames!")

        val spriteSource = frameEntry["spriteSourceSize"] as Map<String, Int>
        val sourceSize = frameEntry["sourceSize"] as Map<String, Int>
        val frame = frameEntry["frame"] as Map<String, Int>

        // Set frame info for animIndex: [textureIndex, x, y, width, height]
        framesInfo[animIndex]["f"] = arrayOf(
            idx,
            frame["x"] ?: error("AssetConfig - frame x is null for sprite '${frameTag}' index '$idx'!"),
            frame["y"] ?: error("AssetConfig - frame y is null for sprite '${frameTag}' index '$idx'!"),
            frame["w"] ?: error("AssetConfig - frame w is null for sprite '${frameTag}' index '$idx'!"),
            frame["h"] ?: error("AssetConfig - frame h is null for sprite '${frameTag}' index '$idx'!")
        )
        framesInfo[animIndex]["x"] = spriteSource["x"] ?: error("AssetConfig - spriteSource x is null for sprite '${frameTag}'!")
        framesInfo[animIndex]["y"] = spriteSource["y"] ?: error("AssetConfig - spriteSource y is null for sprite '${frameTag}'!")
        // Do not set duration here - it was already set by AssetImageLoader from ASEInfo during texture export from Aseprite

        imageInfo["w"] = sourceSize["w"] ?: error("AssetConfig - sourceSize w is null for sprite '${frameTag}'!")
        imageInfo["h"] = sourceSize["h"] ?: error("AssetConfig - sourceSize h is null for sprite '${frameTag}'!")
    }
}
