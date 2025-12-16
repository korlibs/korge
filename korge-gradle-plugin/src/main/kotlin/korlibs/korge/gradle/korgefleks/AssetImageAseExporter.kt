package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.util.ASEInfo
import korlibs.korge.gradle.util.LocalSFile
import korlibs.korge.gradle.util.executeSystemCommand
import org.gradle.api.GradleException
import java.io.File
import kotlin.collections.set

/**
 * Asset image loader which can export images from Aseprite files and
 * add them to the internal asset info structure.
 *
 * @param asepriteExe Path to the Aseprite executable.
 * @param assetDir Directory where the Aseprite source files are located relative to the root of the project.
 * @param exportTilesDir Directory where the exported images should be saved. Normally inside the build resources directory.
 * @param assetInfo The internal asset info structure where exported images will be added.
 */
class AssetImageAseExporter(
    private val asepriteExe: String,
    private val assetDir: File,
    private val exportTilesDir: File,
    private val assetInfo: LinkedHashMap<String, Any>
) {
    // Functions which needs to be overridden for testing
    internal var runAsepriteExport: (command: Array<String>) -> Unit = {
        //println("Executing command: ${cmd.joinToString(" ")}")
        executeSystemCommand(it)
    }
    internal var getAseFile: (filename: String) -> File = {
        // Get the Aseprite File object from the asset path
        val aseFile = assetDir.resolve(it)
        if (!aseFile.exists()) throw GradleException("Aseprite file '${aseFile}' not found!")
        aseFile
    }
    internal var loadAseInfo: (file: File) -> ASEInfo = {
        ASEInfo.getAseInfo(LocalSFile(it))
    }

    /**
     * Export specific layers and tags from Aseprite file as independent png images.
     * Adds exported images to internal asset info list.
     */
    fun addImageAse(filename: String, layers: List<String>, tags: List<String>, output: String) {
        if (output.isBlank()) throw GradleException("No output file specified for Aseprite export of image '${filename}'!")
        println("Export image file: '${filename}', layers: '${layers}', tags: '${tags}', output: '${output}'")

        exportImageFromAseprite(filename, layers, tags, output, /* OPTIONAL: prefix = "img_" */) { aseInfo, imageName, tag ->
            // Create image animation and store frame duration
            val animLength: Int
            val animStart: Int
            if (tag.isNotEmpty()) {
                animLength = aseInfo.tagsByName[tag]!!.toFrame - aseInfo.tagsByName[tag]!!.fromFrame + 1
                animStart = aseInfo.tagsByName[tag]!!.fromFrame

            } else {
                animLength = aseInfo.frames.size
                animStart = 0
            }

            // Create frames list and set frame durations based on the Aseprite info
            val frames = MutableList(animLength) { LinkedHashMap<String, Any>() }
            for (animIndex in animStart until animStart + animLength) {
                val frameDuration = aseInfo.frames[animIndex].duration
                frames[animIndex - animStart]["d"] = frameDuration
            }
            // Get image map from asset info and store frames list
            val images = assetInfo["images"] as LinkedHashMap<String, Any>
            val image = linkedMapOf<String, Any>()
            image["fs"] = frames
            images[imageName] = image
        }
    }

    /**
     * Export specific layers and tags from Aseprite file as independent png nine-patch images.
     * Adds exported nine-patch images to internal asset info list.
     */
    fun addNinePatchImageAse(filename: String, layers: List<String>, tags: List<String>, output: String) {
        if (output.isBlank()) throw GradleException("No output file specified for Aseprite export of nine-patch '${filename}'!")
        println("Export nine-patch image file: '${filename}', layers: '${layers}', tags: '${tags}', output: '${output}'")

        exportImageFromAseprite(filename, layers, tags, output, /* OPTIONAL: prefix = "npt_" */) { aseInfo, imageName, _ ->
            // Currently we do not use slice names for nine-patch images - implement later if we need it
            val slicename = ""

            // Create and store nine-patch info
            val ninepaches = assetInfo["ninePatches"] as LinkedHashMap<String, Any>
            val ninepatch = linkedMapOf<String, Any>()
            // For nine-patches we only store the frame info of the first frame ???
            if (slicename.isEmpty()) {
                val ninePatchslice = aseInfo.slices.first()
                if (ninePatchslice.hasNinePatch && ninePatchslice.keys.isNotEmpty() && ninePatchslice.keys.first().ninePatch != null) {
                    val ninePatchInfo = ninePatchslice.keys.first().ninePatch
                    ninepatch["x"] = ninePatchInfo!!.centerX
                    ninepatch["y"] = ninePatchInfo.centerY
                    ninepatch["w"] = ninePatchInfo.centerWidth
                    ninepatch["h"] = ninePatchInfo.centerHeight
                } else error("No nine-patch slice found in Aseprite file '${filename}' for nine-patch image '${imageName}'!")
                ninepaches[imageName] = ninepatch
            }
        }
    }

    private fun exportImageFromAseprite(
        filename: String,
        layers: List<String>,
        tags: List<String>,
        output: String,
        prefix: String = "",
        configure: (aseInfo: ASEInfo, imageName: String, tag: String) -> Unit
    ) {
        val aseFile = getAseFile(filename)
        val aseInfo = loadAseInfo(aseFile)
        checkLayersTagsAvailable(aseInfo, filename, layers, tags)

        if (layers.isNotEmpty()) {
            val useLayerName = layers.size != 1  // Use layer name in output if multiple layers are specified

            for (layer in layers) {
                if (tags.isNotEmpty()) {
                    val useTagName = tags.size != 1  // Use tag name in output if multiple tags are specified

                    for (tag in tags) {
                        val imageName = if (useLayerName && useTagName) "${prefix}${output}_${layer}_${tag}"
                        else if (useLayerName) "${prefix}${output}_${layer}"
                        else if (useTagName) "${prefix}${output}_${tag}"
                        else "${prefix}${output}"
                        val cmd = arrayOf(asepriteExe, "-b",
                            "--layer", layer,
                            "--tag", tag,
                            aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${imageName}_{frame0}.png").absolutePath)
                        runAsepriteExport(cmd)
                        configure(aseInfo, imageName, tag)
                    }
                } else {
                    val imageName = if (useLayerName) "${prefix}${output}_${layer}" else "${prefix}${output}"
                    val cmd = arrayOf(asepriteExe, "-b",
                        "--layer", layer,
                        aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${imageName}_{frame0}.png").absolutePath)
                    runAsepriteExport(cmd)
                    configure(aseInfo, imageName, "")
                }

            }
        } else {
            if (tags.isNotEmpty()) {
                val useTagName = tags.size != 1  // Only use tag name in output if multiple tags are specified

                for (tag in tags) {
                    val imageName = if (useTagName) "${prefix}${output}_${tag}" else "${prefix}${output}"
                    val cmd = arrayOf(asepriteExe, "-b",
                        "--tag", tag,
                        aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${imageName}_{frame0}.png").absolutePath)
                    runAsepriteExport(cmd)
                    configure(aseInfo, imageName, tag)
                }
            } else {
                val imageName = "${prefix}${output}"
                val cmd = arrayOf(asepriteExe, "-b",
                    aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${imageName}_{frame0}.png").absolutePath)
                runAsepriteExport(cmd)
                configure(aseInfo, imageName, "")
            }
        }

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
}
