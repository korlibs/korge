package com.soywiz.korim.font

// @TODO: Can get get glyphs from native fonts on JS?
actual val nativeSystemFontProvider: NativeSystemFontProvider = FallbackNativeSystemFontProvider(DefaultTtfFont)
