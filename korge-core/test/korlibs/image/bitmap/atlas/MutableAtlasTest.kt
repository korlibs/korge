package korlibs.image.bitmap.atlas

import korlibs.image.atlas.MutableAtlas
import korlibs.image.bitmap.*
import korlibs.image.color.Colors
import korlibs.math.geom.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MutableAtlasTest {
    @Test
    fun test() {
        val atlas = MutableAtlas<Unit>(100, 100, border = 2)
        val slice1 = atlas.add(Bitmap32(10, 10, Colors.RED), Unit, "a")
        val version1 = atlas.bitmap.contentVersion
        val slice2 = atlas.add(Bitmap32(10, 10, Colors.GREEN), Unit, "b")
        val version2 = atlas.bitmap.contentVersion
        val slice3 = atlas.add(Bitmap32(10, 10, Colors.BLUE), Unit, "c")
        val version3 = atlas.bitmap.contentVersion
        assertNotEquals(version1, version2)
        assertNotEquals(version2, version3)
        assertEquals("a", slice1.name)
        assertEquals("b", slice2.name)
        assertEquals("c", slice3.name)
        assertEquals(RectangleInt(2, 2, 10, 10), slice1.slice.bounds)
        assertEquals(RectangleInt(2, 16, 10, 10), slice2.slice.bounds)
        assertEquals(RectangleInt(2, 30, 10, 10), slice3.slice.bounds)
        assertEquals(Colors.RED, atlas.bitmap[5, 5])
        assertEquals(Colors.GREEN, atlas.bitmap[5, 20])
        assertEquals(Colors.BLUE, atlas.bitmap[5, 35])
    }
}
