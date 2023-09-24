package korlibs.image.vector

import korlibs.math.geom.*
import korlibs.math.geom.shape.*
import kotlin.test.*

class NinePatchVectorTest {
    @Test
    fun testNinePatchWithoutSlices() {
        val vector = buildVectorPath {
            rect(0, 0, 100, 100)
        }
        assertEquals(
            """
                M0,0 L200,0 L200,200 L0,200 Z
                M0,0 L50,0 L50,50 L0,50 Z
                M0,0 L100,0 L100,100 L0,100 Z
            """.trimIndent(),
            """
                ${vector.scaleNinePatch(Size(200, 200)).toSvgString()}
                ${vector.scaleNinePatch(Size(50, 50)).toSvgString()}
                ${vector.toSvgString()}
            """.trimIndent()

        )
    }

    @Test
    fun testRoundRect() {
        val vector = buildVectorPath {
            roundRect(0, 0, 100, 100, 25, 25)
        }
        assertEquals(
            """
                M50,0 L150,0 Q200,0,200,50 L200,150 Q200,200,150,200 L50,200 Q0,200,0,150 L0,50 Q0,0,50,0 Z
                M25,0 L75,0 Q100,0,100,25 L100,75 Q100,100,75,100 L25,100 Q0,100,0,75 L0,25 Q0,0,25,0 Z
            """.trimIndent(),
            """
                ${vector.scaleNinePatch(Size(200, 200)).roundDecimalPlaces(1).toSvgString()}
                ${vector.toSvgString()}
            """.trimIndent()

        )
    }

    @Test
    fun testRoundRectScaleDown() {
        val vector = buildVectorPath {
            roundRect(0, 0, 100, 100, 25, 25)
        }
        assertEquals(
            """
                M12.5,0 L37.5,0 Q50,0,50,12.5 L50,37.5 Q50,50,37.5,50 L12.5,50 Q0,50,0,37.5 L0,12.5 Q0,0,12.5,0 Z
                M12.5,0 L37.5,0 Q50,0,50,2.5 L50,7.5 Q50,10,37.5,10 L12.5,10 Q0,10,0,7.5 L0,2.5 Q0,0,12.5,0 Z
                M25,0 L75,0 Q100,0,100,25 L100,75 Q100,100,75,100 L25,100 Q0,100,0,75 L0,25 Q0,0,25,0 Z
            """.trimIndent(),
            """
                ${vector.scaleNinePatch(Size(50, 50)).roundDecimalPlaces(1).toSvgString()}
                ${vector.scaleNinePatch(Size(50, 10)).roundDecimalPlaces(1).toSvgString()}
                ${vector.toSvgString()}
            """.trimIndent()

        )
    }
}
