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

    fun packImages(
        vararg folders: File,
        enableRotation: Boolean = true,
        enableTrimming: Boolean = true,
    ): List<AtlasInfo> {
        val images: List<Pair<File, SimpleBitmap>> = getAllFiles(*folders).mapNotNull {
            try {
                it.relative to SimpleBitmap(it.file)
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        }

        val PADDING = 2

        val packer = NewBinPacker.MaxRectsPacker(4096, 4096, PADDING * 2, NewBinPacker.IOption(
            smart = true,
            pot = true,
            square = false,
            allowRotation = enableRotation,
            //allowRotation = false,
            tag = false,
            border = PADDING
        ))

        packer.addArray(images.map { (file, image) ->
            val fullArea = Rectangle(0, 0, image.width, image.height)
            val trimArea = if (enableTrimming) image.trim() else fullArea
            val trimmedImage = image.slice(trimArea)
            //println(trimArea == fullArea)
            NewBinPacker.Rectangle(width = trimmedImage.width, height = trimmedImage.height, raw = Info(
                file, fullArea, trimArea, trimmedImage
            ))
        })

        val outAtlases = arrayListOf<AtlasInfo>()
        for (bin in packer.bins) {
            //val rwidth = bin.rects.maxOf { it.right }
            //val rheight = bin.rects.maxOf { it.bottom }
            //val maxWidth = bin.maxWidth
            //val maxHeight = bin.maxHeight
            //val out = SimpleBitmap(rwidth, rheight)
            val out = SimpleBitmap(bin.width, bin.height)
            //println("${bin.width}x${bin.height}")

            val frames = linkedMapOf<String, Any?>()

            for (rect in bin.rects) {
                val info = rect.raw as Info
                val fileName = info.file.name
                //println("$rect :: info=$info")

                val chunk = if (rect.rot) info.trimmedImage.flipY().rotate90() else info.trimmedImage
                out.put(rect.x - PADDING, rect.y - PADDING, chunk.extrude(PADDING))
                //out.put(rect.x, rect.y, chunk)

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
