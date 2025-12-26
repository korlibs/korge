package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.IMAGES
import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.NINE_PATCHES
import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.PARALLAX_LAYERS
import korlibs.korge.gradle.korgefleks.ParallaxInfo.*
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
    internal var getFile: (filename: String) -> File = {
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

        exportImageFromAseprite(filename, layers, tags, output) { aseInfo, imageName, tag ->
            createImageFramesList(aseInfo, imageName, tag, IMAGES)
        }
    }

    /**
     * Export specific layers and tags from Aseprite file as independent png nine-patch images.
     * Adds exported nine-patch images to internal asset info list.
     */
    fun addNinePatchImageAse(filename: String, layers: List<String>, tags: List<String>, output: String) {
        if (output.isBlank()) throw GradleException("No output file specified for Aseprite export of nine-patch '${filename}'!")
        println("Export nine-patch image file: '${filename}', layers: '${layers}', tags: '${tags}', output: '${output}'")

        exportImageFromAseprite(filename, layers, tags, output) { aseInfo, imageName, _ ->
            // Currently we do not use slice names for nine-patch images - implement later if we need it
            val slicename = ""

            // Create and store nine-patch info
            val ninepaches = assetInfo[NINE_PATCHES] as LinkedHashMap<String, Any>
            val ninepatch = linkedMapOf<String, Any>()
            // For nine-patches we only store the frame info of the first frame
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

    fun addParallaxImageAse(filename: String, parallaxInfo: ParallaxInfo, tags: List<String> = listOf("export"), output: String = "parallax") {

        // Prepare parallax layer info storage
        val parallaxLayersJson = assetInfo[PARALLAX_LAYERS] as LinkedHashMap<String, Any>
        parallaxLayersJson[parallaxInfo.name] = linkedMapOf(
            "m" to parallaxInfo.mode,
            "offsetX" to parallaxInfo.offsetX,  // Save offsets for usage in AssetAtlasBuilder
            "offsetY" to parallaxInfo.offsetY,
            "b" to arrayListOf<LinkedHashMap<String, Any?>>(),  // background layers
            "f" to arrayListOf<LinkedHashMap<String, Any?>>()   // foreground layers
        )
        val parallaxLayerJson = parallaxLayersJson[parallaxInfo.name] as LinkedHashMap<String, Any>
        // Fast access to background and foreground layers
        val backgroundLayersJson = parallaxLayerJson["b"] as ArrayList<LinkedHashMap<String, Any?>>
        val foregroundLayersJson = parallaxLayerJson["f"] as ArrayList<LinkedHashMap<String, Any?>>

        // (1) Prepare list of parallax layers to export
        val layers = arrayListOf<String>()
        parallaxInfo.backgroundLayers.forEach { layer -> layers.add(layer.name) }
        parallaxInfo.foregroundLayers.forEach { layer -> layers.add(layer.name) }
        println("Export parallax layers from image file: '${filename}', layers: '${layers}', tags: '${tags}', output: '${output}'")

        // (2) Export all parallax layers as individual images and store layer info
        exportImageFromAseprite(filename, layers, tags, output) { _, imageName, _ ->

            fun setLayerInfo(layersJson: ArrayList<LinkedHashMap<String, Any?>>, layerInfo: ParallaxLayerInfo) {
                layersJson.add(linkedMapOf(
                    "n"  to imageName,
                    // "f" will be populated in AssetAtlasBuilder
                    // "tx" will be populated in AssetAtlasBuilder
                    // "ty" will be populated in AssetAtlasBuilder
                    "rx" to layerInfo.repeatX,
                    "ry" to layerInfo.repeatY,
                    "cx" to layerInfo.centerX,  // Center the layer in the parallax background image
                    "cy" to layerInfo.centerY,
                    "sf" to layerInfo.speedFactor,  // If this is null than no movement is applied to the layer
                    "sx" to layerInfo.selfSpeedX,
                    "sy" to layerInfo.selfSpeedY
                ))
            }

            // Get image map from asset info and store frames list
            val layerName = imageName.removePrefix("parallax_")  // Remove default prefix to get the original layer name

            // Store background and foreground layer info
            parallaxInfo.backgroundLayers.forEach { layerInfo -> if (layerInfo.name == layerName) setLayerInfo(backgroundLayersJson, layerInfo) }
            parallaxInfo.foregroundLayers.forEach { layerInfo -> if (layerInfo.name == layerName) setLayerInfo(foregroundLayersJson, layerInfo) }
        }

        parallaxInfo.parallaxPlane?.let { parallaxPlaneInfo ->
            // (3) Prepare parallax plane
            val parallaxPlaneJson = linkedMapOf<String, Any>(
                "n" to parallaxPlaneInfo.name,
                "s" to parallaxPlaneInfo.selfSpeed,
                "t" to arrayListOf<LinkedHashMap<String, Any>>(),  // top attached layers
                "b" to arrayListOf<LinkedHashMap<String, Any>>(),  // bottom attached layers
                "l" to arrayListOf<LinkedHashMap<String, Any>>()   // line textuers
            )
            parallaxLayerJson["p"] = parallaxPlaneJson

            // Fast access to parallax plane line textures and attached layers
            val planeLinesJson = parallaxPlaneJson["l"] as ArrayList<LinkedHashMap<String, Any>>
            val topAttachedLayersJson = parallaxPlaneJson["t"] as ArrayList<LinkedHashMap<String, Any>>
            val bottomAttachedLayersJson = parallaxPlaneJson["b"] as ArrayList<LinkedHashMap<String, Any>>

            // (4) Prepare list of attached parallax layers to export
            val layers = arrayListOf<String>()
            parallaxPlaneInfo.topAttachedLayers.forEach { layer -> layers.add(layer.name) }
            parallaxPlaneInfo.bottomAttachedLayers.forEach { layer -> layers.add(layer.name) }
            println("Export parallax attached layers from image file: '${filename}', layers: '${layers}', tags: '${tags}', output: '${output}'")

            // (5) Export all parallax attached layers as individual images and store layer info
            exportImageFromAseprite(filename, layers, tags, output) { _, imageName, _ ->

                fun setLayerInfo(layersJson: ArrayList<LinkedHashMap<String, Any>>) {
                    layersJson.add(linkedMapOf(
                        "n" to imageName,
                        // "f" frame info will be populated in AssetAtlasBuilder
                        // "i" index will be populated in AssetAtlasBuilder
                        "s" to parallaxPlaneInfo.speedFactor  // Store speed factor here - the correct speed per slice index will be calculated in AssetAtlasBuilder
                    ))
                }

                // Get parallax map from asset info and store layer list
                val layerName = imageName.removePrefix("parallax_")  // Remove default prefix to get the original layer name

                // Store background and foreground layer info
                parallaxPlaneInfo.topAttachedLayers.forEach { layerInfo -> if (layerInfo.name == layerName) setLayerInfo(topAttachedLayersJson) }
                parallaxPlaneInfo.bottomAttachedLayers.forEach { layerInfo -> if (layerInfo.name == layerName) setLayerInfo(bottomAttachedLayersJson) }
            }

            // (6) Prepare list of parallax plane slices to export
            val aseFile = getFile(filename)
            val aseInfo = loadAseInfo(aseFile)
            aseInfo.slices.forEach { slice ->
                slice.sliceName
                val index = when (parallaxInfo.mode) {
                    Mode.HORIZONTAL_PLANE -> slice.keys.first().y + parallaxInfo.offsetY
                    Mode.VERTICAL_PLANE -> slice.keys.first().x + parallaxInfo.offsetX
                    else -> throw GradleException("Parallax mode must be HORIZONTAL_PLANE or VERTICAL_PLANE to export parallax plane slices!")
                }

                planeLinesJson.add(linkedMapOf(
                    "name" to slice.sliceName,  // Name is needed here to identify the slice in the atlas in AssetAtlasBuilder
                    // "f" will be populated in AssetAtlasBuilder
                    "i" to index,
                    "s" to parallaxPlaneInfo.speedFactor  // Store speed factor here - the correct speed per slice index will be calculated in AssetAtlasBuilder
                ))
            }

            // (7) Export ground plane as individual sliced images
            val planeName = parallaxPlaneInfo.name
            println("Export parallax plane from image file: '${filename}', layer: '${planeName}', tags: '${tags}', output: '${output}'")
            exportImageFromAseprite(filename, listOf(planeName), tags, "${output}_${planeName}", slice = true, frame = false) { _, _, _ ->
                // Nothing to do here - slices are stores already in step (6)
            }
        }
    }

    private fun createImageFramesList(aseInfo: ASEInfo, imageName: String, tag: String, assetSectionName: String) {
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
        val images = assetInfo[assetSectionName] as LinkedHashMap<String, Any>
        val image = linkedMapOf<String, Any>()
        image["f"] = frames
        images[imageName] = image
    }

    private fun exportImageFromAseprite(
        filename: String,
        layers: List<String>,
        tags: List<String>,
        output: String,
        prefix: String = "",
        slice: Boolean = false,
        frame: Boolean = true,
        configure: (aseInfo: ASEInfo, imageName: String, tag: String) -> Unit
    ) {
        val aseFile = getFile(filename)
        val aseInfo = loadAseInfo(aseFile)
        checkLayersTagsAvailable(aseInfo, filename, layers, tags)

        val slice = if (slice) "_{slice}" else ""
        val frame = if (frame) "_{frame0}" else ""

        if (layers.isNotEmpty()) {
            val useLayerName = layers.size != 1  // Use layer name in output if multiple layers are specified

            for (layer in layers) {
                if (tags.isNotEmpty()) {
                    val useTagName = tags.size != 1  // Use tag name in output if multiple tags are specified

                    for (tag in tags) {
                        val imageName = if (useLayerName && useTagName) "${prefix}${output}_${layer}_${tag}${slice}"
                        else if (useLayerName) "${prefix}${output}_${layer}${slice}"
                        else if (useTagName) "${prefix}${output}_${tag}${slice}"
                        else "${prefix}${output}${slice}"
                        val cmd = arrayOf(asepriteExe, "-b",
                            "--layer", layer,
                            "--tag", tag,
                            aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${imageName}${frame}.png").absolutePath)
                        runAsepriteExport(cmd)
                        configure(aseInfo, imageName, tag)
                    }
                } else {
                    val imageName = if (useLayerName) "${prefix}${output}_${layer}${slice}" else "${prefix}${output}${slice}"
                    val cmd = arrayOf(asepriteExe, "-b",
                        "--layer", layer,
                        aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${imageName}${frame}.png").absolutePath)
                    runAsepriteExport(cmd)
                    configure(aseInfo, imageName, "")
                }

            }
        } else {
            if (tags.isNotEmpty()) {
                val useTagName = tags.size != 1  // Only use tag name in output if multiple tags are specified

                for (tag in tags) {
                    val imageName = if (useTagName) "${prefix}${output}_${tag}${slice}" else "${prefix}${output}${slice}"
                    val cmd = arrayOf(asepriteExe, "-b",
                        "--tag", tag,
                        aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${imageName}${frame}.png").absolutePath)
                    runAsepriteExport(cmd)
                    configure(aseInfo, imageName, tag)
                }
            } else {
                val imageName = "${prefix}${output}${slice}"
                val cmd = arrayOf(asepriteExe, "-b",
                    aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${imageName}${frame}.png").absolutePath)
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
