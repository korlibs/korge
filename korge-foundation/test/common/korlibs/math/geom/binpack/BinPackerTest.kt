package korlibs.math.geom.binpack

import korlibs.math.geom.*
import kotlin.test.*

class BinPackerTest {
    @Test
    fun name() {
        val packer = BinPacker(100, 100)
        val result = packer.addBatch(listOf(Size(20, 10), Size(10, 30), Size(100, 20), Size(20, 80)))
        assertEquals(
            "[Rectangle(x=20, y=50, width=20, height=10), Rectangle(x=20, y=20, width=10, height=30), Rectangle(x=0, y=0, width=100, height=20), Rectangle(x=0, y=20, width=20, height=80)]",
            result.toString()
        )
    }

    @Test
    fun packSeveral() {
        val packs = BinPacker.packSeveral(
            Size(10, 10),
            listOf(Size(10, 10), Size(5, 5), Size(5, 5), Size(5, 5), Size(5, 5), Size(10, 5), Size(5, 10), Size(5, 5)).map { Sizeable(it) }
        )
        assertEquals(4, packs.size)

        assertEquals(10.0, packs.first().width)
        assertEquals(10.0, packs.first().height)
        assertEquals(10.0, packs.first().maxWidth)
        assertEquals(10.0, packs.first().maxHeight)

        assertEquals(10.0, packs.last().width)
        assertEquals(10.0, packs.last().height)
        assertEquals(10.0, packs.last().maxWidth)
        assertEquals(10.0, packs.last().maxHeight)

        assertEquals("[Rectangle(x=0, y=0, width=10, height=10)]", packs[0].rectsStr)
        assertEquals(
            "[Rectangle(x=0, y=0, width=5, height=5), Rectangle(x=0, y=5, width=5, height=5), Rectangle(x=5, y=0, width=5, height=5), Rectangle(x=5, y=5, width=5, height=5)]",
            packs[1].rectsStr
        )
        assertEquals("[Rectangle(x=0, y=0, width=10, height=5)]", packs[2].rectsStr)
        assertEquals(
            "[Rectangle(x=0, y=0, width=5, height=10), Rectangle(x=5, y=0, width=5, height=5)]",
            packs[3].rectsStr
        )
    }

    @Test
    fun packZero() {
        val packs = BinPacker.packSeveral(Size(10, 10), listOf(Size(0, 0)).map { Sizeable(it) })
        assertEquals(1, packs.size)
        assertEquals("[Rectangle(x=0, y=0, width=0, height=0)]", packs[0].rectsStr)
    }
}
