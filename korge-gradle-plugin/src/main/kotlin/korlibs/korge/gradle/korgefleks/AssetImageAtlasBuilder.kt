package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.IMAGES
import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.NINE_PATCHES
import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.PARALLAX_LAYERS
import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.PIXEL_FONTS
import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.TEXTURES
import korlibs.korge.gradle.korgefleks.ParallaxInfo.*
import korlibs.korge.gradle.texpacker.NewTexturePacker
import java.io.File
import java.util.ArrayList
import kotlin.String


/**
 * Builds texture atlases from exported tile images and updates asset information accordingly.
 *
 * @param exportTilesDir The directory containing exported tile images.
 * @param gameResourcesDir The directory where the generated texture atlases will be stored.
 * @param assetInfo The asset information map to be updated with texture atlas data.
 */
class AssetImageAtlasBuilder(
    private val exportTilesDir: File,
    private val gameResourcesDir: File,
    private val assetInfo: LinkedHashMap<String, Any>
) {
    /**
     * Builds a texture atlas from the exported tiles in the export tiles directory.
     * Adds the texture atlas information to the internal asset info list.
     */
    fun buildAtlases(
        textureAtlasName: String,
        atlasWidth: Int,
        atlasHeight: Int,
        atlasPadding: Int
    ) {
        if (exportTilesDir.listFiles() != null && exportTilesDir.listFiles().isNotEmpty()) {
            // First build texture atlas
            val atlasInfoList = NewTexturePacker.packImages(exportTilesDir,
                enableRotation = false,
                enableTrimming = true,
                padding = atlasPadding,
                trimFileName = true,
                removeDuplicates = true,
                textureAtlasWidth = atlasWidth,
                textureAtlasHeight = atlasHeight
            )

            // Go through each generated atlas entry and map frames to asset info list
            val textures = assetInfo[TEXTURES] as ArrayList<String>
            val imagesInfo = assetInfo[IMAGES] as LinkedHashMap<String, Any>
            val ninePatchesInfo = assetInfo[NINE_PATCHES] as LinkedHashMap<String, Any>
            val pixelFontsInfo = assetInfo[PIXEL_FONTS] as LinkedHashMap<String, Any>
            val parallaxLayersInfo = assetInfo[PARALLAX_LAYERS] as LinkedHashMap<String, Any>

            atlasInfoList.forEachIndexed { idx, atlasInfo ->
                val atlasOutputFile = gameResourcesDir.resolve("${textureAtlasName}_${idx}.atlas.png")
                atlasInfo.writeImage(atlasOutputFile)
                textures.add(atlasOutputFile.name)

                // Store parallax size info which is extracted from any image frame found for the parallax layers
                var parallaxWidth = 0
                var parallaxHeight = 0

                // Go through each frame in the atlas and map it to the correct asset info entry
                val frames = atlasInfo.info["frames"] as Map<String, Any>
                frames.forEach { (frameName, frameEntry) ->
                    frameEntry as Map<String, Any>
                    // Split frameName into frameTag and animation index number
                    val regex = "_(\\d+)$".toRegex()
                    val frameTag = frameName.replace(regex, "")
                    val animIndex = regex.find(frameName)?.groupValues?.get(1)?.toInt() ?: 0

                    // (1) Check if this frame is an image (animation)
                    imagesInfo[frameTag]?.let { imageInfo ->
                        saveImageInfo(imageInfo as LinkedHashMap<String, Any>, frameEntry as Map<String, Int>, frameTag, idx, animIndex)
                    }

                    // (2) Check if this frame is a nine-patch image
                    ninePatchesInfo[frameTag]?.let { ninePatchInfo ->
                        ninePatchInfo as LinkedHashMap<String, Any>
                        val frame = frameEntry["frame"] as Map<String, Int>

                        // Set frame info: [textureIndex, x, y, width, height]
                        ninePatchInfo["f"] = arrayOf(
                            idx,
                            frame["x"] ?: error("NinePatchImageBuilder - frame x is null for sprite '${frameName}'!"),
                            frame["y"] ?: error("NinePatchImageBuilder - frame y is null for sprite '${frameName}'!"),
                            frame["w"] ?: error("NinePatchImageBuilder - frame w is null for sprite '${frameName}'!"),
                            frame["h"] ?: error("NinePatchImageBuilder - frame h is null for sprite '${frameName}'!")
                        )
                        // Nine-patch center info was already set by AssetImageLoader during export
                    }

                    // (3) Check if this frame is a pixel font map image
                    pixelFontsInfo[frameTag]?.let { pixelFontInfo ->
                        pixelFontInfo as LinkedHashMap<String, Any>
                        val frame = frameEntry["frame"] as Map<String, Int>

                        // Set frame info: [textureIndex, x, y, width, height]
                        pixelFontInfo["f"] = arrayOf(
                            idx,
                            frame["x"] ?: error("PixelFontImageBuilder - frame x is null for sprite '${frameName}'!"),
                            frame["y"] ?: error("PixelFontImageBuilder - frame y is null for sprite '${frameName}'!"),
                            frame["w"] ?: error("PixelFontImageBuilder - frame w is null for sprite '${frameName}'!"),
                            frame["h"] ?: error("PixelFontImageBuilder - frame h is null for sprite '${frameName}'!")
                        )
                    }

                    // (4) Check if this frame is a parallax layer image
                    parallaxLayersInfo.forEach { (_, parallaxLayerInfo) ->
                        parallaxLayerInfo as LinkedHashMap<String, Any>
                        // Get offset info
                        val offsetX = parallaxLayerInfo["offsetX"]?.let { it as Int } ?: error("AssetConfig - parallaxLayerInfo offsetX is null")
                        val offsetY = parallaxLayerInfo["offsetY"]?.let { it as Int } ?: error("AssetConfig - parallaxLayerInfo offsetY is null")
                        val parallaxMode = parallaxLayerInfo["m"]?.let { it as Mode } ?: error("AssetConfig - parallaxLayerInfo mode is null")

                        fun saveLayerInfo(layerInfo: LinkedHashMap<String, Any>) {
                            val layerName = layerInfo["n"]?.let { it as String } ?: error("ParallaxImageBuilder - layer name is null for parallax layer!")
                            if (layerName == frameTag) {
                                val sourceSize = frameEntry["sourceSize"]?.let { it as Map<String, Int> } ?: error("ParallaxImageBuilder - sourceSize is null for sprite '${frameTag}'!")
                                val frame = frameEntry["frame"]?.let { it as Map<String, Int> } ?: error("ParallaxImageBuilder - frame is null for sprite '${frameTag}'!")
                                val spriteSource = frameEntry["spriteSourceSize"]?.let { it as Map<String, Int> } ?: error("ParallaxImageBuilder - spriteSourceSize is null for sprite '${frameTag}'!")

                                // Save parallax size (possibly redundant if multiple frames exist for the same layer)
                                parallaxWidth = sourceSize["w"]?.let { it + offsetX } ?: error("ParallaxImageBuilder - sourceSize w is null for sprite '${frameTag}'!")
                                parallaxHeight = sourceSize["h"]?.let { it + offsetY } ?: error("ParallaxImageBuilder - sourceSize h is null for sprite '${frameTag}'!")
                                parallaxLayerInfo["w"] = parallaxWidth
                                parallaxLayerInfo["h"] = parallaxHeight

                                // Set layer position (targetX/Y) within parallax background
                                layerInfo["tx"] = spriteSource["x"]?.let { it + offsetX } ?: error("ParallaxImageBuilder - spriteSource x is null for sprite '${frameTag}'!")
                                layerInfo["ty"] = spriteSource["y"]?.let { it + offsetY } ?: error("ParallaxImageBuilder - spriteSource y is null for sprite '${frameTag}'!")

                                // Set frame info for animIndex: [textureIndex, x, y, width, height]
                                layerInfo["f"] = arrayOf(
                                    idx,  // Save index to texture atlas where the frame is located
                                    frame["x"] ?: error("ParallaxImageBuilder - frame x is null for sprite '${frameTag}' index '$idx'!"),
                                    frame["y"] ?: error("ParallaxImageBuilder - frame y is null for sprite '${frameTag}' index '$idx'!"),
                                    frame["w"] ?: error("ParallaxImageBuilder - frame w is null for sprite '${frameTag}' index '$idx'!"),
                                    frame["h"] ?: error("ParallaxImageBuilder - frame h is null for sprite '${frameTag}' index '$idx'!")
                                )
                            }
                        }

                        // Check if this frame belongs to background or foreground layers (lists might not exist, which is fine)
                        val backgroundLayerInfo = parallaxLayerInfo["b"]?.let { it as ArrayList<LinkedHashMap<String, Any>> } ?: emptyList()
                        backgroundLayerInfo.forEach { layerInfo -> saveLayerInfo(layerInfo) }
                        val foregroundLayerInfo = parallaxLayerInfo["f"]?.let { it as ArrayList<LinkedHashMap<String, Any>> } ?: emptyList()
                        foregroundLayerInfo.forEach { layerInfo -> saveLayerInfo(layerInfo) }

                        val parallaxPlaneInfo = parallaxLayerInfo["p"] as LinkedHashMap<String, Any>?
                        parallaxPlaneInfo?.let { parallaxPlaneInfo ->
                            val planeName = parallaxPlaneInfo["n"]?.let { it as String } ?: error("ParallaxImageBuilder - plane name is null for parallax plane!")

                            fun saveLayerInfo(layerInfo: LinkedHashMap<String, Any>, attachBottomRight: Boolean = false) {
                                val layerName = layerInfo["n"]?.let { it as String } ?: error("ParallaxImageBuilder - attached layer name is null for parallax layer!")
                                if (layerName == frameTag) {
                                    val sourceSize = frameEntry["sourceSize"]?.let { it as Map<String, Int> } ?: error("ParallaxImageBuilder - sourceSize is null for sprite '${frameTag}'!")
                                    val frame = frameEntry["frame"]?.let { it as Map<String, Int> } ?: error("ParallaxImageBuilder - frame is null for sprite '${frameTag}'!")
                                    val spriteSource = frameEntry["spriteSourceSize"]?.let { it as Map<String, Int> } ?: error("ParallaxImageBuilder - spriteSourceSize is null for sprite '${frameTag}'!")

                                    // Save parallax size (possibly redundant if multiple frames exist for the same layer)
                                    parallaxWidth = sourceSize["w"]?.let { it + offsetX } ?: error("ParallaxImageBuilder - sourceSize w is null for sprite '${frameTag}'!")
                                    parallaxHeight = sourceSize["h"]?.let { it + offsetY } ?: error("ParallaxImageBuilder - sourceSize h is null for sprite '${frameTag}'!")
                                    parallaxLayerInfo["w"] = parallaxWidth
                                    parallaxLayerInfo["h"] = parallaxHeight

                                    // Set frame info for animIndex: [textureIndex, x, y, width, height]
                                    layerInfo["f"] = arrayOf(
                                        idx,  // Save index to texture atlas where the frame is located
                                        frame["x"] ?: error("ParallaxImageBuilder - frame x is null for sprite '${frameTag}' index '$idx'!"),
                                        frame["y"] ?: error("ParallaxImageBuilder - frame y is null for sprite '${frameTag}' index '$idx'!"),
                                        frame["w"] ?: error("ParallaxImageBuilder - frame w is null for sprite '${frameTag}' index '$idx'!"),
                                        frame["h"] ?: error("ParallaxImageBuilder - frame h is null for sprite '${frameTag}' index '$idx'!")
                                    )

                                    val parallaxSize: Int
                                    val offset: Int
                                    val layerTextureSize: Int
                                    when (parallaxMode) {
                                        Mode.HORIZONTAL_PLANE -> {
                                            parallaxSize = parallaxHeight
                                            offset = spriteSource["y"]?.let { it + offsetY } ?: error("ParallaxImageBuilder - spriteSource y is null for sprite '${frameTag}'!")
                                            layerTextureSize = frame["h"] ?: error("ParallaxImageBuilder - frame h is null for sprite '${frameTag}' index '$idx'!")
                                        }
                                        Mode.VERTICAL_PLANE -> {
                                            parallaxSize = parallaxWidth
                                            offset =  spriteSource["x"]?.let { it + offsetX } ?: error("ParallaxImageBuilder - spriteSource x is null for sprite '${frameTag}'!")
                                            layerTextureSize = frame["w"] ?: error("ParallaxImageBuilder - frame w is null for sprite '${frameTag}' index '$idx'!")
                                        }
                                        else -> error("ParallaxImageBuilder - Parallax mode must be set to HORIZONTAL_PLANE or VERTICAL_PLANE in parallax plane '$planeName'!")
                                    }
                                    val index = if (attachBottomRight) offset else offset
                                    val indexForSpeed = if (attachBottomRight) offset else offset + layerTextureSize
                                    val speedFactor = layerInfo["s"]?.let { it as Float } ?: error("ParallaxImageBuilder - attached layer speedFactor is null for parallax layer '$layerName'!")

                                    // Set index and calculated speed factor for parallax plane attached layer
                                    layerInfo["i"] = index  // Index within the parallax plane
                                    layerInfo["s"] = getParallaxPlaneSpeedFactor(indexForSpeed, parallaxSize, speedFactor)
                                }
                            }

                            // Check if this frame belongs to parallax plane attached layers (if attached layers exist)
                            val topAttachedLayersInfo = parallaxPlaneInfo["t"]?.let { it as ArrayList<LinkedHashMap<String, Any>> } ?: emptyList()
                            topAttachedLayersInfo.forEach { layerInfo -> saveLayerInfo(layerInfo) }
                            val bottomAttachedLayersInfo = parallaxPlaneInfo["b"]?.let { it as ArrayList<LinkedHashMap<String, Any>> } ?: emptyList()
                            bottomAttachedLayersInfo.forEach { layerInfo -> saveLayerInfo(layerInfo, attachBottomRight = true) }

                            // Sanity check - Parallax size must be set
                            if (parallaxMode == Mode.HORIZONTAL_PLANE && parallaxHeight == 0) error("ParallaxImageBuilder - Parallax height must be defined for parallax plane '$planeName' in HORIZONTAL_PLANE mode!")
                            if (parallaxMode == Mode.VERTICAL_PLANE && parallaxWidth == 0) error("ParallaxImageBuilder - Parallax width must be defined for parallax plane '$planeName' in VERTICAL_PLANE mode!")

                            // Check if this frame belongs to parallax plane lines
                            val planeLinesInfo = parallaxPlaneInfo["l"]?.let { it as ArrayList<LinkedHashMap<String, Any>> } ?: emptyList()
                            planeLinesInfo.forEach { lineInfo ->
                                val sliceName = lineInfo["name"] as String?
                                // Set frame info for parallax plane line
                                if (sliceName != null && frameTag.contains(sliceName)) {
                                    val frame = frameEntry["frame"] as Map<String, Int>
                                    // Set frame info: [textureIndex, x, y, width, height]
                                    lineInfo["f"] = arrayOf(
                                        idx,  // Save index to texture atlas where the frame is located
                                        frame["x"] ?: error("ParallaxImageBuilder - frame x is null for plane line '${frameTag}'!"),
                                        frame["y"] ?: error("ParallaxImageBuilder - frame y is null for plane line '${frameTag}'!"),
                                        frame["w"] ?: error("ParallaxImageBuilder - frame w is null for plane line '${frameTag}'!"),
                                        frame["h"] ?: error("ParallaxImageBuilder - frame h is null for plane line '${frameTag}'!")
                                    )

                                    // Calculate and set speed factor for parallax plane line
                                    val index = lineInfo["i"]?.let { it as Int } ?: error("ParallaxImageBuilder - line index is null for parallax plane line '$sliceName'!")
                                    val speedFactor = lineInfo["s"]?.let { it as Float } ?: error("ParallaxImageBuilder - line speedFactor is null for parallax plane line '$sliceName'!")
                                    val parallaxSize = when (parallaxMode) {
                                        Mode.HORIZONTAL_PLANE -> parallaxHeight
                                        Mode.VERTICAL_PLANE -> parallaxWidth
                                        else -> error("ParallaxImageBuilder - Parallax mode must be set to HORIZONTAL_PLANE or VERTICAL_PLANE in parallax plane '$planeName'!")
                                    }
                                    lineInfo["s"] = getParallaxPlaneSpeedFactor(index, parallaxSize, speedFactor)

                                    // Cleanup obsolete slice name
                                    lineInfo.remove("name")
                                }
                            }
                        }
                    }
                }
            }

            // Cleanup obsolete data from parallax layer info
            parallaxLayersInfo.forEach { (_, parallaxLayerInfo) ->
                parallaxLayerInfo as LinkedHashMap<String, Any>
                parallaxLayerInfo.remove("offsetX")
                parallaxLayerInfo.remove("offsetY")
            }
        }
    }

    private fun getParallaxPlaneSpeedFactor(index: Int, size: Int, speedFactor: Float) : Float {
        val midPoint: Float = size * 0.5f
        return speedFactor * (
            // The pixel in the point of view must not stand still, they need to move with the lowest possible speed (= 1 / midpoint)
            // Otherwise the midpoint is "running" away over time
            if (index < midPoint)
                1f - (index / midPoint)
            else
                (index - midPoint + 1f) / midPoint
            )
    }

    private fun saveImageInfo(imageInfo: LinkedHashMap<String, Any>, frameEntry: Map<String, Int>, frameTag: String, idx: Int, animIndex: Int, saveSize: Boolean = true) {
        // Ensure the frames list is large enough and set the frame at the correct index
        val framesInfo = imageInfo["f"] as MutableList<LinkedHashMap<String, Any>>
        if (animIndex >= framesInfo.size) error("ImageBuilder - Animation index ${animIndex} out of bounds for sprite '${frameTag}' with ${framesInfo.size} frames!")

        val spriteSource = frameEntry["spriteSourceSize"] as Map<String, Int>
        val sourceSize = frameEntry["sourceSize"] as Map<String, Int>
        val frame = frameEntry["frame"] as Map<String, Int>

        // Set frame info for animIndex: [textureIndex, x, y, width, height]
        framesInfo[animIndex]["f"] = arrayOf(
            idx,
            frame["x"] ?: error("ImageBuilder - frame x is null for sprite '${frameTag}' index '$idx'!"),
            frame["y"] ?: error("ImageBuilder - frame y is null for sprite '${frameTag}' index '$idx'!"),
            frame["w"] ?: error("ImageBuilder - frame w is null for sprite '${frameTag}' index '$idx'!"),
            frame["h"] ?: error("ImageBuilder - frame h is null for sprite '${frameTag}' index '$idx'!")
        )
        framesInfo[animIndex]["x"] = spriteSource["x"] ?: error("ImageBuilder - spriteSource x is null for sprite '${frameTag}'!")
        framesInfo[animIndex]["y"] = spriteSource["y"] ?: error("ImageBuilder - spriteSource y is null for sprite '${frameTag}'!")
        // Do not set duration here - it was already set by AssetImageLoader from ASEInfo during texture export from Aseprite

        if (saveSize) {
            imageInfo["w"] = sourceSize["w"] ?: error("ImageBuilder - sourceSize w is null for sprite '${frameTag}'!")
            imageInfo["h"] = sourceSize["h"] ?: error("ImageBuilder - sourceSize h is null for sprite '${frameTag}'!")
        }
    }
}
