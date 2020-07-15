package com.soywiz.kmem

import kotlin.test.*

class FBufferTest {
	@Test
	fun testBasicUsage() {
		val data = MemBufferAlloc(16)

		val i8 = data.asInt8Buffer()
		i8[0] = 0
		i8[1] = 1
		i8[2] = 2
		i8[3] = 3

		i8[4] = 4
		i8[5] = 5
		i8[6] = 6
		i8[7] = 7

		val i32 = data.asInt32Buffer()
		assertEquals(0x03020100, i32[0])
		assertEquals(0x07060504, i32[1])

		val i32_off1 = i32.subarray(1)
		assertEquals(0x07060504, i32_off1[0])
		i32_off1[1] = 0x0B0A0908

		assertEquals(0x0B0A0908, i32[2])
	}

    @Test
    fun testAllocUnaligned() {
        assertEquals(12, FBuffer.alloc(4 * 3 * 3).f32.size)
        assertEquals(9, FBuffer.allocUnaligned(4 * 3 * 3).f32.size)
    }

	@Test
	fun testArrayCopyOverlapping() {
		val i32 = Int32BufferAlloc(10)
		i32[0] = 0x01020304
		i32[1] = 0x05060708
		arraycopy(i32, 0, i32, 1, 4)
		assertEquals(0x01020304, i32[0])
		assertEquals(0x01020304, i32[1])
		assertEquals(0x05060708, i32[2])
		assertEquals(0x00000000, i32[3])
		assertEquals(0x00000000, i32[4])

		val fast = FBuffer(i32.mem)

		assertEquals(listOf(4, 3, 2, 1, 4, 3, 2, 1, 8, 7, 6, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), (0 until (10 * 4)).map { fast.i8[it].toInt() }.toList())

		val out = ByteArray(10)
		fast.getArrayInt8(1, out, 1, 5)

		assertEquals(listOf(0, 3, 2, 1, 4, 3, 0, 0, 0, 0), out.toList().map { it.toInt() })

		val outS = ShortArray(10)
		fast.getAlignedArrayInt16(1, outS, 1, 5)

		assertEquals(listOf(0, 258, 772, 258, 1800, 1286, 0, 0, 0, 0), outS.toList().map { it.toInt() })

		fast.setAlignedArrayInt16(1, shortArrayOf(1, 2, 3, 4, 5, 6), 1, 4)
		fast.getAlignedArrayInt16(1, outS, 1, 5)

		assertEquals(listOf(0, 2, 3, 4, 5, 1286, 0, 0, 0, 0), outS.toList().map { it.toInt() })
	}

	@Test
	fun testFBuffer() {
		val mem = FBuffer.alloc(10)
		for (n in 0 until 8) mem[n] = n
		assertEquals(0x03020100, mem.getAlignedInt32(0))
		assertEquals(0x07060504, mem.getAlignedInt32(1))

		assertEquals(0x03020100, mem.getUnalignedInt32(0))
		assertEquals(0x04030201, mem.getUnalignedInt32(1))
		assertEquals(0x05040302, mem.getUnalignedInt32(2))
	}
}
