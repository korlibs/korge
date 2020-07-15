package com.soywiz.korge.font

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.font.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.*

suspend fun VfsFile.readBitmapFontWithMipmaps(imageFormat: ImageFormat = RegisteredImageFormats, mipmaps: Boolean = true): BitmapFont =
    readBitmapFont(imageFormat).also { it.atlas.mipmaps(mipmaps) }
