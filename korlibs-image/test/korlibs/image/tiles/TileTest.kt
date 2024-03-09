package korlibs.image.tiles

import kotlin.test.*

class TileTest {
    @Test
    fun testFromRaw() {
        Tile.fromRaw(-1, -1).also { tile ->
            assertEquals(-1, tile.rawLow)
            assertEquals(-1, tile.rawHigh)
            assertEquals(-1, tile.data)
        }
        Tile.fromRaw(-1234567, 78912345).also { tile ->
            assertEquals(-1234567, tile.rawLow)
            assertEquals(78912345, tile.rawHigh)
            assertEquals(-1234567, tile.data)
        }
    }

    @Test
    fun testFromConstructor() {
        assertEquals(
            "Tile(tile=12345, offsetX=-23456, offsetY=32767, flipX=true, flipY=false, rotate=true)",
            Tile(12345, offsetX = -23456, offsetY = 32767, flipX = true, flipY = false, rotate = true).toStringInfo()
        )
        assertEquals(
            "Tile(tile=33534431, offsetX=32767, offsetY=-32768, flipX=false, flipY=true, rotate=false)",
            Tile(33534431, offsetX = 32767, offsetY = -32768, flipX = false, flipY = true, rotate = false).toStringInfo()
        )
    }
}
