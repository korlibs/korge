package korlibs.math.geom.ds

import korlibs.datastructure.Array2
import korlibs.math.geom.*
import korlibs.math.geom.MPointInt
import kotlin.test.Test
import kotlin.test.assertEquals

class Array2ExtTest {
    val array = Array2(10, 10) { 0 }

    @Test
    fun test() {
        array[PointInt(5, 5)] = 10
        assertEquals(10, array[5, 5])
    }
}
