package com.soywiz.korim.font

import kotlin.native.concurrent.*

@ThreadLocal
actual val nativeSystemFontProvider: NativeSystemFontProvider = FolderBasedNativeSystemFontProvider()
