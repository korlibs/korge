package com.soywiz.korim.font

import com.soywiz.kmem.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.flow.*

suspend fun VfsFile.readFont(preload: Boolean = false, imageFormat: ImageFormat = RegisteredImageFormats, mipmaps: Boolean = true): Font {
    val header = readRangeBytes(0 until 16)
    return when (header.readS32BE(0)) {
        0x74746366, 0x4F54544F, 0x00010000 -> readTtfFont(preload)
        0x504B0304 -> this.openAsZip {
            it.listRecursive().filter { it.extensionLC == "ttf" || it.extensionLC == "fnt" || it.extensionLC == "ttc" || it.extensionLC == "otf" }.firstOrNull()?.readFont() ?: error("Can't find TTF or FNT on zip")
        }
        else -> readBitmapFont(imageFormat, mipmaps)
    }
}
