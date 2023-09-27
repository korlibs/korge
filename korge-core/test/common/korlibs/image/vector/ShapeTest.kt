package korlibs.image.vector

import korlibs.image.color.*
import korlibs.math.geom.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*
import kotlin.test.*

class ShapeTest {
    @Test
    fun name() {
        val shape = FillShape(
            path = VectorPath().apply {
                moveTo(Point(0, 0))
                lineTo(Point(100, 100))
                lineTo(Point(0, 100))
                close()
            },
            clip = null,
            paint = Colors.GREEN,
            //paint = BitmapPaint(Bitmap32(100, 100, Colors.RED, premultiplied = false), Matrix()),
            transform = Matrix.IDENTITY
        )
        assertEquals(
            //"""<svg width="100px" height="100px" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs><pattern id="def0" patternUnits="userSpaceOnUse" width="100" height="100" patternTransform="translate()"><image xlink:href="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAGQAAABkCAYAAABw4pVUAAAA/UlEQVR4nO3RoQ0AMBDEsNt/6e8YDTAwj5TddnTsdwCGpBkSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgS8wBKb9Zkl+sjewAAAABJRU5ErkJggg==" width="100" height="100"/></pattern></defs><g transform="translate()"><path d="M0 0L100 100L0 100Z" transform="translate()" fill="url(#def0)"/></g></svg>""",
            """<svg width="100px" height="100px" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate()"><path d="M0 0L100 100L0 100Z" transform="translate()" fill="rgba(0,255,0,1)"/></g></svg>""",
            shape.toSvg().outerXml
        )
    }

    @Test
    fun testShapeTransform() {
        val shape1 = buildShape {
            keepTransform {
                scale(2, 2)
                translate(200, 100)
                fill(
                    createLinearGradient(50, 50, 75, 120, transform = Matrix(2f, 0f, 0f, 2f)).addColorStop(0.0, Colors.RED)
                        .addColorStop(1.0, Colors.BLUE)
                ) {
                    rect(50, 10, 300, 200)
                }
            }
        }
        val shape2 = buildShape {
            draw(shape1)
        }
        val shape3 = buildShape {
            draw(shape2)
        }
        assertEquals(shape1, shape2)
        assertEquals(shape1, shape3)
    }

    @Test
    fun testEmptyBounds() {
        fun createStrokeShape(block: VectorBuilder.() -> Unit): PolylineShape = PolylineShape(buildVectorPath { block() }, null, Colors.RED, Matrix.IDENTITY, StrokeInfo())
        val shape1 = createStrokeShape { moveTo(Point(100, 100)) }
        val shape2 = createStrokeShape { moveTo(Point(100, 100)); lineTo(Point(200, 100)) }
        val shape3 = createStrokeShape { moveTo(Point(100, 100)); lineTo(Point(200, 200)) }
        assertEquals(shape1.bounds, shape1.getBounds(includeStrokes = true))
        assertEquals(shape2.bounds, shape2.getBounds(includeStrokes = true))
        assertEquals(shape3.bounds, shape3.getBounds(includeStrokes = true))

        assertEquals(Rectangle(100, 100, 0, 0), shape1.getBounds(includeStrokes = false))
        assertEquals(Rectangle(100, 100, 100, 0), shape2.getBounds(includeStrokes = false))
        assertEquals(Rectangle(100, 100, 100, 100), shape3.getBounds(includeStrokes = false))

        assertEquals(Rectangle(99.5, 99.5, 1.0, 1.0), shape1.getBounds(includeStrokes = true))
        assertEquals(Rectangle(99.5, 99.5, 101.0, 1.0), shape2.getBounds(includeStrokes = true))
        assertEquals(Rectangle(99.5, 99.5, 101.0, 101.0), shape3.getBounds(includeStrokes = true))
    }
}
