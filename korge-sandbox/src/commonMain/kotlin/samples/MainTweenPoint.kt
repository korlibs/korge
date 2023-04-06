package samples

import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.image.vector.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.korge.scene.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*
import korlibs.math.interpolation.*
import korlibs.time.*

class MainTweenPoint : Scene() {
    override suspend fun SContainer.sceneMain() {
        val tex = resourcesVfs["korge.png"].readBitmap()

        container {
            this.scale(2.0)

            val circle = circle(64f).xy(200.0, 200.0).anchor(Anchor.CENTER)
            val path = buildVectorPath {
                //circle(200, 200, 100)
                moveTo(Point(200.0, 200.0))
                lineTo(Point(400.0, 100.0))
                quadTo(Point(400.0, 400.0), Point(200.0, 200.0))
            }
            val curves = path.getCurves()
            cpuGraphics(path.toStrokeShape(Colors.RED, thickness = 2.0))
            image(tex).scale(0.2)
            launch {
                while (true) {
                    tween(circle::pos[path, false], time = 1.0.seconds, easing = Easing.LINEAR)
                    //circle.xy(0.0, 0.0)
                }
            }
        }

        addUpdater {
            println("views.ag.getStats: ${views.ag.getStats()}")
        }
    }
}
