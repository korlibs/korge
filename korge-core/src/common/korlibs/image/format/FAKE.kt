package korlibs.image.format

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.io.file.*
import korlibs.io.lang.*
import korlibs.io.stream.*

object FAKE : ImageFormat("fake") {
    override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? = generateImageInfoFromString(s.readString(1024))
    override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData = ImageData(generateFromString(s.readString(1024)))
    override suspend fun decode(file: VfsFile, props: ImageDecodingProps): Bitmap {
        val numbers = file.baseName.substringBefore('.').split("x")
        return when {
            numbers.size < 2 -> generateFromString(file.openUse { readBytesUpTo(1024).toString(Charsets.UTF8) })
            else -> generateFromString(file.baseName)
        }
    }

    fun generateImageInfoFromString(str: String): ImageInfo {
        val (widthS, heightS) = str.substringBefore('.').split("x") + listOf("128", "128")
        val width = widthS.toIntOrNull() ?: 128
        val height = heightS.toIntOrNull() ?: 128
        return ImageInfo {
            this.width = width
            this.height = height
        }
    }

    fun generateFromString(str: String): Bitmap {
        val info = generateImageInfoFromString(str)
        return Bitmap32(info.width, info.height) { x, y ->
            val a = ((x / 32) % 2) != 0
            val b = ((y / 32) % 2) != 0
            if (a xor b) Colors.RED else Colors.BLUE
        }
    }
}
