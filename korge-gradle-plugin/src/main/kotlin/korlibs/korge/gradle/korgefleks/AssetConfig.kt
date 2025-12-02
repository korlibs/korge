package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.texpacker.NewTexturePacker
import korlibs.korge.gradle.util.ASEInfo
import korlibs.korge.gradle.util.LocalSFile
import korlibs.korge.gradle.util.executeSystemCommand
import org.gradle.api.GradleException
import java.awt.Rectangle
import java.io.File


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

    private val imagePrefix = "img_"
    private val ninePatchPrefix = "npt_"

    init {
        // Make sure the export directories exist and that they are empty
//        if (exportTilesDir.exists()) exportTilesDir.deleteRecursively()
//        if (exportTilesetDir.exists()) exportTilesetDir.deleteRecursively()
//        exportTilesDir.mkdirs()
//        exportTilesetDir.mkdirs()
    }

    data class AssetInfo(
        val images: MutableMap<String, MutableList<AssetInfoImageFrames>> = mutableMapOf(),
        val ninePatches: MutableMap<String, MutableList<AssetInfoNinePatch>> = mutableMapOf(),
        val pixelFonts: MutableMap<String, MutableList<AssetInfoPixelFonts>> = mutableMapOf()
    ) {
        data class AssetInfoImageFrames(
            val frames: MutableList<ImageFrame> = mutableListOf(),
            val width: Int,  // virtual size of the sprite - can be different from frame.width
            val height: Int  // and frame.height if cropped)
        ) {
            data class ImageFrame(
                val frame: Rectangle,
                val targetX: Int = 0,  // offset from the top-left corner of the original sprite if cropped
                val targetY: Int = 0,
                // Duration in seconds will be set later after all frames have been loaded from texture atlas
                var duration: Float = 0f
            )
        }

        data class AssetInfoNinePatch(
            val frame: Rectangle  // not cropped
        )

        data class AssetInfoPixelFonts(
            val frame: Rectangle  // not cropped
        )
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
                        assetInfoList.images[imageName] = mutableListOf()
                    }
                } else {
                    val imageName = if (useLayerName) "${imagePrefix}${output}_${layer}" else "${imagePrefix}${output}"
                    val cmd = arrayOf(asepriteExe, "-b",
                        "--layer", layer,
                        aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${imageName}_{frame0}.png").absolutePath)

                    //println("Executing command: ${cmd.joinToString(" ")}")
                    executeSystemCommand(cmd)
                    assetInfoList.images[imageName] = mutableListOf()
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
                    assetInfoList.images[imageName] = mutableListOf()
                }
            } else {
                val imageName = "${imagePrefix}${output}"
                val cmd = arrayOf(asepriteExe, "-b",
                    aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${imageName}_{frame0}.png").absolutePath)

                //println("Executing command: ${cmd.joinToString(" ")}")
                executeSystemCommand(cmd)
                assetInfoList.images[imageName] = mutableListOf()
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

    internal fun buildAtlases(spriteAtlasName: String, tilesetAtlasName: String) {
        // First build tiles atlas
        if (exportTilesDir.listFiles() != null && exportTilesDir.listFiles().isNotEmpty()) {
            val atlasInfoList = NewTexturePacker.packImages(exportTilesDir,
                enableRotation = false,
                enableTrimming = true,
                padding = 1,
                trimFileName = true
            )
            atlasInfoList.forEachIndexed { idx, atlasInfo ->
                val atlasOutputFile = gameResourcesDir.resolve("${spriteAtlasName}_${idx}.atlas")
                atlasInfo.writeImage(atlasOutputFile)

//                println("Sprite atlas $idx: $atlasInfo")
            }
        }
        // Then build tilesets atlas
        if (exportTilesetDir.listFiles() != null && exportTilesetDir.listFiles().isNotEmpty()) {
            val atlasInfoList = NewTexturePacker.packTilesets(exportTilesetDir)
            atlasInfoList.forEachIndexed { idx, atlasInfo ->
                val atlasOutputFile = gameResourcesDir.resolve("${tilesetAtlasName}_${idx}.atlas.png")
                atlasInfo.writeImage(atlasOutputFile)
            }
        }
    }
}
