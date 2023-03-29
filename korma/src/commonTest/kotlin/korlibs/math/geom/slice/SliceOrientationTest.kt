package korlibs.math.geom.slice

import korlibs.math.geom.*
import kotlin.test.*
import kotlin.test.assertEquals

class SliceOrientationTest {
    @Test
    fun test() {
        assertEquals(listOf(0, 1, 2, 3), SliceOrientation.ROTATE_0.indices.toList())
        assertEquals(listOf(3, 0, 1, 2), SliceOrientation.ROTATE_90.indices.toList())
        assertEquals(listOf(2, 3, 0, 1), SliceOrientation.ROTATE_180.indices.toList())
        assertEquals(listOf(1, 2, 3, 0), SliceOrientation.ROTATE_270.indices.toList())
    }

    @Test
    fun testTransform() {
        assertEquals(
            SliceOrientation(flipX = true, rotation = SliceRotation.R90),
            SliceOrientation.ROTATE_0.transformed(SliceOrientation.MIRROR_HORIZONTAL_ROTATE_0).transformed(SliceOrientation.ROTATE_90)
        )
        assertEquals(
            SliceOrientation.ROTATE_90,
            SliceOrientation.ROTATE_0.transformed(SliceOrientation.ROTATE_0.rotatedRight())
        )
    }

    @Test
    fun testRotation() {
        assertEquals(
            SliceOrientation.ROTATE_90,
            SliceOrientation.ROTATE_0.rotatedRight()
        )
    }

    @Test
    fun testTable() {
        assertEquals(
            """
                [ROTATE_0, ROTATE_90, ROTATE_180, ROTATE_270, MIRROR_HORIZONTAL_ROTATE_0, MIRROR_HORIZONTAL_ROTATE_90, MIRROR_HORIZONTAL_ROTATE_180, MIRROR_HORIZONTAL_ROTATE_270]
                [ROTATE_90, ROTATE_180, ROTATE_270, ROTATE_0, MIRROR_HORIZONTAL_ROTATE_90, MIRROR_HORIZONTAL_ROTATE_180, MIRROR_HORIZONTAL_ROTATE_270, MIRROR_HORIZONTAL_ROTATE_0]
                [ROTATE_270, ROTATE_0, ROTATE_90, ROTATE_180, MIRROR_HORIZONTAL_ROTATE_270, MIRROR_HORIZONTAL_ROTATE_0, MIRROR_HORIZONTAL_ROTATE_90, MIRROR_HORIZONTAL_ROTATE_180]
                [MIRROR_HORIZONTAL_ROTATE_0, MIRROR_HORIZONTAL_ROTATE_270, MIRROR_HORIZONTAL_ROTATE_180, MIRROR_HORIZONTAL_ROTATE_90, ROTATE_0, ROTATE_270, ROTATE_180, ROTATE_90]
                [MIRROR_HORIZONTAL_ROTATE_180, MIRROR_HORIZONTAL_ROTATE_90, MIRROR_HORIZONTAL_ROTATE_0, MIRROR_HORIZONTAL_ROTATE_270, ROTATE_180, ROTATE_90, ROTATE_0, ROTATE_270]
            """.trimIndent(),
            listOf(
                SliceOrientation.VALUES,
                SliceOrientation.VALUES.map { it.rotatedRight() },
                SliceOrientation.VALUES.map { it.rotatedLeft() },
                SliceOrientation.VALUES.map { it.flippedX() },
                SliceOrientation.VALUES.map { it.flippedY() },
            ).joinToString("\n")
        )
    }

    @Test
    fun testInverted() {
        assertEquals(
            Array(8) { SliceOrientation.ORIGINAL }.toList(),
            SliceOrientation.VALUES.map { it.transformed(it.inverted()) }
        )
    }

    @Test
    fun testXY() {
        assertEquals(PointInt(2, 1), SliceOrientation().getXY(10, 13, 2, 1))
        assertEquals(PointInt(7, 1), SliceOrientation(flipX = true).getXY(10, 13, 2, 1))
        assertEquals(PointInt(2, 11), SliceOrientation().flippedY().getXY(10, 13, 2, 1))

        assertEquals(PointInt(0, 0), SliceOrientation(rotation = SliceRotation.R0).getXY(10, 13, 0, 0))
        assertEquals(PointInt(12, 0), SliceOrientation(rotation = SliceRotation.R90).getXY(10, 13, 0, 0))
        assertEquals(PointInt(9, 12), SliceOrientation(rotation = SliceRotation.R180).getXY(10, 13, 0, 0))
        assertEquals(PointInt(0, 9), SliceOrientation(rotation = SliceRotation.R270).getXY(10, 13, 0, 0))

        assertEquals(PointInt(11, 2), SliceOrientation(rotation = SliceRotation.R90).getXY(10, 13, 2, 1))
        assertEquals(PointInt(7, 11), SliceOrientation(rotation = SliceRotation.R180).getXY(10, 13, 2, 1))
        assertEquals(PointInt(1, 7), SliceOrientation(rotation = SliceRotation.R270).getXY(10, 13, 2, 1))
    }
}
