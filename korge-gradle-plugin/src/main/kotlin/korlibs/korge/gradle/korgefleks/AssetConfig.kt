package korlibs.korge.gradle.korgefleks

import com.android.build.gradle.internal.cxx.json.jsonStringOf
import korlibs.korge.gradle.texpacker.NewTexturePacker
import korlibs.korge.gradle.korgefleks.AssetInfo.*
import java.io.File


/**
 * Configuration for managing assets in a KorgeFleks project.
 *
 * This class is a convenience wrapper around AssetImageLoader.
 *
 * @param asepriteExe The path to the Aseprite executable.
 * @param projectDir The root directory of the project.
 * @param assetPath The relative path to the directory containing asset files.
 * @param resourcePath The relative path to the directory for game resources.
 */
class AssetsConfig(
    private val asepriteExe: String,
    projectDir: File,
    assetPath: String,
    resourcePath: String,
    private val simplifyJson: Boolean
) {
    // Directory where Aseprite files are located
    private val assetDir = projectDir.resolve(assetPath)
    // Directory where exported tiles and tilesets will be stored
    private val exportTilesDir = projectDir.resolve("build/assets/${assetPath.replace("/", "-").replace("\\", "-")}-tiles")
    private val exportTilesetDir = projectDir.resolve("build/assets/${assetPath.replace("/", "-").replace("\\", "-")}-tilesets2")
    // Directory where game resources are located
    private val gameResourcesDir = projectDir.resolve("src/commonMain/resources/${resourcePath}")

    init {
        // Make sure the export directories exist and that they are empty
        if (exportTilesDir.exists()) exportTilesDir.deleteRecursively()
        if (exportTilesetDir.exists()) exportTilesetDir.deleteRecursively()
        exportTilesDir.mkdirs()
        exportTilesetDir.mkdirs()
    }

    private val assetInfoList = linkedMapOf<String, Any>()

    private val assetImageLoader = AssetImageLoader(
        asepriteExe,
        assetDir,
        exportTilesDir,
        assetInfoList
    )

    /**
     * Export specific layers and tags from Aseprite file as independent png images.
     * Adds exported images to internal asset info list.
     */
    fun addImageAse(filename: String, layers: List<String>, tags: List<String>, output: String) {
        assetImageLoader.addImageAse(filename, layers, tags, output)
    }

    /** Export full image from Aseprite file
     */
    fun addImageAse(filename: String, output: String) {
        assetImageLoader.addImageAse(filename, emptyList(), emptyList(), output )
    }

    /** Export specific layer from Aseprite file
     */
    fun addImageAse(filename: String, layer: String, output: String) {
        assetImageLoader.addImageAse(filename, listOf(layer), emptyList(), output)
    }

    /** Export specific layer and tag from Aseprite file
     */
    fun addImageAse(filename: String, layer: String, tag: String, output: String) {
        assetImageLoader.addImageAse(filename, listOf(layer), listOf(tag), output)
    }

    /** Export specific layer and tags from Aseprite file
     */
    fun addImageAse(filename: String, layer: String, tags: List<String>, output: String) {
        assetImageLoader.addImageAse(filename, listOf(layer), tags, output)
    }

    /** Export specific layers from Aseprite file
     */
    fun addImageAse(filename: String, layers: List<String>, output: String) {
        assetImageLoader.addImageAse(filename, layers, emptyList(), output)
    }

    /** Export specific layers and tag from Aseprite file
     */
    fun addImageAse(filename: String, layers: List<String>, tag: String, output: String) {
        assetImageLoader.addImageAse(filename, layers, listOf(tag), output)
    }

    fun addNinePatchImageAse(filename: String, output: String) {
    }

    internal fun buildAtlases(
        textureAtlasName: String,
        tilesetAtlasName: String,
        textureAtlasWidth: Int,
        textureAtlasHeight: Int
    ) {
        val assetInfoVersion = 1
        val assetInfoBuild = 1
        assetInfoList["info"] = arrayOf(assetInfoVersion, assetInfoBuild)

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

            val textures = arrayListOf<String>()
            assetInfoList["textures"] = textures

            // Go through each generated atlas entry and map frames to asset info list
            val imagesInfo = assetInfoList["images"] as LinkedHashMap<String, Any>
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
                    val match = regex.find(frameName)
                    val animIndex = match?.groupValues?.get(1)?.toInt() ?: 0

                    imagesInfo[frameTag]?.let { imageInfo ->
                        imageInfo as LinkedHashMap<String, Any>
                        // Ensure the frames list is large enough and set the frame at the correct index
                        val framesInfo = imageInfo["fs"] as MutableList<LinkedHashMap<String, Any>>
                        if (animIndex >= framesInfo.size) error("AssetConfig - Animation index ${animIndex} out of bounds for sprite '${frameTag}' with ${framesInfo.size} frames!")

                        val frame = frameEntry["frame"] as Map<String, Int>
                        val spriteSource = frameEntry["spriteSourceSize"] as Map<String, Int>
                        val sourceSize = frameEntry["sourceSize"] as Map<String, Int>

                        framesInfo[animIndex]["f"] = arrayOf(
                            idx,
                            frame["x"] ?: error("AssetConfig - frame x is null for sprite '${frameName}'!"),
                            frame["y"] ?: error("AssetConfig - frame y is null for sprite '${frameName}'!"),
                            frame["w"] ?: error("AssetConfig - frame w is null for sprite '${frameName}'!"),
                            frame["h"] ?: error("AssetConfig - frame h is null for sprite '${frameName}'!")
                        )
                        framesInfo[animIndex]["x"] = spriteSource["x"] ?: error("AssetConfig - spriteSource x is null for sprite '${frameName}'!")
                        framesInfo[animIndex]["y"] = spriteSource["y"] ?: error("AssetConfig - spriteSource y is null for sprite '${frameName}'!")
                        // Do not set duration here - it was set already from ASEInfo during texture export from Aseprite

                        imageInfo["w"] = sourceSize["w"] ?: error("AssetConfig - sourceSize w is null for sprite '${frameName}'!")
                        imageInfo["h"] = sourceSize["h"] ?: error("AssetConfig - sourceSize h is null for sprite '${frameName}'!")
                    }
                }
            }

            // Finally, write out the asset info as JSON file
            val assetInfoJsonFile = gameResourcesDir.resolve("${textureAtlasName}.atlas.json")
            assetInfoJsonFile.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) error("Failed to create directory: ${parent.path}")
                val jsonString = jsonStringOf(assetInfoList)
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
}
