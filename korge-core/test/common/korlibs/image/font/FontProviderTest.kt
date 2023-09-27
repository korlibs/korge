package korlibs.image.font

import korlibs.platform.*
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
