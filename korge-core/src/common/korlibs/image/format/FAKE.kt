package korlibs.image.format

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.io.file.*
import korlibs.io.lang.*
import korlibs.io.stream.*

object FAKE : ImageFormat("fake") {
    override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
        val fileName = props.filename
        if (fileName.pathInfo.extensionLC != "fake") return null
        val fakeString = _getFakeString(fileName) { s.readBytes(1024) }
        return generateImageInfoFromString(fakeString)
    }
    override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
        return ImageData(_decode(props.filename) { s.readBytes(1024) })
    }
    override suspend fun decode(file: VfsFile, props: ImageDecodingProps): Bitmap {
        return _decode(file.baseName) { file.openUse { readBytesUpTo(1024) } }
    }

    private inline fun _getFakeString(baseName: String, readBytes: () -> ByteArray): String {
        return when {
            isValidFakeString(baseName) -> baseName
            else -> readBytes().toString(Charsets.UTF8)
        }
    }

    private inline fun _decode(baseName: String, readBytes: () -> ByteArray): Bitmap {
        return generateFromString(_getFakeString(baseName, readBytes))
    }

    private fun isValidFakeString(str: String): Boolean {
        return str.substringBefore('.').split("x").size >= 2
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
