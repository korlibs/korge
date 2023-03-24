package samples

import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge.view.vector.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.image.paint.*
import korlibs.image.vector.*
import korlibs.io.file.std.*
import korlibs.math.geom.*

class MainVectorFill : Scene() {
    override suspend fun SContainer.sceneMain() {
        val bitmap = resourcesVfs["korge.png"].readBitmap()
        fun ShapeBuilder.buildMyShape() {
            fill(
                LinearGradientPaint(
                    x0 = 0, 0,
                    x1 = 512, 512,
                ) {
                    addColorStop(0.3, Colors["#5eff98"])
                    addColorStop(1, Colors["#ff284f"])
                }
            ) {
                rect(0, 0, 512, 512)
            }

            fill(bitmap.toPaint(MMatrix().pretranslate(0, 100).prescale(100.0 / 512.0).immutable)) {
                rect(0, 100, 100, 100)
            }

            fill(Colors.RED) {
                rect(0, 0, 100, 100)
            }
            fill(Colors.GREEN) {
                rect(100, 0, 100, 100)
            }
            fill(Colors.BLUE) {
                rect(200, 0, 100, 100)
            }
        }

        cpuGraphics {
            it.useNativeRendering = true
            buildMyShape()
            it.scale(0.5)
        }
        cpuGraphics {
            it.useNativeRendering = false
            buildMyShape()
            it.scale(0.5)
            it.xy(0, 256)
        }
        gpuShapeView(EmptyShape) {
            updateShape {
                buildMyShape()
            }
            scale(0.5)
            xy(256, 0)
        }
    }
}