package korlibs.korge.gradle.texpacker

import com.android.build.gradle.internal.cxx.json.*
import korlibs.korge.gradle.*
import java.awt.*
import java.io.*

object NewTexturePacker {
    data class Info(
        val file: File,
        val fullArea: Rectangle,
        val trimArea: Rectangle,
        val trimmedImage: SimpleBitmap,
    )

    data class AtlasInfo(
        val image: SimpleBitmap,
        val info: Map<String, Any?>
    ) {
        fun write(output: File): File {
            val imageOut = File(output.parentFile, output.nameWithoutExtension + ".png")
            (info["meta"] as MutableMap<String, Any?>)["image"] = imageOut.name
            output.writeText(jsonStringOf(info))
            image.writeTo(imageOut)
            return imageOut
        }
    }

    data class FileWithBase(val base: File, val file: File) {
        val relative = file.relativeTo(base)
    }

    fun getAllFiles(vararg folders: File): List<FileWithBase> {
        return folders.flatMap { base -> base.walk().mapNotNull {
            when {
                it.name.startsWith('.') -> null
                it.isFile -> FileWithBase(base, it)
                else -> null
            }
        } }
    }

    /**
     * Packs tilesets from the given folders into texture atlases.
     *
     * Each tileset image is split into individual tiles of the specified size.
     * These tiles are checked for duplicates and then packed into atlases.
     *
     * @param folders The folders containing tilesets to be packed.
     * @param padding The padding to apply around each tile in the atlas.
     * @param tileSize The size of each tile in the tilesets.
     * @return A list of AtlasInfo objects representing the packed atlases.
     */
    fun packTilesets(
        vararg folders: File,
        padding: Int = 1,
        tileSize: Int = 16
    ): List<AtlasInfo> {
        // Load all tilesets and create SimpleBitmap instances
        val tilesets: List<Pair<File, SimpleBitmap>> = getAllFiles(*folders).mapNotNull {
            try {
                it.relative to SimpleBitmap(it.file)
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        }
        // Split each tileset into tiles
        val images = arrayListOf<Pair<File, SimpleBitmap>>()
        tilesets.forEach { tileset ->
            val (file, image) = tileset
            if (image.width % tileSize != 0 || image.height % tileSize != 0) {
                throw IllegalArgumentException("Tileset image size must be multiple of tileSize ($tileSize): $file with size ${image.width}x${image.height}")
            }
            images += image.splitInListOfTiles(file.nameWithoutExtension, tileSize)
        }

        return packImages(images, enableRotation = false, enableTrimming = false, padding = padding, trimFileName = true, removeDuplicates = true)
    }

    /**
     * Packs images from the given folders into texture atlases.
     *
     * @param folders The folders containing images to be packed.
     * @param enableRotation Whether to allow rotation of images for better packing.
     * @param enableTrimming Whether to trim transparent pixels from images.
     * @param padding The padding to apply around each image in the atlas.
     * @param trimFileName Whether to trim the file name in the output info.
     * @param removeDuplicates Whether to remove duplicate images and map duplicates in atlas info.
     * @return A list of AtlasInfo objects representing the packed atlases.
     */
    fun packImages(
        vararg folders: File,
        enableRotation: Boolean = true,
        enableTrimming: Boolean = true,
        padding: Int = 2,
        trimFileName: Boolean = false,
        removeDuplicates: Boolean = false
    ): List<AtlasInfo> {
        val images: List<Pair<File, SimpleBitmap>> = getAllFiles(*folders).mapNotNull {
            try {
                it.relative to SimpleBitmap(it.file)
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        }
        return packImages(images, enableRotation, enableTrimming, padding, trimFileName, removeDuplicates)
    }

    private fun packImages(
        images: List<Pair<File, SimpleBitmap>>,
        enableRotation: Boolean,
        enableTrimming: Boolean,
        padding: Int,
        trimFileName: Boolean,
        removeDuplicates: Boolean
    ): List<AtlasInfo> {
        val packer = NewBinPacker.MaxRectsPacker(4096, 4096, padding * 2, NewBinPacker.IOption(
            smart = true,
            pot = true,
            square = false,
            allowRotation = enableRotation,
            tag = false,
            border = padding
        ))

        // Handle duplicate images if requested
        val tileMapping = linkedMapOf<File, File>()  // In case of duplicates, map duplicate file to original file
        val mappedImages = if (removeDuplicates) {   // mappedImages will contain only unique images if removeDuplicates is true
            // Remove duplicate images
            val uniqueMap = linkedMapOf<Int, Pair<File, SimpleBitmap>>()
            for ((fileName, image) in images) {
                val file = if (trimFileName) File(fileName.nameWithoutExtension) else fileName
                val hash = image.hashCode()
                val existing = uniqueMap[hash]
                if (existing == null) {
                    uniqueMap[hash] = file to image
                    tileMapping[file] = file
                } else tileMapping[file] = existing.first
            }
            uniqueMap.values.toList()
        } else {
            // No duplicate removal, use all images
            for ((fileName, _) in images) {
                val file = if (trimFileName) File(fileName.nameWithoutExtension) else fileName
            for ((file, _) in images) {
                tileMapping[file] = file
            }
            images
        }

        // Add images to the packer (possibly without duplicates)
        packer.addArray(mappedImages.map { (file, image) ->
            val fullArea = Rectangle(0, 0, image.width, image.height)
            val trimArea = if (enableTrimming) image.trim() else fullArea
            val trimmedImage = image.slice(trimArea)
            //println(trimArea == fullArea)
            val fileName = if (trimFileName) File(file.nameWithoutExtension) else file
            NewBinPacker.Rectangle(width = trimmedImage.width, height = trimmedImage.height, raw = Info(
                fileName, fullArea, trimArea, trimmedImage
            ))
        })

        // Building atlas info which includes mapping duplicates
        val outAtlases = arrayListOf<AtlasInfo>()
        for (bin in packer.bins) {
            val out = SimpleBitmap(bin.width, bin.height)
            val frames = linkedMapOf<String, Any?>()

            for (rect in bin.rects) {
                val info = rect.raw as Info

                // Check if this rect (image) is used by any duplicate files - if so, map them all to the same rect area in the atlas
                val files = tileMapping.filterValues { it == info.file }.keys
                for (file in files) {
                    val fileName = file.name

                    val chunk = if (rect.rot) info.trimmedImage.flipY().rotate90() else info.trimmedImage
                    out.put(rect.x - padding, rect.y - padding, chunk.extrude(padding))

                    val obj = LinkedHashMap<String, Any?>()

                    fun Dimension.toObj(rot: Boolean): Map<String, Any?> {
                        val w = if (!rot) width else height
                        val h = if (!rot) height else width
                        return mapOf("w" to w, "h" to h)
                    }

                    fun Rectangle.toObj(rot: Boolean): Map<String, Any?> {
                        return mapOf("x" to x, "y" to y) + this.size.toObj(rot)
                    }

                    obj["frame"] = Rectangle(rect.x, rect.y, rect.width, rect.height).toObj(rect.rot)
                    obj["rotated"] = rect.rot
                    obj["trimmed"] = info.trimArea != info.fullArea
                    obj["spriteSourceSize"] = info.trimArea.toObj(false)
                    obj["sourceSize"] = info.fullArea.size.toObj(false)

                    frames[fileName] = obj
                }
            }

            val atlasOut = linkedMapOf<String, Any?>(
                "frames" to frames,
                "meta" to mapOf(
                    "app" to "https://korge.org/",
                    "version" to BuildVersions.KORGE,
                    "image" to "",
                    "format" to "RGBA8888",
                    "size" to mapOf(
                        "w" to bin.width,
                        "h" to bin.height
                    ),
                    "scale" to 1
                ),
            )

            outAtlases.add(AtlasInfo(out, atlasOut))
        }

        return outAtlases
    }
}

/**
 * Split the bitmap into tiles of given size and return them as a list of pairs (File, SimpleBitmap).
 * The File is named as "${name}_${index} without extension."
 */
fun SimpleBitmap.splitInListOfTiles(name: String, tileSize: Int): List<Pair<File, SimpleBitmap>> {
    val tiles = arrayListOf<Pair<File, SimpleBitmap>>()
    val tilesX = this.width / tileSize
    val tilesY = this.height / tileSize
    var index = 0
    for (ty in 0 until tilesY) {
        for (tx in 0 until tilesX) {
            val tileBitmap = this.slice(Rectangle(tx * tileSize, ty * tileSize, tileSize, tileSize))
            val tileFile = File("${name}_${index}")
            tiles.add(tileFile to tileBitmap)
            index++
        }
    }
    return tiles
}
