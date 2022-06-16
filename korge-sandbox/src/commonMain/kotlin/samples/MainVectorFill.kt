package samples

import com.soywiz.korge.Korge
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.graphics
import com.soywiz.korge.view.scale
import com.soywiz.korge.view.vector.gpuShapeView
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.paint.LinearGradientPaint
import com.soywiz.korim.paint.toPaint
import com.soywiz.korim.vector.ShapeBuilder
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.vector.rect

class MainVectorFill : Scene() {
    override suspend fun Container.sceneMain() {
        val bitmap = resourcesVfs["korge.png"].readBitmap()
        fun ShapeBuilder.buildMyShape() {
            fill(
                LinearGradientPaint(
                    x0 = 0, 0,
                    x1 = 512, 512,
                ) {
                    addColorStop(0.3, Colors.RED)
                    addColorStop(1, Colors.GREEN)
                }
            ) {
                rect(0, 0, 512, 512)
            }

            fill(bitmap.toPaint(Matrix().pretranslate(0, 100).prescale(100.0 / 512.0))) {
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

        graphics {
            this.useNativeRendering = true
            updateShape { buildMyShape() }
            scale(0.5)
        }
        graphics {
            this.useNativeRendering = false
            updateShape { buildMyShape() }
            scale(0.5)
            xy(0, 256)
        }
        gpuShapeView {
            updateShape { buildMyShape() }
            scale(0.5)
            xy(256, 0)
        }
    }
}
