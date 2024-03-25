package korlibs.image.format

import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.platform.*
import kotlin.test.*

class WEBPTest {
    @Test
    fun test1() = suspendTest {
        if (Platform.isWasm) { println("Skipping WEBPTest WASM for now as it hangs: https://github.com/korlibs/korge/pull/2057#issuecomment-1837089337"); return@suspendTest }
        if (Platform.isIos || Platform.isTvos) {
            println("Skipping WEBPTest for now on iOS")
            return@suspendTest
        }
        WEBP()
        val bmp = resourcesVfs["Exif5-2x.webp"].readBitmap(ImageDecodingProps(format = WEBP(), preferKotlinDecoder = true))
        assertEquals("256x256", "${bmp.size}")
    }

    @Test
    fun test2() = suspendTest {
        if (Platform.isWasm) { println("Skipping WEBPTest WASM for now as it hangs: https://github.com/korlibs/korge/pull/2057#issuecomment-1837089337"); return@suspendTest }
        if (Platform.isIos || Platform.isTvos) {
            println("Skipping WEBPTest for now on iOS")
            return@suspendTest
        }
        WEBP()
        val bmp = WEBP().decode(resourcesVfs["Exif5-2x.webp"])
        assertEquals("256x256", "${bmp.size}")
    }
}
