package korlibs.korge.gradle.korgefleks

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
    resourcePath: String
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

    private val assetInfoList = AssetInfo()

    private val assetImageLoader = AssetImageLoader(
        asepriteExe,
        assetDir,
        exportTilesDir,
        exportTilesetDir,
        gameResourcesDir,
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

    internal fun buildAtlases(textureAtlasName: String, tilesetAtlasName: String) {
        val assetInfoYaml = StringBuilder()
        val assetInfoVersion = 1
        val assetInfoBuild = 1
        assetInfoYaml.append("info: { v: ${assetInfoVersion}, b: $assetInfoBuild }\n")

        // First build tiles atlas
        if (exportTilesDir.listFiles() != null && exportTilesDir.listFiles().isNotEmpty()) {
            val atlasInfoList = NewTexturePacker.packImages(exportTilesDir,
                enableRotation = false,
                enableTrimming = true,
                padding = 1,
                trimFileName = true,
                removeDuplicates = true
            )
            assetInfoYaml.append("textures:\n")

            // Go through each generated atlas entry and map frames to asset info list
            atlasInfoList.forEachIndexed { idx, atlasInfo ->
                val atlasOutputFile = gameResourcesDir.resolve("${textureAtlasName}_${idx}.atlas.png")
                atlasInfo.writeImage(atlasOutputFile)
                assetInfoYaml.append("  - ${atlasOutputFile.name}\n")

                val frames = atlasInfo.info["frames"] as Map<String, Any>
                frames.forEach { (frameName, frameEntry) ->
                    frameEntry as Map<String, Any>
                    // Split frameName into frameTag and animation index number
                    val regex = "_(\\d+)$".toRegex()
                    val frameTag = frameName.replace(regex, "")
                    val match = regex.find(frameName)
                    val animIndex = match?.groupValues?.get(1)?.toInt() ?: 0

                    assetInfoList.images[frameTag]?.let { image ->
                        // Ensure the frames list is large enough and set the frame at the correct index
                        if (animIndex >= image.frames.size) error("AssetConfig - Animation index ${animIndex} out of bounds for sprite '${frameTag}' with ${image.frames.size} frames!")

                        val frame = frameEntry["frame"] as Map<String, Int>
                        val spriteSource = frameEntry["spriteSourceSize"] as Map<String, Int>
                        val sourceSize = frameEntry["sourceSize"] as Map<String, Int>
                        val duration = (frameEntry["duration"] as? Int)?.toFloat() ?: 0f
                        image.frames[animIndex] = ImageFrame(
                            frame = Frame(
                                idx,
                                frame["x"] ?: error("AssetConfig - frame x is null for sprite '${frameName}'!"),
                                frame["y"] ?: error("AssetConfig - frame y is null for sprite '${frameName}'!"),
                                frame["w"] ?: error("AssetConfig - frame w is null for sprite '${frameName}'!"),
                                frame["h"] ?: error("AssetConfig - frame h is null for sprite '${frameName}'!")
                            ),
                            targetX = spriteSource["x"] ?: error("AssetConfig - spriteSource x is null for sprite '${frameName}'!"),
                            targetY = spriteSource["y"] ?: error("AssetConfig - spriteSource y is null for sprite '${frameName}'!"),
                            duration = duration
                        )
                        image.width = sourceSize["w"] ?: error("AssetConfig - sourceSize w is null for sprite '${frameName}'!")
                        image.height = sourceSize["h"] ?: error("AssetConfig - sourceSize h is null for sprite '${frameName}'!")
                    }
                }
            }

            // Now append the asset info sections for images, nine-patches, pixel fonts ,etc.
            assetInfoYaml.append("images:\n")
            assetInfoList.images.forEach { (imageName, imageFrames) ->
                assetInfoYaml.append("  $imageName:\n")
                assetInfoYaml.append("    w: ${imageFrames.width}\n")
                assetInfoYaml.append("    h: ${imageFrames.height}\n")
                assetInfoYaml.append("    frames:\n")
                imageFrames.frames.forEach { frame ->
                    assetInfoYaml.append("      - frame: { i: ${frame.frame.index}, x: ${frame.frame.x}, y: ${frame.frame.y}, w: ${frame.frame.width}, h: ${frame.frame.height} }\n")
                    assetInfoYaml.append("        x: ${frame.targetX}\n")
                    assetInfoYaml.append("        y: ${frame.targetY}\n")
                    assetInfoYaml.append("        duration: ${frame.duration}\n")
                }
            }

            // Finally, write out the asset info yaml file
            val assetInfoYamlFile = gameResourcesDir.resolve("${textureAtlasName}.atlas.yml")
            assetInfoYamlFile.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) error("Failed to create directory: ${parent.path}")
                assetInfoYamlFile.writeText(assetInfoYaml.toString())
            }
        }

        // Then build tilesets atlas
        if (exportTilesetDir.listFiles() != null && exportTilesetDir.listFiles().isNotEmpty()) {
            val atlasInfoList = NewTexturePacker.packTilesets(exportTilesetDir)
            atlasInfoList.forEachIndexed { idx, atlasInfo ->
                val atlasOutputFile = gameResourcesDir.resolve("${tilesetAtlasName}_${idx}.atlas")
                atlasInfo.writeImage(atlasOutputFile)
            }

            assetInfoYaml.append("tilesets:\n")

        }
    }


}
