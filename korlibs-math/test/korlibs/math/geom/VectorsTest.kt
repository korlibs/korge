package korlibs.math.geom

import kotlin.test.*

class VectorsTest {
    @Test
    fun testAbs() {
        assertEquals(Vector2F(3f, 2f), Vector2F(-3f, +2f).absoluteValue)
        assertEquals(Vector2F(3f, 2f), abs(Vector2F(+3f, -2f)))

        val signs = listOf(-1, +1)
        for (s1 in signs) {
            for (s2 in signs) {
                assertEquals(Vector2F(3f, 2f), Vector2F(3f * s1, 2f * s2).absoluteValue)
                assertEquals(Vector2F(3f, 2f), abs(Vector2F(3f * s1, 2f * s2)))
                for (s3 in signs) {
                    assertEquals(Vector3F(3f, 2f, 4f), Vector3F(3f * s1, 2f * s2, 4f * s3).absoluteValue)
                    assertEquals(Vector3F(3f, 2f, 4f), abs(Vector3F(3f * s1, 2f * s2, 4f * s3)))
                    for (s4 in signs) {
                        assertEquals(Vector4F(3f, 2f, 4f, 5f), Vector4F(3f * s1, 2f * s2, 4f * s3, 5f * s4).absoluteValue)
                        assertEquals(Vector4F(3f, 2f, 4f, 5f), abs(Vector4F(3f * s1, 2f * s2, 4f * s3, 5f * s4)))
                    }
                }
            }
        }
    }

    @Test
    fun testClamp() {
        assertEquals(Vector2F(.5f, 1f), Vector2F(-3f, +2f).clamp(.5f, 1f))
        assertEquals(Vector2F(.5f, 2f), Vector2F(-3f, +2f).clamp(.5f, 3f))
        assertEquals(Vector2F(-3f, 2f), Vector2F(-3f, +2f).clamp(-4f, 3f))

        assertEquals(Vector3F(.5f, 1f, .75f), Vector3F(-3f, +2f, .75f).clamp(.5f, 1f))
        assertEquals(Vector3F(.5f, 2f, .75f), Vector3F(-3f, +2f, .75f).clamp(.5f, 3f))
        assertEquals(Vector3F(-3f, 2f, .75f), Vector3F(-3f, +2f, .75f).clamp(-4f, 3f))
        assertEquals(Vector3F(-3f, 2f, 3f), Vector3F(-3f, +2f, 4f).clamp(-4f, 3f))

        assertEquals(Vector4F(.5f, 1f, .75f, 1f), Vector4F(-3f, +2f, .75f, 1f).clamp(.5f, 1f))
        assertEquals(Vector4F(.5f, 2f, .75f, 1f), Vector4F(-3f, +2f, .75f, 1f).clamp(.5f, 3f))
        assertEquals(Vector4F(-3f, 2f, .75f, 1f), Vector4F(-3f, +2f, .75f, 1f).clamp(-4f, 3f))
        assertEquals(Vector4F(-3f, 2f, 3f, -4f), Vector4F(-3f, +2f, 4f, -7f).clamp(-4f, 3f))
    }

    @Test
    fun testMin() {
        assertEquals(Vector2F(5f, 20f), min(Vector2F(10f, 20f), Vector2F(5f, 30f)))
        assertEquals(Vector2F(5f, 20f), min(Vector2F(5f, 30f), Vector2F(10f, 20f)))
        assertEquals(Vector2F(5f, 20f), min(Vector2F(5f, 20f), Vector2F(10f, 30f)))
        assertEquals(Vector2F(5f, 20f), min(Vector2F(10f, 30f), Vector2F(5f, 20f)))

        assertEquals(Vector3F(5f, 20f, -7f), min(Vector3F(10f, 20f, -7f), Vector3F(5f, 30f, +7f)))
        assertEquals(Vector3F(5f, 20f, -7f), min(Vector3F(5f, 30f, +7f), Vector3F(10f, 20f, -7f)))
        assertEquals(Vector3F(5f, 20f, -7f), min(Vector3F(5f, 20f, -7f), Vector3F(10f, 30f, 0f)))
        assertEquals(Vector3F(5f, 20f, -7f), min(Vector3F(10f, 30f, 0f), Vector3F(5f, 20f, -7f)))

        assertEquals(Vector4F(5f, 20f, -7f, -3f), min(Vector4F(10f, 20f, -7f, -3f), Vector4F(5f, 30f, +7f, +3f)))
        assertEquals(Vector4F(5f, 20f, -7f, -3f), min(Vector4F(5f, 30f, +7f, +3f), Vector4F(10f, 20f, -7f, -3f)))
        assertEquals(Vector4F(5f, 20f, -7f, -3f), min(Vector4F(5f, 20f, -7f, -3f), Vector4F(10f, 30f, 0f, +3f)))
        assertEquals(Vector4F(5f, 20f, -7f, -3f), min(Vector4F(10f, 30f, 0f, 0f), Vector4F(5f, 20f, -7f, -3f)))
    }

    @Test
    fun testMax() {
        assertEquals(Vector2F(10f, 30f), max(Vector2F(10f, 20f), Vector2F(5f, 30f)))
        assertEquals(Vector2F(10f, 30f), max(Vector2F(5f, 30f), Vector2F(10f, 20f)))
        assertEquals(Vector2F(10f, 30f), max(Vector2F(5f, 20f), Vector2F(10f, 30f)))
        assertEquals(Vector2F(10f, 30f), max(Vector2F(10f, 30f), Vector2F(5f, 20f)))

        assertEquals(Vector3F(10f, 30f, 7f), max(Vector3F(10f, 20f, -7f), Vector3F(5f, 30f, +7f)))
        assertEquals(Vector3F(10f, 30f, 7f), max(Vector3F(5f, 30f, +7f), Vector3F(10f, 20f, -7f)))
        assertEquals(Vector3F(10f, 30f, 0f), max(Vector3F(5f, 20f, -7f), Vector3F(10f, 30f, 0f)))
        assertEquals(Vector3F(10f, 30f, 0f), max(Vector3F(10f, 30f, 0f), Vector3F(5f, 20f, -7f)))

        assertEquals(Vector4F(10f, 30f, 7f, 3f), max(Vector4F(10f, 20f, -7f, -3f), Vector4F(5f, 30f, +7f, +3f)))
        assertEquals(Vector4F(10f, 30f, 7f, 3f), max(Vector4F(5f, 30f, +7f, +3f), Vector4F(10f, 20f, -7f, -3f)))
        assertEquals(Vector4F(10f, 30f, 0f, 3f), max(Vector4F(5f, 20f, -7f, -3f), Vector4F(10f, 30f, 0f, +3f)))
        assertEquals(Vector4F(10f, 30f, 0f, 0f), max(Vector4F(10f, 30f, 0f, 0f), Vector4F(5f, 20f, -7f, -3f)))
    }
}
