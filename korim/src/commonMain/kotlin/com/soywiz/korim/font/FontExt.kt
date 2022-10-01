package com.soywiz.korim.font

import com.soywiz.kmem.readS32BE
import com.soywiz.korim.format.ImageDecodingProps
import com.soywiz.korim.format.ImageFormat
import com.soywiz.korim.format.RegisteredImageFormats
import com.soywiz.korio.file.VfsFile

suspend fun VfsFile.readFont(preload: Boolean = false, props: ImageDecodingProps = ImageDecodingProps.DEFAULT, mipmaps: Boolean = true): Font {
    val header = readRangeBytes(0 until 16)
    return when (header.readS32BE(0)) {
        0x74746366, 0x4F54544F, 0x00010000 -> readTtfFont(preload)
        //0x504B0304 -> this.openAsZip { it.listRecursive().filter { it.extensionLC == "ttf" || it.extensionLC == "fnt" || it.extensionLC == "ttc" || it.extensionLC == "otf" }.firstOrNull()?.readFont() ?: error("Can't find TTF or FNT on zip") }
        else -> readBitmapFont(props, mipmaps)
    }
}
