package korlibs.math.geom

import kotlin.test.*

class VectorsTest {
    @Test
    fun testAbs() {
        assertEquals(Vector2(3f, 2f), Vector2(-3f, +2f).absoluteValue)
        assertEquals(Vector2(3f, 2f), abs(Vector2(+3f, -2f)))

        val signs = listOf(-1, +1)
        for (s1 in signs) {
            for (s2 in signs) {
                assertEquals(Vector2(3f, 2f), Vector2(3f * s1, 2f * s2).absoluteValue)
                assertEquals(Vector2(3f, 2f), abs(Vector2(3f * s1, 2f * s2)))
                for (s3 in signs) {
                    assertEquals(Vector3(3f, 2f, 4f), Vector3(3f * s1, 2f * s2, 4f * s3).absoluteValue)
                    assertEquals(Vector3(3f, 2f, 4f), abs(Vector3(3f * s1, 2f * s2, 4f * s3)))
                    for (s4 in signs) {
                        assertEquals(Vector4(3f, 2f, 4f, 5f), Vector4(3f * s1, 2f * s2, 4f * s3, 5f * s4).absoluteValue)
                        assertEquals(Vector4(3f, 2f, 4f, 5f), abs(Vector4(3f * s1, 2f * s2, 4f * s3, 5f * s4)))
                    }
                }
            }
        }
    }

    @Test
    fun testClamp() {
        assertEquals(Vector2(.5f, 1f), Vector2(-3f, +2f).clamp(.5f, 1f))
        assertEquals(Vector2(.5f, 2f), Vector2(-3f, +2f).clamp(.5f, 3f))
        assertEquals(Vector2(-3f, 2f), Vector2(-3f, +2f).clamp(-4f, 3f))

        assertEquals(Vector3(.5f, 1f, .75f), Vector3(-3f, +2f, .75f).clamp(.5f, 1f))
        assertEquals(Vector3(.5f, 2f, .75f), Vector3(-3f, +2f, .75f).clamp(.5f, 3f))
        assertEquals(Vector3(-3f, 2f, .75f), Vector3(-3f, +2f, .75f).clamp(-4f, 3f))
        assertEquals(Vector3(-3f, 2f, 3f), Vector3(-3f, +2f, 4f).clamp(-4f, 3f))

        assertEquals(Vector4(.5f, 1f, .75f, 1f), Vector4(-3f, +2f, .75f, 1f).clamp(.5f, 1f))
        assertEquals(Vector4(.5f, 2f, .75f, 1f), Vector4(-3f, +2f, .75f, 1f).clamp(.5f, 3f))
        assertEquals(Vector4(-3f, 2f, .75f, 1f), Vector4(-3f, +2f, .75f, 1f).clamp(-4f, 3f))
        assertEquals(Vector4(-3f, 2f, 3f, -4f), Vector4(-3f, +2f, 4f, -7f).clamp(-4f, 3f))
    }

    @Test
    fun testMin() {
        assertEquals(Vector2(5f, 20f), min(Vector2(10f, 20f), Vector2(5f, 30f)))
        assertEquals(Vector2(5f, 20f), min(Vector2(5f, 30f), Vector2(10f, 20f)))
        assertEquals(Vector2(5f, 20f), min(Vector2(5f, 20f), Vector2(10f, 30f)))
        assertEquals(Vector2(5f, 20f), min(Vector2(10f, 30f), Vector2(5f, 20f)))

        assertEquals(Vector3(5f, 20f, -7f), min(Vector3(10f, 20f, -7f), Vector3(5f, 30f, +7f)))
        assertEquals(Vector3(5f, 20f, -7f), min(Vector3(5f, 30f, +7f), Vector3(10f, 20f, -7f)))
        assertEquals(Vector3(5f, 20f, -7f), min(Vector3(5f, 20f, -7f), Vector3(10f, 30f, 0f)))
        assertEquals(Vector3(5f, 20f, -7f), min(Vector3(10f, 30f, 0f), Vector3(5f, 20f, -7f)))

        assertEquals(Vector4(5f, 20f, -7f, -3f), min(Vector4(10f, 20f, -7f, -3f), Vector4(5f, 30f, +7f, +3f)))
        assertEquals(Vector4(5f, 20f, -7f, -3f), min(Vector4(5f, 30f, +7f, +3f), Vector4(10f, 20f, -7f, -3f)))
        assertEquals(Vector4(5f, 20f, -7f, -3f), min(Vector4(5f, 20f, -7f, -3f), Vector4(10f, 30f, 0f, +3f)))
        assertEquals(Vector4(5f, 20f, -7f, -3f), min(Vector4(10f, 30f, 0f, 0f), Vector4(5f, 20f, -7f, -3f)))
    }

    @Test
    fun testMax() {
        assertEquals(Vector2(10f, 30f), max(Vector2(10f, 20f), Vector2(5f, 30f)))
        assertEquals(Vector2(10f, 30f), max(Vector2(5f, 30f), Vector2(10f, 20f)))
        assertEquals(Vector2(10f, 30f), max(Vector2(5f, 20f), Vector2(10f, 30f)))
        assertEquals(Vector2(10f, 30f), max(Vector2(10f, 30f), Vector2(5f, 20f)))

        assertEquals(Vector3(10f, 30f, 7f), max(Vector3(10f, 20f, -7f), Vector3(5f, 30f, +7f)))
        assertEquals(Vector3(10f, 30f, 7f), max(Vector3(5f, 30f, +7f), Vector3(10f, 20f, -7f)))
        assertEquals(Vector3(10f, 30f, 0f), max(Vector3(5f, 20f, -7f), Vector3(10f, 30f, 0f)))
        assertEquals(Vector3(10f, 30f, 0f), max(Vector3(10f, 30f, 0f), Vector3(5f, 20f, -7f)))

        assertEquals(Vector4(10f, 30f, 7f, 3f), max(Vector4(10f, 20f, -7f, -3f), Vector4(5f, 30f, +7f, +3f)))
        assertEquals(Vector4(10f, 30f, 7f, 3f), max(Vector4(5f, 30f, +7f, +3f), Vector4(10f, 20f, -7f, -3f)))
        assertEquals(Vector4(10f, 30f, 0f, 3f), max(Vector4(5f, 20f, -7f, -3f), Vector4(10f, 30f, 0f, +3f)))
        assertEquals(Vector4(10f, 30f, 0f, 0f), max(Vector4(10f, 30f, 0f, 0f), Vector4(5f, 20f, -7f, -3f)))
    }
}
