package com.soywiz.korim.font

import com.soywiz.kmem.Os
import com.soywiz.kmem.Platform
import com.soywiz.kmem.Runtime
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FontProviderTest {
    @Test
    fun test() {
        val jsFonts = createNativeSystemFontProvider(EmptyCoroutineContext, Platform(runtime = Runtime.JS))
        val iosFonts = createNativeSystemFontProvider(EmptyCoroutineContext, Platform(os = Os.IOS))
        assertIs<FallbackNativeSystemFontProvider>(jsFonts)
        assertIs<FolderBasedNativeSystemFontProvider>(iosFonts)
        assertEquals(listOf("/System/Library/Fonts"), iosFonts.folders)
    }
}
