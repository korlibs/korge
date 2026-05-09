package samples

import korlibs.image.bitmap.*
import korlibs.image.bitmap.trace.*
import korlibs.image.color.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*

class MainImageTrace : Scene() {
    override suspend fun SContainer.sceneMain() {
        val bmp = Bitmap32Context2d(300, 200) {
            fill(Colors.WHITE, winding = Winding.EVEN_ODD) {
                rect(Rectangle.fromBounds(2, 2, 18, 18))
                rectHole(Rectangle.fromBounds(6, 6, 9, 12))
                rectHole(Rectangle.fromBounds(10, 5, 15, 12))
                rect(Rectangle.fromBounds(50, 2, 68, 18))
                circle(Point(100, 100), 60.0)
                circle(Point(100, 100), 30.0)
                roundRect(200, 50, 50, 50, 5, 5)
                circle(Point(140, 100), 30.0)
            }
        }
        val path = bmp.trace()
        image(bmp)
        cpuGraphics { fill(Colors.RED) { write(path) } }.xy(50, 50).scale(3).alpha(0.5)
        //image(bmp)
        /*
        Bitmap2(bmp.width, bmp.height).also {
            for (y in 0 until)
            it[x, y] =
        }
        bmp.
        bmp.trace()
         */
    }
}
