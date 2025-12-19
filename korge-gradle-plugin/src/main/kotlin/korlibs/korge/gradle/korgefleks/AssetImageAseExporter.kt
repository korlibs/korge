package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.IMAGES
import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.NINE_PATCHES
import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.PARALLAX_CONFIGS
import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.PARALLAX_IMAGES
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

        exportImageFromAseprite(filename, layers, tags, output, /* OPTIONAL: prefix = "npt_" */) { aseInfo, imageName, _ ->
            // Currently we do not use slice names for nine-patch images - implement later if we need it
            val slicename = ""

            // Create and store nine-patch info
            val ninepaches = assetInfo[NINE_PATCHES] as LinkedHashMap<String, Any>
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

    fun addParallaxImageAse(filename: String, parallaxInfo: ParallaxInfo) {
        // Define defaults for usage as parallax layer images
        val tags = listOf("export")  // TODO remove hard coded value later
        val output = "parallax"

        // (1) Export all parallax layers as individual images
        val layers = arrayListOf<String>()
        parallaxInfo.backgroundLayers.forEach { layer -> layers.add(layer.name) }
        parallaxInfo.foregroundLayers.forEach { layer -> layers.add(layer.name) }
        parallaxInfo.parallaxPlane?.topAttachedLayers?.forEach { layer -> layers.add(layer.name) }
        parallaxInfo.parallaxPlane?.bottomAttachedLayers?.forEach { layer -> layers.add(layer.name) }

        println("Export parallax layer from image file: '${filename}', layers: '${layers}', tags: '${tags}', output: '${output}'")
        val parallaxImages = assetInfo[PARALLAX_IMAGES] as LinkedHashMap<String, Any>

        exportImageFromAseprite(filename, layers, tags, output) { aseInfo, imageName, tag ->
            createImageFramesList(aseInfo, imageName, tag, PARALLAX_IMAGES)
            // Get image map from asset info and store frames list
            println("Processing parallax image: '${imageName}'")
            val layerName = imageName.removePrefix("parallax_")  // Remove default prefix to get the original layer name
            val image = parallaxImages[imageName] as LinkedHashMap<String, Any>

            fun setParallaxLayerInfo(image: LinkedHashMap<String, Any>, layer: ParallaxLayerInfo) {
                image["l"] = linkedMapOf<String, Any?>(
                    "tx" to layer.targetX,  // offset from the left corner of the parallax background image used in VERTICAL_PLANE mode
                    "ty" to layer.targetY,  // offset from the top corner of the parallax background image used in HORIZONTAL_PLANE mode
                    "rx" to layer.repeatX,
                    "ry" to layer.repeatX,
                    "cx" to layer.centerX,  // Center the layer in the parallax background image
                    "cy" to layer.centerY,
                    "sf" to layer.speedFactor,  // If this is null than no movement is applied to the layer
                    "sx" to layer.selfSpeedX,
                    "sy" to layer.selfSpeedY
                )
            }

            fun setParallaxAttachedLayerInfo(image: LinkedHashMap<String, Any>, layer: ParallaxAttachedLayerInfo) {
                image["a"] = linkedMapOf<String, Any>(
                    "i" to layer.attachIndex,
                    "r" to layer.repeat,
                    "a" to layer.attachBottomRight
                )
            }

            parallaxInfo.backgroundLayers.forEach { layer ->
                if (layer.name == layerName) setParallaxLayerInfo(
                    image,
                    layer
                )
            }
            parallaxInfo.foregroundLayers.forEach { layer ->
                if (layer.name == layerName) setParallaxLayerInfo(
                    image,
                    layer
                )
            }
            parallaxInfo.parallaxPlane?.topAttachedLayers?.forEach { layer ->
                if (layer.name == layerName) setParallaxAttachedLayerInfo(
                    image,
                    layer
                )
            }
            parallaxInfo.parallaxPlane?.bottomAttachedLayers?.forEach { layer ->
                if (layer.name == layerName) setParallaxAttachedLayerInfo(
                    image,
                    layer
                )
            }
        }

        // (2) Export slices from 2.5 D parallax plane if available
        parallaxInfo.parallaxPlane?.let { parallaxPlane ->
            val planeName = parallaxPlane.name
            println("Export parallax plane from image file: '${filename}', layer: '${planeName}', tags: '${tags}', output: '${output}'")

            exportImageFromAseprite(filename, listOf(planeName), tags, output, slice = true) { aseInfo, imageName, tag ->
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

                // Create object for parallax plane textures if not existing yet
                val name = "${imageName}_${planeName}"
                if (!parallaxImages.containsKey(name)) parallaxImages[name] = arrayListOf<LinkedHashMap<String ,Any>>()

                // Get the parallax plane index number
                val planeName = imageName.removePrefix("parallax_")  // Remove default prefix to get the original layer name

                val regex = "_slice(\\d+)$".toRegex()
                val match = regex.find(planeName)
                val planeIndex = match?.groupValues?.get(1)?.toInt() ?: error("Cannot get plane index of texture '${imageName}'!")

                parallaxImages[imageName] = linkedMapOf<String, Any>(
                    "f" to frames,
                    "i" to planeIndex,
                    "sf" to getParallaxPlaneSpeedFactor(planeIndex, parallaxInfo.parallaxHeight, parallaxPlane.speedFactor)
                )
            }
        }

        // (3) Store generic parallax config info
        val parallaxConfigs = assetInfo[PARALLAX_CONFIGS] as LinkedHashMap<String, Any>
        parallaxConfigs[parallaxInfo.name] = linkedMapOf<String, Any?>(
            "w" to parallaxInfo.parallaxWidth,
            "h" to parallaxInfo.parallaxHeight,
            "m" to parallaxInfo.mode,
            "b" to parallaxInfo.backgroundLayers.map { layer -> layer.name },
            "f" to parallaxInfo.foregroundLayers.map { layer -> layer.name },
            "p" to if (parallaxInfo.parallaxPlane != null) linkedMapOf<String, Any?>(
                "n" to parallaxInfo.parallaxPlane.name,
                "sf" to parallaxInfo.parallaxPlane.speedFactor,
                "s" to parallaxInfo.parallaxPlane.selfSpeed,
                "t" to parallaxInfo.parallaxPlane.topAttachedLayers.map { layer -> layer.name },
                "b" to parallaxInfo.parallaxPlane.bottomAttachedLayers.map { layer -> layer.name }
            ) else null
        )
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
        configure: (aseInfo: ASEInfo, imageName: String, tag: String) -> Unit
    ) {
        val aseFile = getFile(filename)
        val aseInfo = loadAseInfo(aseFile)
        checkLayersTagsAvailable(aseInfo, filename, layers, tags)

        val slice = if (slice) "_{slice}" else ""

        if (layers.isNotEmpty()) {
            val useLayerName = layers.size != 1  // Use layer name in output if multiple layers are specified

            for (layer in layers) {
                if (tags.isNotEmpty()) {
                    val useTagName = tags.size != 1  // Use tag name in output if multiple tags are specified

                    for (tag in tags) {
                        val imageName = if (useLayerName && useTagName) "${prefix}${output}_${layer}_${tag}${slice}"
                        else if (useLayerName) "${prefix}${output}_${layer}${slice}"
                        else if (useTagName) "${prefix}${output}_${tag}${slice}"
                        else "${prefix}${output}"
                        val cmd = arrayOf(asepriteExe, "-b",
                            "--layer", layer,
                            "--tag", tag,
                            aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${imageName}_{frame0}.png").absolutePath)
                        runAsepriteExport(cmd)
                        configure(aseInfo, imageName, tag)
                    }
                } else {
                    val imageName = if (useLayerName) "${prefix}${output}_${layer}${slice}" else "${prefix}${output}${slice}"
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
                    val imageName = if (useTagName) "${prefix}${output}_${tag}${slice}" else "${prefix}${output}${slice}"
                    val cmd = arrayOf(asepriteExe, "-b",
                        "--tag", tag,
                        aseFile.absolutePath, "--save-as", exportTilesDir.resolve("${imageName}_{frame0}.png").absolutePath)
                    runAsepriteExport(cmd)
                    configure(aseInfo, imageName, tag)
                }
            } else {
                val imageName = "${prefix}${output}${slice}"
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
