package korlibs.graphics

import korlibs.memory.*
import korlibs.graphics.shader.*
import kotlin.test.*

class AGValueTest {
    /*
    val array1 = intArrayOf(1, 0, 0, 0, 1, 1, 1, 1)
    val array2 = intArrayOf(0, 1, 1, 1, 0, 0, 0, 0)

    @Test
    fun test() {
        val buffer = AGUniformBuffer(Uniform("test", VarType.Bool4, 2), 2)
        val index0 = buffer.put { set(array1) }
        val index1 = buffer.put { set(array2) }
        assertEquals("0/1", "$index0/$index1")
        assertEquals("01000000010101010001010100000000", buffer.data.hex())
        assertEquals("0100000001010101", buffer.get(0).data.hex())
        assertEquals("0001010100000000", buffer.get(1).data.hex())
        assertEquals(array1.toList(), buffer.get(0).extractToFloatAndInts(Buffer(4 * 8)).getArrayInt32(0, IntArray(8)).toList())
        assertEquals(array2.toList(), buffer.get(1).extractToFloatAndInts(Buffer(4 * 8)).getArrayInt32(0, IntArray(8)).toList())
    }
    */
}
