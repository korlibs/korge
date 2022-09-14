package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.graphics
import com.soywiz.korim.color.Colors
import com.soywiz.korim.util.NinePatchSlices
import com.soywiz.korim.util.NinePatchSlices2D
import com.soywiz.korim.vector.NinePatchVector
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.range.until
import com.soywiz.korma.geom.shape.buildVectorPath
import com.soywiz.korma.geom.vector.roundRect
import com.soywiz.korma.geom.vector.write

class MainVectorNinePatch : Scene() {
    val mouseX get() = sceneView.localMouseX(views)
    val mouseY get() = sceneView.localMouseY(views)

    override suspend fun SContainer.sceneMain() {
        val vector = buildVectorPath {
            roundRect(0, 0, 100, 100, 25, 25)
        }
        val vector9 = NinePatchVector(vector, NinePatchSlices2D(
            NinePatchSlices(26.0 until 74.0),
            NinePatchSlices(26.0 until 74.0),
        ))
        val g = graphics()

        addUpdater {
            g.updateShape {
                fill(Colors.WHITE) {
                    write(vector9.transform(Size(mouseX, mouseY)))
                }
            }
        }
    }
}
