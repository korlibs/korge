package korlibs.image.font

import korlibs.memory.readS32BE
import korlibs.image.format.ImageDecodingProps
import korlibs.image.format.ImageFormat
import korlibs.image.format.RegisteredImageFormats
import korlibs.io.file.VfsFile

suspend fun VfsFile.readFont(preload: Boolean = false, props: ImageDecodingProps = ImageDecodingProps.DEFAULT, mipmaps: Boolean = true): Font {
    val header = readRangeBytes(0 until 16)
    return when (header.readS32BE(0)) {
        0x74746366, 0x4F54544F, 0x00010000 -> readTtfFont(preload)
        //0x504B0304 -> this.openAsZip { it.listRecursive().filter { it.extensionLC == "ttf" || it.extensionLC == "fnt" || it.extensionLC == "ttc" || it.extensionLC == "otf" }.firstOrNull()?.readFont() ?: error("Can't find TTF or FNT on zip") }
        else -> readBitmapFont(props, mipmaps)
    }
}
