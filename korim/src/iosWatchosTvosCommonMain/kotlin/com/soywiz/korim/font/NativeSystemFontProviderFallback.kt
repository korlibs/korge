package com.soywiz.korim.font

import kotlin.native.concurrent.*

private val iosFontsFolders = listOf("/System/Library/Fonts/Cache", "/System/Library/Fonts")

@ThreadLocal
actual val nativeSystemFontProvider: NativeSystemFontProvider = FolderBasedNativeSystemFontProvider(iosFontsFolders)
