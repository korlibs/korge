package korlibs.korge

import korlibs.math.geom.*
import kotlin.test.*

class KorgeReloadTest {
    class MyClass1 {
        @KeepOnReload
        var v = Point(10, 10)
    }

    class MyClass2 {
        @KeepOnReload
        var v = Point(10, 10)
    }

    @Test
    fun test() {
        val v1 = MyClass1().also { it.v = Point(20, 20) }
        val v2 = MyClass2()
        KorgeReloadInternalJvm.transferKeepProperties(v1, v2)
        assertEquals(Point(20, 20), v2.v)
    }
}
