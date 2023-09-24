package korlibs.image.vector

import korlibs.image.color.*
import korlibs.image.vector.format.*
import korlibs.logger.*
import korlibs.math.geom.vector.*
import kotlin.test.*

class ShapeBuilderTest {
    val logger = Logger("ShapeBuilderTest")

    @Test
    fun test() {
        val svg = ChartBars("hello" to 10, "world" to 20).toShape(512, 256).toSvg().toOuterXmlIndented().toString()
        val svg2 = SVG(svg)
        logger.debug { svg2.toShape().toSvg().toOuterXmlIndented() }
        //println(svg)
        /*
        val shape = buildShape {
            lineWidth = 8.0
            fillStroke(ColorPaint(Colors.RED), ColorPaint(Colors.BLUE)) {
                rect(0, 0, 64, 64)
            }
            fillText("HELLO", 32, 32, font = Font("Helvetica", 16.0), halign = HorizontalAlign.CENTER, valign = VerticalAlign.MIDDLE, color = Colors.GREEN)
        }
        println(shape.toSvg())
         */
    }

    @Test
    fun testFillStroke() {
        val shape = buildShape {
            fillStroke(Colors.RED, Colors.BLUE, StrokeInfo(thickness = 5.0)) {
                rect(0, 0, 200, 100)
            }
        }
        assertIs<CompoundShape>(shape)
        assertEquals(2, shape.components.size)
        assertIs<FillShape>(shape.components[0])
        assertIs<PolylineShape>(shape.components[1])
    }
}
