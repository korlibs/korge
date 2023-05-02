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
        fun write(output: File) {
            val imageOut = File(output.parentFile, output.nameWithoutExtension + ".png")
            (info["meta"] as MutableMap<String, Any?>)["image"] = imageOut.name
            output.writeText(jsonStringOf(info))
            image.writeTo(imageOut)
        }
    }

    fun packImages(vararg folders: File): AtlasInfo {
        val images: List<Pair<File, SimpleBitmap>> = folders.flatMap { base -> base.walk().mapNotNull {
            if (it.isFile) {
                try {
                    it.relativeTo(base) to SimpleBitmap(it)
                } catch (e: Throwable) {
                    e.printStackTrace()
                    null
                }
            } else {
                null
            }
        } }

        val PADDING = 2

        val packer = NewBinPacker.MaxRectsPacker(4096, 4096, PADDING * 2, NewBinPacker.IOption(
            smart = true,
            pot = true,
            square = false,
            allowRotation = true,
            //allowRotation = false,
            tag = false,
            border = PADDING
        ))

        packer.addArray(images.map { (file, image) ->
            val fullArea = Rectangle(0, 0, image.width, image.height)
            val trimArea = image.trim()
            val trimmedImage = image.slice(trimArea)
            //println(trimArea == fullArea)
            NewBinPacker.Rectangle(width = trimmedImage.width, height = trimmedImage.height, raw = Info(
                file, fullArea, trimArea, trimmedImage
            ))
        })

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

                val chunk = if (rect.rot) info.trimmedImage.rotate90() else info.trimmedImage
                out.put(rect.x - PADDING, rect.y - PADDING, chunk.extrude(PADDING))
                //out.put(rect.x, rect.y, chunk)

                val obj = LinkedHashMap<String, Any?>()

                fun Rectangle.toObj() = mapOf("x" to x, "y" to y, "w" to width, "h" to height)
                fun Dimension.toObj() = mapOf("w" to width, "h" to height)

                obj["frame"] = Rectangle(rect.x, rect.y, rect.width, rect.height).toObj()
                obj["rotated"] = rect.rot
                obj["trimmed"] = info.trimArea != info.fullArea
                obj["spriteSourceSize"] = info.fullArea.toObj()
                obj["sourceSize"] = info.fullArea.size.toObj()

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

            return AtlasInfo(out, atlasOut)
        }

        TODO()
    }
}
