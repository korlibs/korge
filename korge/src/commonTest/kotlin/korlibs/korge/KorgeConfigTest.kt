package korlibs.korge

import korlibs.math.geom.*
import kotlin.test.*

class KorgeConfigTest {
    @Test
    fun testSize() {
        val size1 = SizeInt(111, 222)
        val size2 = SizeInt(333, 444)
        KorgeConfig(windowSize = size1).also { config ->
            assertEquals(size1, config.windowSize)
            assertEquals(size1, config.virtualSize)
        }
        KorgeConfig(windowSize = size1, virtualSize = size2).also { config ->
            assertEquals(size1, config.windowSize)
            assertEquals(size2, config.virtualSize)
        }
    }

    @Test
    fun testImageFormats() {
        val config = KorgeConfig()
        assertNotNull(config.imageFormats.toString()) // Assert that it doesn't throw due to stack overflow
    }
}
