package korlibs.memory

import kotlin.test.*

class BaseBufferTest {
    @Test
    fun testInt() {
        val ints = intArrayOf(1, 2, 3)
        val data1: BaseIntBuffer = IntArrayBuffer(intArrayOf(1, 2, 3))
        val data2: BaseIntBuffer = Int32Buffer(intArrayOf(1, 2, 3))
        val data3: BaseIntBuffer = Uint8Buffer(ubyteArrayIntOf(1, 2, 3))
        val data4: BaseIntBuffer = Uint8ClampedBuffer(ubyteArrayIntOf(1, 2, 3))
        assertEquals(ints.toList(), data1.toIntArray().toList())
        assertEquals(ints.toList(), data2.toIntArray().toList())
        assertEquals(ints.toList(), data3.toIntArray().toList())
        assertEquals(ints.toList(), data4.toIntArray().toList())
    }

    @Test
    fun testFloat() {
        val floats = floatArrayOf(1f, 2f, 3f)
        val data1: BaseFloatBuffer = FloatArrayBuffer(floats)
        val data2: BaseFloatBuffer = Float32Buffer(floats)
        assertEquals(floats.toList(), data1.toFloatArray().toList())
        assertEquals(floats.toList(), data2.toFloatArray().toList())
    }
}
