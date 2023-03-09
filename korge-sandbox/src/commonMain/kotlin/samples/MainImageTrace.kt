package samples

import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.bitmap.trace.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

class MainImageTrace : Scene() {
    override suspend fun SContainer.sceneMain() {
        val bmp = Bitmap32Context2d(300, 200) {
            fill(Colors.WHITE, winding = Winding.EVEN_ODD) {
                rect(MRectangle.fromBounds(2, 2, 18, 18))
                rectHole(MRectangle.fromBounds(6, 6, 9, 12))
                rectHole(MRectangle.fromBounds(10, 5, 15, 12))
                rect(MRectangle.fromBounds(50, 2, 68, 18))
                circle(Point(100, 100), 60f)
                circle(Point(100, 100), 30f)
                roundRect(200, 50, 50, 50, 5, 5)
                circle(Point(140, 100), 30f)
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
