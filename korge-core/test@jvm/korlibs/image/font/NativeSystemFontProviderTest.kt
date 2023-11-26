package korlibs.image.font

import org.junit.Test
import kotlin.test.*

class NativeSystemFontProviderTest {
    @Test
    fun test() {
        assertEquals(listOf("Sani Trixie Sans Regular"), FallbackNativeSystemFontProvider(DefaultTtfFont).listFontNames())
        //println()
        //println(FolderBasedNativeSystemFontProvider().listFontNames())
        //println(nativeSystemFontProvider.listFontNames())
        //println(FolderBasedNativeSystemFontProvider().locateFontByName("arial")!!.numGlyphs)
        //val fonts = FolderBasedNativeSystemFontProvider()
        //val font = fonts.locateFontByName("Arial")
    }
}
