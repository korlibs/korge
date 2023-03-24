package samples

import korlibs.korge.scene.Scene
import korlibs.korge.view.SContainer
import korlibs.korge.view.addUpdater
import korlibs.korge.view.ninePatchShapeView
import korlibs.image.color.Colors
import korlibs.image.vector.format.readSVG
import korlibs.image.vector.scaledShape
import korlibs.image.vector.toNinePatchFromGuides
import korlibs.image.vector.toShape
import korlibs.io.file.std.resourcesVfs

class MainVectorNinePatch : Scene() {
    val mousePos get() = sceneView.localMousePos(views)

    override suspend fun SContainer.sceneMain() {
        val view = ninePatchShapeView(
            resourcesVfs["chat-bubble.svg"]
                .readSVG()
                .toShape()
                .scaledShape(10.0, 10.0)
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
            view.setSize(mousePos.xD, mousePos.yD)
        }
    }
}
