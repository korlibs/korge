package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.ninePatchShapeView
import com.soywiz.korim.color.Colors
import com.soywiz.korim.vector.format.readSVG
import com.soywiz.korim.vector.toNinePatchFromGuides
import com.soywiz.korim.vector.toShape
import com.soywiz.korio.file.std.resourcesVfs

class MainVectorNinePatch : Scene() {
    val mouseX get() = sceneView.localMouseX(views)
    val mouseY get() = sceneView.localMouseY(views)

    override suspend fun SContainer.sceneMain() {
        val view = ninePatchShapeView(
            resourcesVfs["chat-bubble.svg"]
                .readSVG()
                .toShape()
                .toNinePatchFromGuides(guideColor = Colors.FUCHSIA)
        )

        //graphics(resourcesVfs["chat-bubble.svg"].readSVG().toShape())
        //return
        //val view = ninePatchShapeView(buildShape {
        //    fill(Colors.RED) {
        //        roundRect(0, 0, 300, 300, 75, 75)
        //        rect(0, 225, 75, 75)
        //    }
        //    fill(Colors.WHITE) {
        //        roundRect(10, 10, 300 - 20, 300 - 20, 45, 45)
        //    }
        //}.ninePatch(
        //    NinePatchSlices(77.0 until (300.0 - 77.0)),
        //    NinePatchSlices(77.0 until (300.0 - 77.0)),
        //))

        addUpdater {
            view.setSize(mouseX, mouseY)
        }
    }
}
