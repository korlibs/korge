package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.texpacker.NewTexturePacker
import korlibs.korge.gradle.korgefleks.AssetInfo.*
import korlibs.korge.gradle.util.ASEInfo
import korlibs.korge.gradle.util.LocalSFile
import korlibs.korge.gradle.util.executeSystemCommand
import org.gradle.api.GradleException
import java.awt.Rectangle
import java.io.File
import kotlin.collections.set


/**
 * Configuration for managing assets in a KorgeFleks project.
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

    // Enable prefixes for exported images if needed
    private val imagePrefix = ""  // "img_"
    private val ninePatchPrefix = ""  // "npt_"

    init {
        // Make sure the export directories exist and that they are empty
        if (exportTilesDir.exists()) exportTilesDir.deleteRecursively()
        if (exportTilesetDir.exists()) exportTilesetDir.deleteRecursively()
        exportTilesDir.mkdirs()
        exportTilesetDir.mkdirs()
    }

    val assetInfoList = AssetInfo()

    /** Get the Aseprite File object from the asset path
     */
    private fun getAseFile(filename: String): File {
        val aseFile = assetDir.resolve(filename)
        if (!aseFile.exists()) throw GradleException("Aseprite file '${aseFile}' not found!")
        return aseFile
    }

    /**
     * Check that the specified layers and tags exist in the Aseprite file.
     *
     * @param aseInfo The ASEInfo object containing layers and tags information.
     * @param aseFileName The name of the Aseprite file being checked (Used for error output).
     * @param layers The list of layer names to check.
     * @param tags The list of tag names to check.
     * @throws GradleException if any specified layer or tag is not found.
     */
    private fun checkLayersTagsAvailable(aseInfo: ASEInfo, aseFileName: String, layers: List<String>, tags: List<String>) {
        val availableLayers = aseInfo.layers.map { it.layerName }
        val availableTags = aseInfo.tags.map { it.tagName }
        for (layer in layers) {
            if (layer !in availableLayers) {
                throw GradleException("Layer '${layer}' not found in Aseprite file '$aseFileName'! " +
                    "Available layers: ${availableLayers.joinToString(", ")}")
            }
        }
        for (tag in tags) {
            if (tag !in availableTags) {
                throw GradleException("Tag '${tag}' not found in Aseprite file '$aseFileName'! " +
                    "Available tags: ${availableTags.joinToString(", ")}")
            }
        }
    }

    /**
     * Export specific layers and tags from Aseprite file as independent png images.
     * Adds exported images to internal asset info list.
     */
    fun addImageAse(filename: String, layers: List<String>, tags: List<String>, output: String) {
        if (output.isBlank()) throw GradleException("No output file specified for Aseprite export of '${filename}'!")
        println("Export image file: '${filename}', layers: '${layers}', tags: '${tags}', output: '${output}'")

        val aseFile = getAseFile(filename)
        val aseInfo = ASEInfo.getAseInfo(LocalSFile(aseFile))
        checkLayersTagsAvailable(aseInfo, aseFile.name, layers, tags)

        println(aseInfo)
        val defaultAnimLength = aseInfo.frames.size

        if (layers.isNotEmpty()) {
            val useLayerName = layers.size != 1  // Only use layer name in output if multiple layers are specified

            for (layer in layers) {
                if (tags.isNotEmpty()) {
                    val useTagName = tags.size != 1  // Only use tag name in output if multiple tags are specified

                    for (tag in tags) {
                        val imageName = if (useLayerName && useTagName) "${imagePrefix}${output}_${layer}_${tag}"
                        else if (useLayerName) "${imagePrefix}${output}_${layer}"
                        else if (useTagName) "${imagePrefix}${output}_${tag}"
                        else "${imagePrefix}${output}"
                        val cmd = arrayOf(asepriteExe, "-b",
                            "--layer", layer,
                            "--tag", tag,
                            aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${imageName}_{frame0}.png").absolutePath)

                        //println("Executing command: ${cmd.joinToString(" ")}")
                        executeSystemCommand(cmd)

                        val animLength: Int = aseInfo.tagsByName[tag]!!.toFrame - aseInfo.tagsByName[tag]!!.fromFrame + 1
                        assetInfoList.images[imageName] = ImageFrames(frames = MutableList(animLength) { ImageFrame() } )
                    }
                } else {
                    val imageName = if (useLayerName) "${imagePrefix}${output}_${layer}" else "${imagePrefix}${output}"
                    val cmd = arrayOf(asepriteExe, "-b",
                        "--layer", layer,
                        aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${imageName}_{frame0}.png").absolutePath)

                    //println("Executing command: ${cmd.joinToString(" ")}")
                    executeSystemCommand(cmd)

                    assetInfoList.images[imageName] = ImageFrames(frames = MutableList(defaultAnimLength) { ImageFrame() } )
                }

            }
        } else {
            if (tags.isNotEmpty()) {
                val useTagName = tags.size != 1  // Only use tag name in output if multiple tags are specified

                for (tag in tags) {
                    val imageName = if (useTagName) "${imagePrefix}${output}_${tag}" else "${imagePrefix}${output}"
                    val cmd = arrayOf(asepriteExe, "-b",
                        "--tag", tag,
                        aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${imageName}_{frame0}.png").absolutePath)

                    //println("Executing command: ${cmd.joinToString(" ")}")
                    executeSystemCommand(cmd)

                    val animLength: Int = aseInfo.tagsByName[tag]!!.toFrame - aseInfo.tagsByName[tag]!!.fromFrame + 1
                    assetInfoList.images[imageName] = ImageFrames(frames = MutableList(animLength) { ImageFrame() } )
                }
            } else {
                val imageName = "${imagePrefix}${output}"
                val cmd = arrayOf(asepriteExe, "-b",
                    aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${imageName}_{frame0}.png").absolutePath)

                //println("Executing command: ${cmd.joinToString(" ")}")
                executeSystemCommand(cmd)

                assetInfoList.images[imageName] = ImageFrames(frames = MutableList(defaultAnimLength) { ImageFrame() } )
            }
        }
    }

    /** Export full image from Aseprite file
     */
    fun addImageAse(filename: String, output: String) {
        addImageAse(filename, emptyList(), emptyList(), output )
    }

    /** Export specific layer from Aseprite file
     */
    fun addImageAse(filename: String, layer: String, output: String) {
        addImageAse(filename, listOf(layer), emptyList(), output)
    }

    /** Export specific layer and tag from Aseprite file
     */
    fun addImageAse(filename: String, layer: String, tag: String, output: String) {
        addImageAse(filename, listOf(layer), listOf(tag), output)
    }

    /** Export specific layer and tags from Aseprite file
     */
    fun addImageAse(filename: String, layer: String, tags: List<String>, output: String) {
        addImageAse(filename, listOf(layer), tags, output)
    }

    /** Export specific layers from Aseprite file
     */
    fun addImageAse(filename: String, layers: List<String>, output: String) {
        addImageAse(filename, layers, emptyList(), output)
    }

    /** Export specific layers and tag from Aseprite file
     */
    fun addImageAse(filename: String, layers: List<String>, tag: String, output: String) {
        addImageAse(filename, layers, listOf(tag), output)
    }

    fun addNinePatchImageAse(filename: String, output: String) {
        if (output.isBlank()) throw GradleException("No output file specified for Aseprite export of '${filename}' nine-patch!")
        println("Export nine-patch image file: '${filename}', output: '${output}'")

        val aseFile = getAseFile(filename)
        val cmd = arrayOf(asepriteExe, "-b", aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${ninePatchPrefix}${output}.png").absolutePath)

        //println("Executing command: ${cmd.joinToString(" ")}")
        executeSystemCommand(cmd)
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
                            frame = Rectangle(
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
                    assetInfoYaml.append("      - frame: { x: ${frame.frame.x}, y: ${frame.frame.y}, w: ${frame.frame.width}, h: ${frame.frame.height} }\n")
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
