package korlibs.math.geom

import kotlin.test.*

class SphereTest {
    @Test
    fun test() {
        val sphere = Sphere3D(Vector3F(0, 0, 0), 10f)
        assertEquals(10f, sphere.radius)
        assertEquals(4188.79f, sphere.volume, 0.01f)
    }
}
