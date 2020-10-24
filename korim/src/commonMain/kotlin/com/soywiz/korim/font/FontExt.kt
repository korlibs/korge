package com.soywiz.korim.font

import com.soywiz.korim.format.*
import com.soywiz.korio.file.*

suspend fun VfsFile.readFont(preload: Boolean = false, imageFormat: ImageFormat = RegisteredImageFormats, mipmaps: Boolean = true): Font =
    try {
        readTtfFont(preload)
    } catch (e: Throwable) {
        readBitmapFont(imageFormat, mipmaps)
    }
