package korlibs.korge

import korlibs.math.geom.*
import kotlin.test.*

class KorgeConfigTest {
    @Test
    fun testSize() {
        val size1 = SizeInt(111, 222)
        val size2 = SizeInt(333, 444)
        KorgeConfig(windowSize = size1).also { config ->
            assertEquals(size1, config.finalWindowSize)
            assertEquals(size1, config.finalVirtualSize)
        }
        KorgeConfig(windowSize = size1, virtualSize = size2).also { config ->
            assertEquals(size1, config.finalWindowSize)
            assertEquals(size2, config.finalVirtualSize)
        }
    }

    @Test
    fun testImageFormats() {
        val config = KorgeConfig()
        assertEquals("ImageFormats(0)[]", config.finalImageFormats.toString())
    }
}
