package korlibs.math.geom

import kotlin.test.*

class ScaleTest {
    @Test
    fun testConstructors() {
        assertEquals(Scale(1f, 1f), Scale(1f))
        assertEquals(Scale(2f, 2f), Scale(2f))
        assertEquals(Scale(-2f, -2f), Scale(-2f))
        assertEquals(Scale(1f, 1f), Scale())
        assertEquals(Scale(1f, 1f), Scale.IDENTITY)
    }

    @Test
    fun testConversion() {
        assertEquals(Scale(2f, 3f), Vector2F(2f, 3f).toScale())
        assertEquals(Vector2F(2f, 3f), Scale(2f, 3f).toVector2())
        assertEquals(MScale(2.0, 3.0), Scale(2f, 3f).toMutable())
        assertEquals(Scale(2f, 3f), MScale(2.0, 3.0).toImmutable())
        run {
            val out = MScale(1.0, 1.0)
            Scale(2f, 3f).toMutable(out)
            assertEquals(MScale(2.0, 3.0), out)
        }
    }

    @Test
    fun testProperties() {
        val scale = Scale(2f, 4f)

        assertEquals(3f, scale.avg)
        assertEquals(3f, scale.scaleAvg)
        assertEquals(2f, scale.scaleX)
        assertEquals(4f, scale.scaleY)

        assertEquals(3.0, scale.avgD)
        assertEquals(3.0, scale.scaleAvgD)
        assertEquals(2.0, scale.scaleXD)
        assertEquals(4.0, scale.scaleYD)
    }

    @Test
    fun testArithmetic() {
        assertEquals(Scale(-2f, -3f), -Scale(2f, 3f))
        assertEquals(Scale(2f, 3f), +Scale(2f, 3f))
        assertEquals(Scale(2f), Scale.IDENTITY * 2f)
        assertEquals(Scale(6f, 8f), Scale(2f, 3f) + Scale(4f, 5f))
        assertEquals(Scale(-2f, -3f), Scale(2f, 3f) - Scale(4f, 6f))
        assertEquals(Scale(8f, 15f), Scale(2f, 3f) * Scale(4f, 5f))
        assertEquals(Scale(4f, 2f), Scale(8f, 6f) / Scale(2f, 3f))
        assertEquals(Scale(4f, 3f), Scale(8f, 6f) / 2f)
        assertEquals(Scale(1f, 2f), Scale(4f, 5f) % Scale(3f, 3f))
    }
}
