package com.soywiz.korim.font

private val iosFontsFolders = listOf("/System/Library/Fonts/Cache", "/System/Library/Fonts")
actual val nativeSystemFontProvider: NativeSystemFontProvider = FolderBasedNativeSystemFontProvider(iosFontsFolders)
