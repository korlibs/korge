package korlibs.image.format

import korlibs.image.atlas.*
import korlibs.image.bitmap.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.math.geom.*
import kotlin.test.*

class BmpSliceTest {
    val props = ImageDecodingProps(format = ImageFormats(PNG))

    @Test
    fun testName() = suspendTest {
        val slice = resourcesVfs["rgba.png"].readBitmapSlice(name = "hello", props = props)
        assertEquals("hello", slice.name)
        assertEquals(SizeInt(4, 1), slice.bounds.size)
    }

    @Test
    fun testPacking() = suspendTest {
        val atlas = AtlasPacker.pack(listOf(
            resourcesVfs["rgba.png"].readBitmapSlice(name = "hello", props = props)
        ))
        val slice = atlas["hello"]
        assertEquals("hello", slice.name)
        assertEquals(SizeInt(4, 1), slice.bounds.size)
    }

    @Test
    fun testPackingMutable() = suspendTest {
        val atlas = MutableAtlasUnit()
        resourcesVfs["rgba.png"].readBitmapSlice(atlas = atlas, name = "hello", props = props)
        val slice = atlas["hello"]
        assertEquals("hello", slice.name)
        assertEquals(SizeInt(4, 1), slice.bounds.size)
    }
}
