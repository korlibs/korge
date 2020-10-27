package com.soywiz.korim.font

// @TODO: Can we get glyphs from native fonts on JS?
actual val nativeSystemFontProvider: NativeSystemFontProvider = FallbackNativeSystemFontProvider(DefaultTtfFont)
