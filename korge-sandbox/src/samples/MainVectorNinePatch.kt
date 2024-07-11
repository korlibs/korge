package samples

import korlibs.image.color.*
import korlibs.image.vector.*
import korlibs.image.vector.format.*
import korlibs.io.file.std.*
import korlibs.korge.scene.*
import korlibs.korge.view.*

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

        addFastUpdater {
            view.size(mousePos.x, mousePos.y)
        }
    }
}
