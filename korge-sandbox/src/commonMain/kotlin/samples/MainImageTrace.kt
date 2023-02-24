package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.alpha
import com.soywiz.korge.view.cpuGraphics
import com.soywiz.korge.view.image
import com.soywiz.korge.view.scale
import com.soywiz.korge.view.xy
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.bitmap.trace.trace
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.MRectangle
import com.soywiz.korma.geom.vector.Winding
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.geom.vector.rect
import com.soywiz.korma.geom.vector.rectHole
import com.soywiz.korma.geom.vector.roundRect
import com.soywiz.korma.geom.vector.write

class MainImageTrace : Scene() {
    override suspend fun SContainer.sceneMain() {
        val bmp = Bitmap32Context2d(300, 200) {
            fill(Colors.WHITE, winding = Winding.EVEN_ODD) {
                rect(MRectangle.fromBounds(2, 2, 18, 18))
                rectHole(MRectangle.fromBounds(6, 6, 9, 12))
                rectHole(MRectangle.fromBounds(10, 5, 15, 12))
                rect(MRectangle.fromBounds(50, 2, 68, 18))
                circle(100, 100, 60)
                circle(100, 100, 30)
                roundRect(200, 50, 50, 50, 5, 5)
                circle(140, 100, 30)
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
