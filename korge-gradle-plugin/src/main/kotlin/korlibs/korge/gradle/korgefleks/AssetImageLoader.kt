package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.korgefleks.AssetInfo.ImageFrame
import korlibs.korge.gradle.korgefleks.AssetInfo.ImageFrames
import korlibs.korge.gradle.util.ASEInfo
import korlibs.korge.gradle.util.LocalSFile
import korlibs.korge.gradle.util.executeSystemCommand
import org.gradle.api.GradleException
import java.io.File
import kotlin.collections.set

class AssetImageLoader(
    private val asepriteExe: String,
    private val assetDir: File,
    private val exportTilesDir: File,
    private val exportTilesetDir: File,
    private val gameResourcesDir: File,
    private val assetInfoList: AssetInfo
) {
    // Enable prefixes for exported images if needed
    private val imagePrefix = ""  // "img_"
    private val ninePatchPrefix = ""  // "npt_"

    // Functions which needs to be overridden for testing
    internal var runAsepriteExport: (command: Array<String>) -> Unit = {
        //println("Executing command: ${cmd.joinToString(" ")}")
        executeSystemCommand(it)
    }
    internal var loadAseInfo: (file: File) -> ASEInfo = {
        ASEInfo.getAseInfo(LocalSFile(it))
    }

    /**
     * Export specific layers and tags from Aseprite file as independent png images.
     * Adds exported images to internal asset info list.
     */
    fun addImageAse(filename: String, layers: List<String>, tags: List<String>, output: String) {
        if (output.isBlank()) throw GradleException("No output file specified for Aseprite export of '${filename}'!")
        println("Export image file: '${filename}', layers: '${layers}', tags: '${tags}', output: '${output}'")

        val aseFile = getAseFile(filename)
        val aseInfo = loadAseInfo(aseFile)
        checkLayersTagsAvailable(aseInfo, filename, layers, tags)

        if (layers.isNotEmpty()) {
            val useLayerName = layers.size != 1  // Use layer name in output if multiple layers are specified

            for (layer in layers) {
                if (tags.isNotEmpty()) {
                    val useTagName = tags.size != 1  // Use tag name in output if multiple tags are specified

                    for (tag in tags) {
                        val imageName = if (useLayerName && useTagName) "${imagePrefix}${output}_${layer}_${tag}"
                        else if (useLayerName) "${imagePrefix}${output}_${layer}"
                        else if (useTagName) "${imagePrefix}${output}_${tag}"
                        else "${imagePrefix}${output}"
                        val cmd = arrayOf(asepriteExe, "-b",
                            "--layer", layer,
                            "--tag", tag,
                            aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${imageName}_{frame0}.png").absolutePath)
                        runAsepriteExport(cmd)
                        createImageFrames(aseInfo, imageName, tag)
                    }
                } else {
                    val imageName = if (useLayerName) "${imagePrefix}${output}_${layer}" else "${imagePrefix}${output}"
                    val cmd = arrayOf(asepriteExe, "-b",
                        "--layer", layer,
                        aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${imageName}_{frame0}.png").absolutePath)
                    runAsepriteExport(cmd)
                    createImageFrames(aseInfo, imageName)
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
                    runAsepriteExport(cmd)
                    createImageFrames(aseInfo, imageName, tag)
                }
            } else {
                val imageName = "${imagePrefix}${output}"
                val cmd = arrayOf(asepriteExe, "-b",
                    aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${imageName}_{frame0}.png").absolutePath)
                runAsepriteExport(cmd)
                createImageFrames(aseInfo, imageName)
            }
        }
    }

    fun addNinePatchImageAse(filename: String, output: String) {
        if (output.isBlank()) throw GradleException("No output file specified for Aseprite export of '${filename}' nine-patch!")
        println("Export nine-patch image file: '${filename}', output: '${output}'")

        val aseFile = getAseFile(filename)
        val cmd = arrayOf(asepriteExe, "-b", aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${ninePatchPrefix}${output}.png").absolutePath)

        //println("Executing command: ${cmd.joinToString(" ")}")
        executeSystemCommand(cmd)
    }


    /** Get the Aseprite File object from the asset path
     */
    private fun getAseFile(filename: String): File {
        val aseFile = assetDir.resolve(filename)
//        if (!aseFile.exists()) throw GradleException("Aseprite file '${aseFile}' not found!")
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

    private fun createImageFrames(aseInfo: ASEInfo, imageName: String, tag: String = "") {
        val animLength: Int
        val animStart: Int
        if (tag.isNotEmpty()) {
            animLength = aseInfo.tagsByName[tag]!!.toFrame - aseInfo.tagsByName[tag]!!.fromFrame + 1
            animStart = aseInfo.tagsByName[tag]!!.fromFrame

        } else {
            animLength = aseInfo.frames.size
            animStart = 0
        }
        // Create empty frames list
        assetInfoList.images[imageName] = ImageFrames(frames = MutableList(animLength) { ImageFrame() } )

        // Now set the frame durations based on the Aseprite info
        for (animIndex in animStart until animStart + animLength) {
            val frameDuration = aseInfo.frames[animIndex].duration.toFloat()
            assetInfoList.images[imageName]?.frames?.get(animIndex - animStart)?.duration = frameDuration
        }
    }

}
