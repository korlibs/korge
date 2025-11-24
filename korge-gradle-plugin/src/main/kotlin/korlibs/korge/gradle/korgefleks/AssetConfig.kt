package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.util.ASEInfo
import korlibs.korge.gradle.util.LocalSFile
import org.gradle.api.GradleException
import java.io.File

class AssetsConfig(
    private val asepriteExe: String,
    private val projectDir: File,
    private val assetPath: String
) {
    // Directory where exported tiles and tilesets will be stored
    private val exportTilesDir = projectDir.resolve("build/assets/${assetPath.replace("/", "-").replace("\\", "-")}-tiles")
    private val exportTilesetDir = projectDir.resolve("build/assets/${assetPath.replace("/", "-").replace("\\", "-")}-tilesets")
    // Directory where game resources are located
    private val gameResourcesDir = projectDir.resolve("src/commonMain/resources")

    private val imagePrefix = "img_"

    init {
        // Make sure the export directories exist and that they are empty
        if (exportTilesDir.exists()) exportTilesDir.deleteRecursively()
        if (exportTilesetDir.exists()) exportTilesetDir.deleteRecursively()
        exportTilesDir.mkdirs()
        exportTilesetDir.mkdirs()
    }

    /** Get the Aseprite File object from the asset path
     */
    private fun getAseFile(filename: String): File {
        val aseFile = projectDir.resolve("${assetPath}/${filename}")
        if (!aseFile.exists()) throw GradleException("Aseprite file '${aseFile}' not found!")
        return aseFile
    }

    /** Check that the specified layers and tags exist in the Aseprite file
     */
    private fun checkLayersTagsAvailable(aseFile: File, layers: List<String>, tags: List<String>) {
        val aseInfo = ASEInfo.getAseInfo(LocalSFile(aseFile))
        val availableLayers = aseInfo.layers.map { it.layerName }
        val availableTags = aseInfo.tags.map { it.tagName }
        for (layer in layers) {
            if (layer !in availableLayers) {
                throw GradleException("Layer '${layer}' not found in Aseprite file '${aseFile.name}'! " +
                    "Available layers: ${availableLayers.joinToString(", ")}")
            }
        }
        for (tag in tags) {
            if (tag !in availableTags) {
                throw GradleException("Tag '${tag}' not found in Aseprite file '${aseFile.name}'! " +
                    "Available tags: ${availableTags.joinToString(", ")}")
            }
        }
    }

    /** Export specific layers and tags from Aseprite file
     */
    fun addImageAse(filename: String, layers: List<String>, tags: List<String>, output: String) {
        if (output.isBlank()) throw GradleException("No output file specified for Aseprite export of '${filename}'!")

        val aseFile = getAseFile(filename)
        checkLayersTagsAvailable(aseFile, layers, tags)

        println("Export image file: '${filename}', layers: '${layers}', tags: '${tags}', output: '${output}'")

        if (layers.isNotEmpty()) {
            val useLayerName = layers.size != 1  // Only use layer name in output if multiple layers are specified

            for (layer in layers) {
                if (tags.isNotEmpty()) {
                    val useTagName = tags.size != 1  // Only use tag name in output if multiple tags are specified

                    for (tag in tags) {
                        val cmd = mutableListOf(asepriteExe, "-b")
                        cmd.add("--layer")
                        cmd.add(layer)
                        cmd.add("--tag")
                        cmd.add(tag)
                        cmd.add(aseFile.absolutePath)
                        cmd.add("--save-as")
                        cmd.add(exportTilesDir.resolve(
                            if (useLayerName && useTagName) "${imagePrefix}${output}_${layer}_${tag}_{{frame0}}.png"
                            else if (useLayerName) "${imagePrefix}${output}_${layer}_{{frame0}}.png"
                            else if (useTagName) "${imagePrefix}${output}_${tag}_{{frame0}}.png"
                            else "${imagePrefix}${output}_{{frame0}}.png").absolutePath)

                        println("Executing command: ${cmd.joinToString(" ")}")
//                        executeSystemCommand(cmd)

                    }
                } else {
                    val cmd = mutableListOf(asepriteExe, "-b")
                    cmd.add("--layer")
                    cmd.add(layer)
                    cmd.add(aseFile.absolutePath)
                    cmd.add("--save-as")
                    cmd.add(exportTilesDir.resolve(
                        if (useLayerName) "${imagePrefix}${output}_${layer}_{{frame0}}.png"
                        else "${imagePrefix}${output}_{{frame0}}.png").absolutePath)

                    println("Executing command: ${cmd.joinToString(" ")}")
//                    executeSystemCommand(cmd)
                }
            }
        } else {
            if (tags.isNotEmpty()) {
                val useTagName = tags.size != 1  // Only use tag name in output if multiple tags are specified

                for (tag in tags) {
                    val cmd = mutableListOf(asepriteExe, "-b")
                    cmd.add("--tag")
                    cmd.add(tag)
                    cmd.add(aseFile.absolutePath)
                    cmd.add("--save-as")
                    cmd.add(exportTilesDir.resolve(
                        if (useTagName) "${imagePrefix}${output}_${tag}_{{frame0}}.png"
                        else "${imagePrefix}${output}_{{frame0}}.png").absolutePath)

                    println("Executing command: ${cmd.joinToString(" ")}")
//                    executeSystemCommand(cmd)
                }
            } else {
                val cmd = mutableListOf(asepriteExe, "-b")
                cmd.add(aseFile.absolutePath)
                cmd.add("--save-as")
                cmd.add(exportTilesDir.resolve("${imagePrefix}${output}_{{frame0}}.png").absolutePath)

                println("Executing command: ${cmd.joinToString(" ")}")
//                executeSystemCommand(cmd)
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
}
