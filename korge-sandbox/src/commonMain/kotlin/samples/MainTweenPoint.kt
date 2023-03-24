package samples

import korlibs.time.seconds
import korlibs.korge.scene.Scene
import korlibs.korge.tween.get
import korlibs.korge.tween.tween
import korlibs.korge.view.SContainer
import korlibs.korge.view.anchor
import korlibs.korge.view.addUpdater
import korlibs.korge.view.circle
import korlibs.korge.view.container
import korlibs.korge.view.cpuGraphics
import korlibs.korge.view.image
import korlibs.korge.view.scale
import korlibs.korge.view.xy
import korlibs.image.color.Colors
import korlibs.image.format.readBitmap
import korlibs.image.vector.toStrokeShape
import korlibs.io.async.launch
import korlibs.io.file.std.resourcesVfs
import korlibs.math.geom.*
import korlibs.math.geom.shape.buildVectorPath
import korlibs.math.geom.vector.getCurves
import korlibs.math.interpolation.Easing

class MainTweenPoint : Scene() {
    override suspend fun SContainer.sceneMain() {
        val tex = resourcesVfs["korge.png"].readBitmap()

        container {
            this.scale(2.0)

            val circle = circle(64.0).xy(200.0, 200.0).anchor(Anchor.CENTER)
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
