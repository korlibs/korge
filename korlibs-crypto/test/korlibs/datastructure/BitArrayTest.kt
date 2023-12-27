package korlibs.datastructure

import kotlin.test.*
import kotlin.time.*

class BitArrayTest {
    @Test
    fun testBasic() {
        val bitArray = BitArray(5) { it % 2 != 0 }
        val booleanArray = BooleanArray(5) { it % 2 != 0 }
        assertEquals(5, bitArray.size)
        assertEquals(false, bitArray[0])
        assertEquals(true, bitArray[1])
        assertEquals(booleanArray.toList(), bitArray.toList())
        assertEquals("[false, true, false, true, false]", bitArray.toList().toString())
        assertEquals(BooleanArray(100) { it % 2 != 0 }.toList(), BitArray(100) { it % 2 != 0 }.toList())
        assertEquals(BooleanArray(100).toList(), BitArray(100).toList())
    }

    @Test
    fun testOutOfBounds() {
        val bitArray = BitArray(5)
        assertFailsWith<IndexOutOfBoundsException> { bitArray[-1] }
        for (n in 0 until 5) bitArray[n]
        assertFailsWith<IndexOutOfBoundsException> { bitArray[5] }
    }

    @Test
    fun testWrite() {
        val bitArray = BitArray(111)
        val bitArray2 = BitArray(111) { true }
        val booleanArray = BooleanArray(111)
        val booleanArray2 = BooleanArray(111) { true }
        var n = 0
        while (n < bitArray.size) {
            bitArray[n] = true
            booleanArray[n] = true
            bitArray2[n] = false
            booleanArray2[n] = false
            n += n + 1
        }
        assertEquals(bitArray.toList(), booleanArray.toList())
        assertEquals(bitArray2.toList(), booleanArray2.toList())
    }
}
