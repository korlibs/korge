package korlibs.image.awt

import korlibs.math.geom.*
import kotlin.test.Test
import kotlin.test.assertEquals

class AwtNativeImageTest {
    @Test
    fun testMatrixAwtTransform() {
        val matrix = Matrix(1, 2, 3, 4, 5, 6)
        assertEquals(matrix, matrix.toAwt().toMatrix())
    }
}
