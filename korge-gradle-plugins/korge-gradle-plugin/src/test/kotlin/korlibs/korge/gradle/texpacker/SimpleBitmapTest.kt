package korlibs.korge.gradle.texpacker

import java.awt.*
import org.junit.Assert
import org.junit.Test

class SimpleBitmapTest {
    @Test
    fun testConstructionAndPixelAccess() {
        val bmp = SimpleBitmap(2, 2)
        bmp[0, 0] = SimpleRGBA(0x11223344)
        bmp[1, 0] = SimpleRGBA(0x55667788)
        bmp[0, 1] = SimpleRGBA(0x19AABBCC)
        bmp[1, 1] = SimpleRGBA(0x1DEEFF00)
        Assert.assertEquals(0x11223344, bmp[0, 0].data)
        Assert.assertEquals(0x55667788, bmp[1, 0].data)
        Assert.assertEquals(0x19AABBCC, bmp[0, 1].data)
        Assert.assertEquals(0x1DEEFF00, bmp[1, 1].data)
    }

    @Test
    fun testSlice() {
        val bmp = SimpleBitmap(4, 4)
        for (y in 0 until 4) for (x in 0 until 4) bmp[x, y] = SimpleRGBA(x + y * 4)
        val slice = bmp.slice(Rectangle(1, 1, 2, 2))
        Assert.assertEquals(2, slice.width)
        Assert.assertEquals(2, slice.height)
        Assert.assertEquals(5, slice[0, 0].data)
        Assert.assertEquals(6, slice[1, 0].data)
        Assert.assertEquals(9, slice[0, 1].data)
        Assert.assertEquals(10, slice[1, 1].data)
    }

    @Test
    fun testHashCodeConsistency() {
        val bmp1 = SimpleBitmap(2, 2)
        val bmp2 = SimpleBitmap(2, 2)
        for (i in 0..3) {
            bmp1.data[i] = i * 10
            bmp2.data[i] = i * 10
        }
        Assert.assertEquals(bmp1.hashCode(), bmp2.hashCode())
        bmp2.data[3] = 999
        Assert.assertNotEquals(bmp1.hashCode(), bmp2.hashCode())
    }

    @Test
    fun testGetRectAndPutRect() {
        val bmp = SimpleBitmap(3, 3)
        for (i in bmp.data.indices) bmp.data[i] = i
        val rect = bmp.getRect(1, 1, 2, 2)
        Assert.assertArrayEquals(intArrayOf(4, 5, 7, 8), rect)
        val bmp2 = SimpleBitmap(2, 2)
        bmp2.putRect(0, 0, 2, 2, rect)
        Assert.assertArrayEquals(rect, bmp2.data)
    }
}

