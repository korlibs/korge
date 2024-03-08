package korlibs.number

import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("INTEGER_OVERFLOW")
class Int53Test {
    @Test
    fun test() {
        assertEquals(30.toInt53(), 10.toInt53() + 20.toInt53())
        assertEquals(false, 10.toInt53() > 20.toInt53())
        assertEquals(true, 10.toInt53() < 20.toInt53())
        assertEquals(Int.MIN_VALUE, Int.MAX_VALUE + 1)
        assertEquals(true, (Int.MAX_VALUE.toInt53() + 1) > 0.toInt53())
        assertEquals(2147483648.toInt53(), (Int.MAX_VALUE.toInt53() + 1))
        assertEquals(500.toInt53(), 1001.toInt53() / 2)
        assertEquals((-500).toInt53(), (-1001).toInt53() / 2)
        assertEquals(0x0FFFFF, Int53.MAX_VALUE.high)
        assertEquals(0xFFFFFFFF.toInt(), Int53.MAX_VALUE.low)

        assertEquals(0x1FFFFF, (-1L).toInt53().high)
        assertEquals(0xFFFFFFFF.toInt(), (-1L).toInt53().low)

        assertEquals(0x1FFFFF.toInt(), Int53.fromLowHigh(-1, -1).high)
        assertEquals(0xFFFFFFFF.toInt(), Int53.fromLowHigh(-1, -1).low)
    }
}
