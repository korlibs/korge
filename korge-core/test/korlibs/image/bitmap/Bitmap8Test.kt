package korlibs.image.bitmap

import kotlin.test.Test
import kotlin.test.assertTrue

class Bitmap8Test {
    val bmpOdd = Bitmap8(5, 3, byteArrayOf(
        0, 1, 2, 3, 4,
        5, 6, 7, 8, 9,
        10, 11, 12, 13, 14
    ))

    val bmpEven = Bitmap8(6, 4, byteArrayOf(
        0, 1, 2, 3, 4, 5,
        6, 7, 8, 9, 10, 11,
        12, 13, 14, 15, 16, 17,
        18, 19, 20, 21, 22, 23
    ))

    @Test
    fun testFlipXOdd() {
        assertTrue {
            bmpOdd.clone().flipX().contentEquals(
                Bitmap8(5, 3, byteArrayOf(
                    4, 3, 2, 1, 0,
                    9, 8, 7, 6, 5,
                    14, 13, 12, 11, 10
                ))
            )
        }
    }

    @Test
    fun testFlipYOdd() {
        assertTrue {
            bmpOdd.clone().flipY().contentEquals(
                Bitmap8(5, 3, byteArrayOf(
                    10, 11, 12, 13, 14,
                    5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4
                ))
            )
        }
    }

    @Test
    fun testFlipXEven() {
        assertTrue {
            bmpEven.clone().flipX().contentEquals(
                Bitmap8(6, 4, byteArrayOf(
                    5, 4, 3, 2, 1, 0,
                    11, 10, 9, 8, 7, 6,
                    17, 16, 15, 14, 13, 12,
                    23, 22, 21, 20, 19, 18
                ))
            )
        }
    }

    @Test
    fun testFlipYEven() {
        assertTrue {
            bmpEven.clone().flipY().contentEquals(
                Bitmap8(6, 4, byteArrayOf(
                    18, 19, 20, 21, 22, 23,
                    12, 13, 14, 15, 16, 17,
                    6, 7, 8, 9, 10, 11,
                    0, 1, 2, 3, 4, 5
                ))
            )
        }
    }
}
