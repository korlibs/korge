package korlibs.math.geom

import kotlin.test.*

class Matrix4Test {
    @Test
    fun test() {
        val matrix = Matrix4.IDENTITY * -2f
        assertEquals(
            Vector4(-2f, -4f, -6f, -8f),
            matrix.transformTransposed(Vector4(1, 2, 3, 4))
        )

        Matrix4.fromRows(
            1f, 2f, 3f, 4f,
            5f, 6f, 7f, 8f,
            9f, 10f, 11f, 12f,
            13f, 14f, 15f, 16f,
        )
    }
}
