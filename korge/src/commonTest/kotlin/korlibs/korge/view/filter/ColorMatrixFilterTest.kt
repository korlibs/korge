package korlibs.korge.view.filter

import assertEqualsFloat
import korlibs.math.geom.*
import kotlin.test.Test

class ColorMatrixFilterTest {
    @Test
    fun test() {
        val grayFilter = ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX)

        assertEqualsFloat(
            Vector4F(0.57f, 0.57f, 0.57f, 1f),
            grayFilter.colorMatrix.transform(Vector4F(.75f, .5f, .25f, 1f))
        )
    }
}
