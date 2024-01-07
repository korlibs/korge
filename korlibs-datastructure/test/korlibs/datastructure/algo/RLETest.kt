package korlibs.datastructure.algo

import kotlin.test.Test
import kotlin.test.assertEquals

class RLETest {
    @Test
    fun test() {
        assertEquals(
            """
                RLE()
                RLE([(1),0,1])
                RLE([(1),0,2])
                RLE([(1),0,3])
                RLE([(1),0,1], [(2),1,1])
                RLE([(1),0,1], [(2),1,1], [(1),2,1])
                RLE([(1),0,1], [(2),1,2], [(1),3,1])
                RLE([(1),0,2], [(3),2,1], [(2),3,2])
            """.trimIndent(),
            """
                ${RLE.compute(intArrayOf())}
                ${RLE.compute(intArrayOf(1))}
                ${RLE.compute(intArrayOf(1, 1))}
                ${RLE.compute(intArrayOf(1, 1, 1))}
                ${RLE.compute(intArrayOf(1, 2))}
                ${RLE.compute(intArrayOf(1, 2, 1))}
                ${RLE.compute(intArrayOf(1, 2, 2, 1))}
                ${RLE.compute(intArrayOf(1, 1, 3, 2, 2))}
            """.trimIndent()
        )
    }
}
