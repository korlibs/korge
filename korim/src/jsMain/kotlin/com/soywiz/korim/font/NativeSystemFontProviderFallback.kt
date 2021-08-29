package com.soywiz.korim.font

// Note: not using by lazy or direct set it because it would be included in the output even if not referenced
private var nativeSystemFontProviderOrNull: NativeSystemFontProvider? = null

// @TODO: Can we get glyphs from native fonts on JS?
actual val nativeSystemFontProvider: NativeSystemFontProvider get() {
    if (nativeSystemFontProviderOrNull == null) {
        nativeSystemFontProviderOrNull = FallbackNativeSystemFontProvider(DefaultTtfFont)
    }
    return nativeSystemFontProviderOrNull!!
}
