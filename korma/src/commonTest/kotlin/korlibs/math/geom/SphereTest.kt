package korlibs.math.geom

import kotlin.test.*

class SphereTest {
    @Test
    fun test() {
        val sphere = Sphere3D(Vector3(0, 0, 0), 10f)
        println(sphere.radius)
        println(sphere.volume)
    }
}
